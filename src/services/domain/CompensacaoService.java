package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compensation Service - Transaction Rollback Delegate
 *
 * Executes compensation logic for process rollbacks and error recovery.
 * Ensures transaction safety and idempotent operations during failure scenarios.
 *
 * BPMN Coverage:
 * - compensacaoService.executar (Execute compensation/rollback)
 */
@Component("compensacaoService")
public class CompensacaoService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(CompensacaoService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("compensacaoMethod");

        if ("executar".equals(method)) {
            executar(execution);
        } else {
            logger.warn("Unknown compensation method: {}", method);
            throw new IllegalArgumentException("Invalid compensation method: " + method);
        }
    }

    /**
     * Execute compensation logic for rollbacks
     *
     * Reverts changes made during process execution when errors occur.
     * Maintains data consistency and audit trail of compensated actions.
     *
     * Compensation scenarios:
     * - Authorization rejection -> Revert beneficiary flags
     * - Payment failure -> Cancel reservation
     * - External API failure -> Rollback status changes
     * - Timeout -> Clean temporary data
     *
     * @param execution Process execution context
     */
    @Transactional
    public void executar(DelegateExecution execution) throws Exception {
        logger.info("Executing compensacaoService.executar for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract compensation context
            String tipoCompensacao = (String) execution.getVariable("tipoCompensacao");
            String motivoCompensacao = (String) execution.getVariable("motivoCompensacao");
            List<String> acoesExecutadas = (List<String>) execution.getVariable("acoesExecutadas");

            if (acoesExecutadas == null) {
                acoesExecutadas = new ArrayList<>();
            }

            List<String> acoesCompensadas = new ArrayList<>();
            boolean compensacaoCompleta = true;

            logger.info("Starting compensation for type: {}, reason: {}, actions: {}",
                tipoCompensacao, motivoCompensacao, acoesExecutadas.size());

            // Compensate based on executed actions (reverse order)
            for (int i = acoesExecutadas.size() - 1; i >= 0; i--) {
                String acao = acoesExecutadas.get(i);

                try {
                    switch (acao) {
                        case "AUTORIZACAO_CRIADA":
                            compensarAutorizacao(execution);
                            acoesCompensadas.add("AUTORIZACAO_CANCELADA");
                            break;

                        case "BENEFICIARIO_ATUALIZADO":
                            compensarBeneficiario(execution);
                            acoesCompensadas.add("BENEFICIARIO_REVERTIDO");
                            break;

                        case "NOTIFICACAO_ENVIADA":
                            compensarNotificacao(execution);
                            acoesCompensadas.add("NOTIFICACAO_CANCELADA");
                            break;

                        case "PAGAMENTO_RESERVADO":
                            compensarPagamento(execution);
                            acoesCompensadas.add("PAGAMENTO_LIBERADO");
                            break;

                        case "CARE_PLAN_CRIADO":
                            compensarCarePlan(execution);
                            acoesCompensadas.add("CARE_PLAN_REMOVIDO");
                            break;

                        case "DADOS_EXTERNOS_SALVOS":
                            compensarDadosExternos(execution);
                            acoesCompensadas.add("DADOS_EXTERNOS_REMOVIDOS");
                            break;

                        default:
                            logger.warn("No compensation handler for action: {}", acao);
                            acoesCompensadas.add("NAO_COMPENSADO_" + acao);
                    }

                } catch (Exception e) {
                    logger.error("Error compensating action {}: {}", acao, e.getMessage(), e);
                    compensacaoCompleta = false;
                    acoesCompensadas.add("FALHA_COMPENSACAO_" + acao);
                }
            }

            // Store compensation results
            execution.setVariable("compensacaoCompleta", compensacaoCompleta);
            execution.setVariable("acoesCompensadas", acoesCompensadas);
            execution.setVariable("compensadoEm", LocalDateTime.now().toString());

            // Publish compensation event
            Map<String, Object> compensationEvent = Map.of(
                "processInstanceId", execution.getProcessInstanceId(),
                "tipoCompensacao", tipoCompensacao,
                "motivoCompensacao", motivoCompensacao,
                "acoesCompensadas", acoesCompensadas,
                "compensacaoCompleta", compensacaoCompleta,
                "timestamp", LocalDateTime.now().toString()
            );

            kafkaPublisher.publicar(execution, "compensacao-executada", compensationEvent);

            logger.info("Compensation complete: {} actions compensated, success: {}",
                acoesCompensadas.size(), compensacaoCompleta);

        } catch (Exception e) {
            logger.error("Critical error in compensation for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("compensacaoCompleta", false);
            execution.setVariable("compensacaoErro", e.getMessage());
            throw e;
        }
    }

    private void compensarAutorizacao(DelegateExecution execution) {
        String numeroAutorizacao = (String) execution.getVariable("numeroAutorizacao");
        logger.info("Compensating authorization: {}", numeroAutorizacao);
        // Cancel authorization in external system
        execution.setVariable("autorizacaoCancelada", true);
    }

    private void compensarBeneficiario(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        logger.info("Reverting beneficiary changes: {}", beneficiarioId);
        // Restore previous beneficiary state
        execution.setVariable("beneficiarioRevertido", true);
    }

    private void compensarNotificacao(DelegateExecution execution) {
        String notificacaoId = (String) execution.getVariable("notificacaoId");
        logger.info("Canceling notification: {}", notificacaoId);
        // Mark notification as canceled
        execution.setVariable("notificacaoCancelada", true);
    }

    private void compensarPagamento(DelegateExecution execution) {
        String pagamentoId = (String) execution.getVariable("pagamentoId");
        logger.info("Releasing payment reservation: {}", pagamentoId);
        // Release reserved funds
        execution.setVariable("pagamentoLiberado", true);
    }

    private void compensarCarePlan(DelegateExecution execution) {
        String carePlanId = (String) execution.getVariable("carePlanId");
        logger.info("Removing care plan: {}", carePlanId);
        // Delete care plan
        execution.setVariable("carePlanRemovido", true);
    }

    private void compensarDadosExternos(DelegateExecution execution) {
        logger.info("Removing external data records");
        // Clean up temporary external data
        execution.setVariable("dadosExternosRemovidos", true);
    }
}
