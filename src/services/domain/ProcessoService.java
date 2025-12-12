package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Java Delegate para gerenciamento de processos
 *
 * Responsabilidades:
 * - Encerrar todos os processos ativos
 * - Suspender todos os processos
 * - Reativar processos suspensos
 *
 * Uso no BPMN:
 * <serviceTask id="Task_EncerrarProcessos"
 *              name="Encerrar Processos"
 *              camunda:delegateExpression="${processoService.encerrarTodos}">
 * </serviceTask>
 */
@Component("processoService")
public class ProcessoService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessoService.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String operacao = (String) execution.getVariable("processoOperacao");

        if (operacao == null) {
            operacao = "encerrarTodos";
        }

        LOGGER.info("Executando operação de processo: {} para processo: {}",
                   operacao, execution.getProcessInstanceId());

        try {
            switch (operacao.toLowerCase()) {
                case "encerrartodos":
                    encerrarTodos(execution);
                    break;
                case "suspendertodos":
                    suspenderTodos(execution);
                    break;
                case "reatívartodos":
                    reativarTodos(execution);
                    break;
                default:
                    throw new IllegalArgumentException("Operação inválida: " + operacao);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao executar operação de processo: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Encerra todos os processos ativos do beneficiário
     *
     * Input:
     * - beneficiarioId (String)
     *
     * Output:
     * - processosEncerrados (Integer): Número de processos encerrados
     */
    private void encerrarTodos(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        LOGGER.info("Encerrando todos os processos para beneficiário: {}", beneficiarioId);

        // Simulação de encerramento de processos
        int processosEncerrados = 0; // Aqui seria implementada a lógica real

        execution.setVariable("processosEncerrados", processosEncerrados);

        LOGGER.info("Processos encerrados: {}", processosEncerrados);
    }

    /**
     * Suspende todos os processos ativos do beneficiário
     *
     * Input:
     * - beneficiarioId (String)
     *
     * Output:
     * - processosSuspensos (Integer): Número de processos suspensos
     */
    private void suspenderTodos(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        LOGGER.info("Suspendendo todos os processos para beneficiário: {}", beneficiarioId);

        // Simulação de suspensão de processos
        int processosSuspensos = 0; // Aqui seria implementada a lógica real

        execution.setVariable("processosSuspensos", processosSuspensos);

        LOGGER.info("Processos suspensos: {}", processosSuspensos);
    }

    /**
     * Reativa todos os processos suspensos do beneficiário
     *
     * Input:
     * - beneficiarioId (String)
     *
     * Output:
     * - processosReativados (Integer): Número de processos reativados
     */
    private void reativarTodos(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        LOGGER.info("Reativando todos os processos para beneficiário: {}", beneficiarioId);

        // Simulação de reativação de processos
        int processosReativados = 0; // Aqui seria implementada a lógica real

        execution.setVariable("processosReativados", processosReativados);

        LOGGER.info("Processos reativados: {}", processosReativados);
    }
}
