package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

/**
 * Java Delegate para gerenciamento de contexto do beneficiário
 *
 * Responsabilidades:
 * - Inicializar contexto do beneficiário
 * - Atualizar estado do beneficiário
 * - Enriquecer contexto com dados adicionais
 * - Enriquecer contexto 360°
 *
 * Uso no BPMN:
 * <serviceTask id="Task_InicializarContexto"
 *              name="Inicializar Contexto"
 *              camunda:delegateExpression="${contextoService.inicializar}">
 * </serviceTask>
 */
@Component("contextoService")
public class ContextoService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextoService.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String operacao = (String) execution.getVariable("contextoOperacao");

        if (operacao == null) {
            operacao = "inicializar";
        }

        LOGGER.info("Executando operação de contexto: {} para processo: {}",
                   operacao, execution.getProcessInstanceId());

        try {
            switch (operacao.toLowerCase()) {
                case "inicializar":
                    inicializar(execution);
                    break;
                case "atualizarestado":
                    atualizarEstado(execution);
                    break;
                case "enriquecer":
                    enriquecer(execution);
                    break;
                case "enriquecer360":
                    enriquecer360(execution);
                    break;
                default:
                    throw new IllegalArgumentException("Operação inválida: " + operacao);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao executar operação de contexto: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Inicializa contexto do beneficiário
     *
     * Input:
     * - beneficiarioId (String)
     * - contratoId (String)
     *
     * Output:
     * - estadoBeneficiario (String): Estado inicial (NOVO)
     * - contextoInicial (Map): Contexto inicializado
     */
    private void inicializar(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String contratoId = (String) execution.getVariable("contratoId");

        LOGGER.info("Inicializando contexto para beneficiário: {}", beneficiarioId);

        Map<String, Object> contextoInicial = new HashMap<>();
        contextoInicial.put("beneficiarioId", beneficiarioId);
        contextoInicial.put("contratoId", contratoId);
        contextoInicial.put("estado", "NOVO");
        contextoInicial.put("dataInicializacao", LocalDateTime.now().toString());
        contextoInicial.put("historicoEstados", new java.util.ArrayList<>());

        execution.setVariable("estadoBeneficiario", "NOVO");
        execution.setVariable("contextoInicial", contextoInicial);

        LOGGER.info("Contexto inicializado para beneficiário: {}", beneficiarioId);
    }

    /**
     * Atualiza estado do beneficiário
     *
     * Input:
     * - novoEstado (String): Novo estado (ATIVO, INATIVO, etc.)
     *
     * Output:
     * - estadoBeneficiario (String): Estado atualizado
     * - dataAtualizacao (String): Data da atualização
     */
    private void atualizarEstado(DelegateExecution execution) {
        String novoEstado = (String) execution.getVariable("novoEstado");
        String estadoAnterior = (String) execution.getVariable("estadoBeneficiario");

        LOGGER.info("Atualizando estado de {} para {}", estadoAnterior, novoEstado);

        execution.setVariable("estadoBeneficiario", novoEstado);
        execution.setVariable("dataAtualizacao", LocalDateTime.now().toString());
        execution.setVariable("estadoAnterior", estadoAnterior);

        LOGGER.info("Estado atualizado para: {}", novoEstado);
    }

    /**
     * Enriquece contexto com dados adicionais
     *
     * Input:
     * - beneficiarioId (String)
     *
     * Output:
     * - contextoEnriquecido (Map): Contexto enriquecido
     */
    private void enriquecer(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        LOGGER.info("Enriquecendo contexto para beneficiário: {}", beneficiarioId);

        Map<String, Object> contextoEnriquecido = new HashMap<>();
        contextoEnriquecido.put("dataEnriquecimento", LocalDateTime.now().toString());
        contextoEnriquecido.put("fontesDados", java.util.Arrays.asList("Tasy", "DataLake", "CRM"));

        execution.setVariable("contextoEnriquecido", contextoEnriquecido);

        LOGGER.info("Contexto enriquecido para beneficiário: {}", beneficiarioId);
    }

    /**
     * Enriquece contexto 360° do beneficiário
     *
     * Input:
     * - beneficiarioId (String)
     *
     * Output:
     * - contexto360 (Map): Visão 360° do beneficiário
     */
    private void enriquecer360(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        LOGGER.info("Enriquecendo contexto 360° para beneficiário: {}", beneficiarioId);

        Map<String, Object> contexto360 = new HashMap<>();
        contexto360.put("historico_utilizacoes", new java.util.ArrayList<>());
        contexto360.put("programas_ativos", new java.util.ArrayList<>());
        contexto360.put("autorizacoes_pendentes", new java.util.ArrayList<>());
        contexto360.put("nps_historico", new java.util.ArrayList<>());
        contexto360.put("jornadas_ativas", new java.util.ArrayList<>());

        execution.setVariable("contexto360", contexto360);

        LOGGER.info("Contexto 360° enriquecido para beneficiário: {}", beneficiarioId);
    }
}
