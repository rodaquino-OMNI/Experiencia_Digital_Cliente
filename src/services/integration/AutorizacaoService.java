package br.com.austa.experiencia.service.integration;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Java Delegate para processamento de autorizações
 *
 * Responsabilidades:
 * - Gerar número de autorização
 * - Processar decisões de autorização
 * - Registrar autorizações no sistema
 * - Calcular custos estimados
 *
 * Uso no BPMN:
 * <serviceTask id="ServiceTask_GerarAutorizacao"
 *              name="Gerar Autorização"
 *              camunda:delegateExpression="${autorizacaoService}">
 * </serviceTask>
 */
@Component("autorizacaoService")
public class AutorizacaoService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutorizacaoService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String operacao = (String) execution.getVariable("autorizacaoOperacao");

        if (operacao == null) {
            operacao = "gerar";
        }

        LOGGER.info("Executando operação de autorização: {} para processo: {}",
                   operacao, execution.getProcessInstanceId());

        try {
            switch (operacao.toLowerCase()) {
                case "gerar":
                    gerarAutorizacao(execution);
                    break;
                case "atualizar":
                    atualizarAutorizacao(execution);
                    break;
                case "cancelar":
                    cancelarAutorizacao(execution);
                    break;
                default:
                    throw new IllegalArgumentException("Operação inválida: " + operacao);
            }

            execution.setVariable("autorizacaoSucesso", true);

        } catch (Exception e) {
            LOGGER.error("Erro ao processar autorização: {}", e.getMessage(), e);
            execution.setVariable("autorizacaoSucesso", false);
            throw e;
        }
    }

    /**
     * Gera nova autorização
     *
     * Input:
     * - beneficiarioId, tipoProcedimento, valorProcedimento,
     *   prestadorId, decisaoAutorizacao
     *
     * Output:
     * - numeroAutorizacao, dataAutorizacao, validadeAutorizacao
     */
    private void gerarAutorizacao(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String tipoProcedimento = (String) execution.getVariable("tipoProcedimento");
        String decisao = (String) execution.getVariable("decisaoAutorizacao");

        LOGGER.info("Gerando autorização para beneficiário {} - Procedimento: {} - Decisão: {}",
                   beneficiarioId, tipoProcedimento, decisao);

        // Gerar número único de autorização
        String numeroAutorizacao = gerarNumeroAutorizacao();

        Map<String, Object> autorizacao = new HashMap<>();
        autorizacao.put("numero_autorizacao", numeroAutorizacao);
        autorizacao.put("beneficiario_id", beneficiarioId);
        autorizacao.put("tipo_procedimento", tipoProcedimento);
        autorizacao.put("decisao", decisao);
        autorizacao.put("data_autorizacao", Instant.now().toString());

        // Dados do procedimento
        autorizacao.put("valor_procedimento", execution.getVariable("valorProcedimento"));
        autorizacao.put("prestador_id", execution.getVariable("prestadorId"));
        autorizacao.put("prestador_nome", execution.getVariable("prestadorNome"));

        // Dados da decisão
        autorizacao.put("motivo_decisao", execution.getVariable("motivoDecisao"));
        autorizacao.put("requer_analise_tecnica", execution.getVariable("requerAnaliseTecnica"));
        autorizacao.put("atende_protocolo", execution.getVariable("atendeProtocolo"));

        // Validade da autorização (30 dias para aprovadas)
        if ("APROVADO".equals(decisao)) {
            String validadeAutorizacao = Instant.now().plusSeconds(30 * 24 * 60 * 60).toString();
            autorizacao.put("validade_autorizacao", validadeAutorizacao);
            execution.setVariable("validadeAutorizacao", validadeAutorizacao);
        }

        // Dados de auditoria
        autorizacao.put("process_instance_id", execution.getProcessInstanceId());
        autorizacao.put("usuario_aprovador", execution.getVariable("usuarioAprovador"));

        try {
            // Registrar no sistema de autorizações
            String url = "http://autorizacao-service/api/v1/autorizacoes";
            restTemplate.postForObject(url, autorizacao, String.class);

            execution.setVariable("numeroAutorizacao", numeroAutorizacao);
            execution.setVariable("dataAutorizacao", autorizacao.get("data_autorizacao"));

            // Publicar evento
            execution.setVariable("eventoTipo", "AutorizacaoProcessada");
            kafkaPublisher.execute(execution);

            LOGGER.info("Autorização gerada com sucesso: {}", numeroAutorizacao);

        } catch (Exception e) {
            LOGGER.error("Erro ao gerar autorização", e);
            throw e;
        }
    }

    /**
     * Atualiza status de autorização existente
     */
    private void atualizarAutorizacao(DelegateExecution execution) {
        String numeroAutorizacao = (String) execution.getVariable("numeroAutorizacao");
        String novoStatus = (String) execution.getVariable("novoStatusAutorizacao");

        LOGGER.info("Atualizando autorização {} para status: {}", numeroAutorizacao, novoStatus);

        Map<String, Object> atualizacao = new HashMap<>();
        atualizacao.put("status", novoStatus);
        atualizacao.put("data_atualizacao", Instant.now().toString());
        atualizacao.put("motivo_atualizacao", execution.getVariable("motivoAtualizacao"));

        try {
            String url = String.format("http://autorizacao-service/api/v1/autorizacoes/%s",
                                      numeroAutorizacao);
            restTemplate.put(url, atualizacao);

            LOGGER.info("Autorização atualizada com sucesso");

        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar autorização", e);
            throw e;
        }
    }

    /**
     * Cancela autorização
     */
    private void cancelarAutorizacao(DelegateExecution execution) {
        String numeroAutorizacao = (String) execution.getVariable("numeroAutorizacao");
        String motivoCancelamento = (String) execution.getVariable("motivoCancelamento");

        LOGGER.info("Cancelando autorização {} - Motivo: {}", numeroAutorizacao, motivoCancelamento);

        Map<String, Object> cancelamento = new HashMap<>();
        cancelamento.put("status", "CANCELADO");
        cancelamento.put("data_cancelamento", Instant.now().toString());
        cancelamento.put("motivo_cancelamento", motivoCancelamento);

        try {
            String url = String.format("http://autorizacao-service/api/v1/autorizacoes/%s/cancelar",
                                      numeroAutorizacao);
            restTemplate.put(url, cancelamento);

            execution.setVariable("autorizacaoCancelada", true);

            LOGGER.info("Autorização cancelada com sucesso");

        } catch (Exception e) {
            LOGGER.error("Erro ao cancelar autorização", e);
            throw e;
        }
    }

    /**
     * Gera número único de autorização no formato: AUT-YYYY-NNNNNNNN
     */
    private String gerarNumeroAutorizacao() {
        String ano = String.valueOf(java.time.Year.now().getValue());
        String sequencial = String.format("%08d", (int) (Math.random() * 100000000));
        return String.format("AUT-%s-%s", ano, sequencial);
    }
}
