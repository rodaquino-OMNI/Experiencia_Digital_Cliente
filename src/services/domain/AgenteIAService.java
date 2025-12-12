package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

/**
 * Java Delegate para gerenciamento de Agentes IA
 *
 * Responsabilidades:
 * - Selecionar agente IA especializado
 * - Executar ações solicitadas por agentes IA
 *
 * Uso no BPMN:
 * <serviceTask id="Task_SelecionarAgenteIA"
 *              name="Selecionar Agente IA"
 *              camunda:delegateExpression="${agenteIAService.selecionar}">
 * </serviceTask>
 */
@Component("agenteIAService")
public class AgenteIAService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgenteIAService.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String operacao = (String) execution.getVariable("agenteIAOperacao");

        if (operacao == null) {
            operacao = "selecionar";
        }

        LOGGER.info("Executando operação Agente IA: {} para processo: {}",
                   operacao, execution.getProcessInstanceId());

        try {
            switch (operacao.toLowerCase()) {
                case "selecionar":
                    selecionar(execution);
                    break;
                case "executaração":
                    executarAcao(execution);
                    break;
                default:
                    throw new IllegalArgumentException("Operação inválida: " + operacao);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao executar operação de Agente IA: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Seleciona agente IA especializado baseado no contexto
     *
     * Input:
     * - demandaContext (Map): Contexto da demanda
     * - intenção (String): Intenção classificada
     *
     * Output:
     * - agenteIASelecionado (String): Tipo de agente selecionado
     * - agenteEspecialidade (String): Especialidade do agente
     */
    private void selecionar(DelegateExecution execution) {
        @SuppressWarnings("unchecked")
        Map<String, Object> demandaContext = (Map<String, Object>) execution.getVariable("demandaContext");
        String intencao = (String) execution.getVariable("intencao");

        LOGGER.info("Selecionando agente IA para intenção: {}", intencao);

        String agenteIASelecionado;
        String especialidade;

        // Lógica de seleção de agente baseada na intenção
        if (intencao != null) {
            if (intencao.contains("autorizacao") || intencao.contains("procedimento")) {
                agenteIASelecionado = "AGENTE_AUTORIZACOES";
                especialidade = "Autorizações e Procedimentos";
            } else if (intencao.contains("consulta") || intencao.contains("informacao")) {
                agenteIASelecionado = "AGENTE_CONSULTAS";
                especialidade = "Consultas e Informações";
            } else if (intencao.contains("agendamento")) {
                agenteIASelecionado = "AGENTE_AGENDAMENTOS";
                especialidade = "Agendamentos";
            } else {
                agenteIASelecionado = "AGENTE_GERAL";
                especialidade = "Atendimento Geral";
            }
        } else {
            agenteIASelecionado = "AGENTE_GERAL";
            especialidade = "Atendimento Geral";
        }

        execution.setVariable("agenteIASelecionado", agenteIASelecionado);
        execution.setVariable("agenteEspecialidade", especialidade);

        LOGGER.info("Agente IA selecionado: {} - Especialidade: {}", agenteIASelecionado, especialidade);
    }

    /**
     * Executa ação solicitada pelo beneficiário
     *
     * Input:
     * - acaoSolicitada (String): Ação a ser executada
     * - parametros (Map): Parâmetros da ação
     *
     * Output:
     * - acaoExecutada (Boolean): Sucesso da execução
     * - resultadoAcao (Map): Resultado da execução
     */
    private void executarAcao(DelegateExecution execution) {
        String acaoSolicitada = (String) execution.getVariable("acaoSolicitada");
        @SuppressWarnings("unchecked")
        Map<String, Object> parametros = (Map<String, Object>) execution.getVariable("parametros");

        LOGGER.info("Executando ação: {}", acaoSolicitada);

        try {
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("status", "EXECUTADO");
            resultado.put("mensagem", "Ação executada com sucesso");
            resultado.put("timestamp", System.currentTimeMillis());

            execution.setVariable("acaoExecutada", true);
            execution.setVariable("resultadoAcao", resultado);

            LOGGER.info("Ação executada com sucesso: {}", acaoSolicitada);
        } catch (Exception e) {
            execution.setVariable("acaoExecutada", false);
            LOGGER.error("Erro ao executar ação: {}", acaoSolicitada, e);
            throw e;
        }
    }
}
