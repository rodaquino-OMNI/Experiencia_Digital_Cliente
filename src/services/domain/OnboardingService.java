package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Onboarding Service - New Member Onboarding Management Delegate
 *
 * Manages onboarding status for new beneficiaries, tracks incomplete
 * registrations, and triggers re-engagement workflows.
 *
 * BPMN Coverage:
 * - onboardingService.marcarIncompleto (Mark onboarding as incomplete)
 */
@Component("onboardingService")
public class OnboardingService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("onboardingMethod");

        if ("marcarIncompleto".equals(method)) {
            marcarIncompleto(execution);
        } else {
            logger.warn("Unknown onboarding method: {}", method);
            throw new IllegalArgumentException("Invalid onboarding method: " + method);
        }
    }

    /**
     * Mark onboarding as incomplete
     *
     * Flags incomplete onboarding registrations and triggers
     * re-engagement workflows to complete beneficiary activation.
     *
     * Incomplete reasons:
     * - Missing documentation
     * - Incomplete profile data
     * - Pending verification
     * - Abandoned process
     * - Technical failure
     *
     * @param execution Process execution context
     */
    public void marcarIncompleto(DelegateExecution execution) throws Exception {
        logger.info("Executing onboardingService.marcarIncompleto for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String motivoIncompleto = (String) execution.getVariable("motivoIncompleto");
            String etapaInterrompida = (String) execution.getVariable("etapaInterrompida");

            // Build incomplete onboarding record
            Map<String, Object> registroIncompleto = new HashMap<>();
            registroIncompleto.put("beneficiarioId", beneficiarioId);
            registroIncompleto.put("processInstanceId", execution.getProcessInstanceId());
            registroIncompleto.put("statusOnboarding", "INCOMPLETO");
            registroIncompleto.put("motivoIncompleto", motivoIncompleto);
            registroIncompleto.put("etapaInterrompida", etapaInterrompida);
            registroIncompleto.put("dataInicio", execution.getVariable("dataInicioOnboarding"));
            registroIncompleto.put("dataInterrupcao", LocalDateTime.now().toString());

            // Track completion percentage
            int etapasCompletas = (int) execution.getVariable("etapasCompletasOnboarding");
            int etapasTotal = (int) execution.getVariable("etapasTotalOnboarding");
            int percentualCompleto = (etapasCompletas * 100) / etapasTotal;

            registroIncompleto.put("etapasCompletas", etapasCompletas);
            registroIncompleto.put("etapasTotal", etapasTotal);
            registroIncompleto.put("percentualCompleto", percentualCompleto);

            // Identify missing items
            Map<String, Boolean> checklistOnboarding =
                (Map<String, Boolean>) execution.getVariable("checklistOnboarding");

            if (checklistOnboarding != null) {
                registroIncompleto.put("itensCompletos", checklistOnboarding);
                registroIncompleto.put("numeroItensPendentes",
                    checklistOnboarding.values().stream().filter(v -> !v).count());
            }

            // Determine re-engagement strategy
            String estrategiaReengajamento = determinarEstrategiaReengajamento(
                motivoIncompleto, percentualCompleto, etapaInterrompida);

            registroIncompleto.put("estrategiaReengajamento", estrategiaReengajamento);

            // Calculate priority for follow-up
            String prioridadeFollowUp = calcularPrioridadeFollowUp(percentualCompleto, motivoIncompleto);
            registroIncompleto.put("prioridadeFollowUp", prioridadeFollowUp);

            // Determine optimal contact timing
            String momentoContatoOtimo = determinarMomentoContato(execution);
            registroIncompleto.put("momentoContatoOtimo", momentoContatoOtimo);

            // Save incomplete record
            dataLakeService.salvar(execution, "onboarding_incompleto",
                beneficiarioId, registroIncompleto);

            // Update beneficiary onboarding status
            Map<String, Object> updateBeneficiario = new HashMap<>();
            updateBeneficiario.put("onboardingStatus", "INCOMPLETO");
            updateBeneficiario.put("onboardingPercentual", percentualCompleto);
            updateBeneficiario.put("ultimaInteracaoOnboarding", LocalDateTime.now().toString());

            dataLakeService.atualizar(execution, "beneficiarios", beneficiarioId, updateBeneficiario);

            // Publish incomplete event for re-engagement workflow
            Map<String, Object> eventoIncompleto = new HashMap<>();
            eventoIncompleto.put("beneficiarioId", beneficiarioId);
            eventoIncompleto.put("estrategiaReengajamento", estrategiaReengajamento);
            eventoIncompleto.put("prioridadeFollowUp", prioridadeFollowUp);
            eventoIncompleto.put("percentualCompleto", percentualCompleto);

            kafkaPublisher.publicar(execution, "onboarding-incompleto", eventoIncompleto);

            execution.setVariable("incompletoRegistrado", true);
            execution.setVariable("estrategiaReengajamento", estrategiaReengajamento);
            execution.setVariable("agendarFollowUp", true);

            logger.info("Incomplete onboarding marked: beneficiary={}, reason={}, completion={}%, strategy={}",
                beneficiarioId, motivoIncompleto, percentualCompleto, estrategiaReengajamento);

        } catch (Exception e) {
            logger.error("Error marking incomplete onboarding for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("incompletoErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Determine re-engagement strategy based on incomplete reason
     */
    private String determinarEstrategiaReengajamento(
        String motivo, int percentualCompleto, String etapa) {

        // Near completion - gentle reminder
        if (percentualCompleto >= 80) {
            return "LEMBRETE_RAPIDO";
        }

        // Documentation issues - provide assistance
        if ("DOCUMENTACAO_PENDENTE".equals(motivo)) {
            return "ASSISTENCIA_DOCUMENTACAO";
        }

        // Technical issues - proactive outreach
        if ("ERRO_TECNICO".equals(motivo)) {
            return "CONTATO_SUPORTE_TECNICO";
        }

        // Abandoned mid-process - strong re-engagement
        if (percentualCompleto >= 40 && percentualCompleto < 80) {
            return "CAMPANHA_REENGAJAMENTO_ATIVA";
        }

        // Early abandonment - educational approach
        if (percentualCompleto < 40) {
            return "EDUCACAO_BENEFICIOS";
        }

        // Default
        return "CONTATO_PADRAO";
    }

    /**
     * Calculate follow-up priority
     */
    private String calcularPrioridadeFollowUp(int percentualCompleto, String motivo) {
        // High completion rate - high priority (don't lose them!)
        if (percentualCompleto >= 70) {
            return "ALTA";
        }

        // Critical blocking issues - high priority
        if ("ERRO_TECNICO".equals(motivo) || "VERIFICACAO_PENDENTE".equals(motivo)) {
            return "ALTA";
        }

        // Moderate completion - medium priority
        if (percentualCompleto >= 40) {
            return "MEDIA";
        }

        // Early stage abandonment - low priority (likely exploring)
        return "BAIXA";
    }

    /**
     * Determine optimal contact timing based on user behavior
     */
    private String determinarMomentoContato(DelegateExecution execution) {
        // Analyze user activity patterns from process variables
        String canalPreferido = (String) execution.getVariable("canalPreferido");
        String horarioPreferido = (String) execution.getVariable("horarioPreferido");

        if (horarioPreferido != null) {
            return horarioPreferido;
        }

        // Default timing based on channel
        if ("APP".equals(canalPreferido)) {
            return "NOITE_18_21"; // Evening for app users
        } else if ("TELEFONE".equals(canalPreferido)) {
            return "MANHA_09_12"; // Morning for phone calls
        }

        return "TARDE_14_17"; // Default afternoon
    }
}
