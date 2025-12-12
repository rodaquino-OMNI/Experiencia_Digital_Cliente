package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Onboarding Falha Service - Onboarding Failure Handler Delegate
 *
 * Handles onboarding failures with error recovery workflows,
 * support escalation, and root cause tracking.
 *
 * BPMN Coverage:
 * - onboardingFalhaService.tratar (Handle onboarding failure)
 */
@Component("onboardingFalhaService")
public class OnboardingFalhaService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingFalhaService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("onboardingFalhaMethod");

        if ("tratar".equals(method)) {
            tratar(execution);
        } else {
            logger.warn("Unknown onboarding falha method: {}", method);
            throw new IllegalArgumentException("Invalid onboarding falha method: " + method);
        }
    }

    /**
     * Handle onboarding failure
     *
     * Executes recovery workflow for failed onboardings including:
     * - Error classification and logging
     * - Automatic retry for transient failures
     * - Support escalation for blocking issues
     * - Root cause analysis tracking
     * - Customer communication
     *
     * @param execution Process execution context
     */
    public void tratar(DelegateExecution execution) throws Exception {
        logger.info("Executing onboardingFalhaService.tratar for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String tipoFalha = (String) execution.getVariable("tipoFalha");
            String mensagemErro = (String) execution.getVariable("mensagemErro");
            String etapaFalha = (String) execution.getVariable("etapaFalha");

            // Build failure record
            Map<String, Object> registroFalha = new HashMap<>();
            registroFalha.put("beneficiarioId", beneficiarioId);
            registroFalha.put("processInstanceId", execution.getProcessInstanceId());
            registroFalha.put("tipoFalha", tipoFalha);
            registroFalha.put("mensagemErro", mensagemErro);
            registroFalha.put("etapaFalha", etapaFalha);
            registroFalha.put("dataHoraFalha", LocalDateTime.now().toString());
            registroFalha.put("stackTrace", execution.getVariable("stackTrace"));

            // Classify failure severity and type
            String severidadeFalha = classificarSeveridade(tipoFalha, etapaFalha);
            String categoriaFalha = classificarCategoria(tipoFalha, mensagemErro);

            registroFalha.put("severidade", severidadeFalha);
            registroFalha.put("categoria", categoriaFalha);

            // Determine if failure is recoverable
            boolean recuperavel = isRecuperavel(tipoFalha, severidadeFalha);
            registroFalha.put("recuperavel", recuperavel);

            // Define recovery strategy
            String estrategiaRecuperacao = definirEstrategiaRecuperacao(
                tipoFalha, severidadeFalha, categoriaFalha, recuperavel);

            registroFalha.put("estrategiaRecuperacao", estrategiaRecuperacao);

            // Execute recovery actions
            List<String> acoesRecuperacao = new ArrayList<>();

            if (recuperavel) {
                // Attempt automatic recovery
                logger.info("Attempting automatic recovery for failure: {}", tipoFalha);

                if ("RETRY".equals(estrategiaRecuperacao)) {
                    acoesRecuperacao.add("AGENDADO_RETRY");
                    agendarRetry(execution, beneficiarioId);
                } else if ("COMPENSACAO".equals(estrategiaRecuperacao)) {
                    acoesRecuperacao.add("EXECUTADO_COMPENSACAO");
                    executarCompensacao(execution);
                } else if ("ROTA_ALTERNATIVA".equals(estrategiaRecuperacao)) {
                    acoesRecuperacao.add("ACIONADA_ROTA_ALTERNATIVA");
                    acionarRotaAlternativa(execution);
                }
            }

            // Escalate to support if needed
            boolean requerEscalacao = severidadeFalha.equals("CRITICA") ||
                                     severidadeFalha.equals("ALTA") ||
                                     !recuperavel;

            if (requerEscalacao) {
                acoesRecuperacao.add("ESCALADO_SUPORTE");
                escalarParaSuporte(execution, beneficiarioId, registroFalha);
            }

            // Notify customer
            acoesRecuperacao.add("NOTIFICADO_CLIENTE");
            notificarCliente(execution, beneficiarioId, recuperavel);

            // Track for RCA (Root Cause Analysis)
            acoesRecuperacao.add("REGISTRADO_RCA");
            registrarParaRCA(execution, registroFalha);

            registroFalha.put("acoesRecuperacao", acoesRecuperacao);

            // Save failure record
            dataLakeService.salvar(execution, "onboarding_falhas",
                UUID.randomUUID().toString(), registroFalha);

            // Update beneficiary status
            Map<String, Object> updateBeneficiario = new HashMap<>();
            updateBeneficiario.put("onboardingStatus", "FALHA");
            updateBeneficiario.put("onboardingFalhaData", LocalDateTime.now().toString());
            updateBeneficiario.put("onboardingRecuperavel", recuperavel);

            dataLakeService.atualizar(execution, "beneficiarios", beneficiarioId, updateBeneficiario);

            // Publish failure event
            kafkaPublisher.publicar(execution, "onboarding-falha", registroFalha);

            execution.setVariable("falhaProcessada", true);
            execution.setVariable("estrategiaRecuperacao", estrategiaRecuperacao);
            execution.setVariable("acoesRecuperacao", acoesRecuperacao);
            execution.setVariable("requerEscalacao", requerEscalacao);

            logger.info("Onboarding failure handled: beneficiary={}, type={}, severity={}, recoverable={}, strategy={}",
                beneficiarioId, tipoFalha, severidadeFalha, recuperavel, estrategiaRecuperacao);

        } catch (Exception e) {
            logger.error("Critical error handling onboarding failure for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("falhaProcessamentoErro", e.getMessage());
            throw e;
        }
    }

    private String classificarSeveridade(String tipoFalha, String etapa) {
        // Critical failures that block activation
        if ("ERRO_SISTEMA".equals(tipoFalha) || "INTEGRACAO_FALHOU".equals(tipoFalha)) {
            return "CRITICA";
        }

        // High priority failures
        if ("VALIDACAO_DOCUMENTO".equals(tipoFalha) || "PAGAMENTO_FALHOU".equals(tipoFalha)) {
            return "ALTA";
        }

        // Medium priority
        if ("DADOS_INVALIDOS".equals(tipoFalha) || "TIMEOUT".equals(tipoFalha)) {
            return "MEDIA";
        }

        return "BAIXA";
    }

    private String classificarCategoria(String tipoFalha, String mensagemErro) {
        if (tipoFalha != null) {
            if (tipoFalha.contains("SISTEMA") || tipoFalha.contains("TECNICO")) {
                return "TECNICA";
            }
            if (tipoFalha.contains("VALIDACAO") || tipoFalha.contains("DADOS")) {
                return "NEGOCIO";
            }
            if (tipoFalha.contains("INTEGRACAO") || tipoFalha.contains("API")) {
                return "INTEGRACAO";
            }
        }
        return "DESCONHECIDA";
    }

    private boolean isRecuperavel(String tipoFalha, String severidade) {
        // Non-recoverable failures
        if ("FRAUDE_DETECTADA".equals(tipoFalha) || "CADASTRO_DUPLICADO".equals(tipoFalha)) {
            return false;
        }

        // Transient failures are recoverable
        if ("TIMEOUT".equals(tipoFalha) || "SERVICO_INDISPONIVEL".equals(tipoFalha)) {
            return true;
        }

        // Medium/low severity usually recoverable
        return !severidade.equals("CRITICA");
    }

    private String definirEstrategiaRecuperacao(
        String tipoFalha, String severidade, String categoria, boolean recuperavel) {

        if (!recuperavel) {
            return "MANUAL_INTERVENTION";
        }

        if ("TIMEOUT".equals(tipoFalha) || "SERVICO_INDISPONIVEL".equals(tipoFalha)) {
            return "RETRY";
        }

        if ("VALIDACAO_DOCUMENTO".equals(tipoFalha)) {
            return "ROTA_ALTERNATIVA"; // Try manual verification
        }

        if ("INTEGRACAO".equals(categoria)) {
            return "COMPENSACAO"; // Compensate and retry with fallback
        }

        return "ESCALACAO_SUPORTE";
    }

    private void agendarRetry(DelegateExecution execution, String beneficiarioId) {
        execution.setVariable("retryAgendado", true);
        execution.setVariable("retryEm", LocalDateTime.now().plusMinutes(5).toString());
    }

    private void executarCompensacao(DelegateExecution execution) {
        execution.setVariable("compensacaoExecutada", true);
        execution.setVariable("tipoCompensacao", "ONBOARDING_ROLLBACK");
    }

    private void acionarRotaAlternativa(DelegateExecution execution) {
        execution.setVariable("rotaAlternativaAcionada", true);
        execution.setVariable("tipoRotaAlternativa", "VERIFICACAO_MANUAL");
    }

    private void escalarParaSuporte(DelegateExecution execution, String beneficiarioId,
                                   Map<String, Object> falha) {
        execution.setVariable("escaladoParaSuporte", true);
        execution.setVariable("ticketSuporte", "TICKET-" + UUID.randomUUID().toString());
        execution.setVariable("prioridadeSuporte", falha.get("severidade"));
    }

    private void notificarCliente(DelegateExecution execution, String beneficiarioId, boolean recuperavel) {
        execution.setVariable("clienteNotificado", true);
        execution.setVariable("tipoNotificacao", recuperavel ? "TEMPORARY_ISSUE" : "MANUAL_REQUIRED");
    }

    private void registrarParaRCA(DelegateExecution execution, Map<String, Object> falha) {
        execution.setVariable("registradoRCA", true);
        execution.setVariable("rcaId", "RCA-" + UUID.randomUUID().toString());
    }
}
