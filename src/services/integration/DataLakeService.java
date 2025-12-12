package br.com.austa.experiencia.service.integration;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Java Delegate para persistência no Data Lake
 *
 * Responsabilidades:
 * - Registrar perfis completos de beneficiários
 * - Armazenar jornadas e interações
 * - Persistir métricas de processo
 * - Consolidar dados analíticos
 *
 * Uso no BPMN:
 * <serviceTask id="ServiceTask_RegistrarPerfil"
 *              name="Registrar Perfil no Data Lake"
 *              camunda:delegateExpression="${dataLakeService}">
 * </serviceTask>
 */
@Component("dataLakeService")
public class DataLakeService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeService.class);

    @Value("${datalake.api.base-url}")
    private String dataLakeBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String operacao = (String) execution.getVariable("dataLakeOperacao");

        if (operacao == null) {
            operacao = "registrar_perfil";
        }

        LOGGER.info("Executando operação Data Lake: {} para processo: {}",
                   operacao, execution.getProcessInstanceId());

        try {
            switch (operacao.toLowerCase()) {
                case "registrar_perfil":
                    registrarPerfil(execution);
                    break;
                case "registrar_jornada":
                    registrarJornada(execution);
                    break;
                case "registrar_interacao":
                    registrarInteracao(execution);
                    break;
                case "consolidar_metricas":
                    consolidarMetricas(execution);
                    break;
                default:
                    throw new IllegalArgumentException("Operação inválida: " + operacao);
            }

            execution.setVariable("dataLakeSucesso", true);

        } catch (Exception e) {
            LOGGER.error("Erro ao persistir no Data Lake: {}", e.getMessage(), e);
            execution.setVariable("dataLakeSucesso", false);
            throw e;
        }
    }

    /**
     * Registra perfil completo do beneficiário no Data Lake
     *
     * Input:
     * - beneficiarioId, scoreRisco, classificacaoRisco, statusCPT,
     *   condicoesCronicas, fatoresRisco, respostasScreening
     *
     * Output:
     * - perfilRegistradoId (String)
     */
    @SuppressWarnings("unchecked")
    private void registrarPerfil(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        LOGGER.info("Registrando perfil completo do beneficiário {} no Data Lake", beneficiarioId);

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("perfil_id", UUID.randomUUID().toString());
        perfil.put("beneficiario_id", beneficiarioId);
        perfil.put("timestamp", Instant.now().toString());
        perfil.put("process_instance_id", execution.getProcessInstanceId());

        // Dados demográficos e clínicos
        perfil.put("idade", execution.getVariable("idade"));
        perfil.put("sexo", execution.getVariable("sexo"));
        perfil.put("bmi", execution.getVariable("bmi"));
        perfil.put("categoria_bmi", execution.getVariable("categoriaBMI"));

        // Scores de risco
        perfil.put("score_risco", execution.getVariable("scoreRisco"));
        perfil.put("classificacao_risco", execution.getVariable("classificacaoRisco"));
        perfil.put("score_comportamental", execution.getVariable("scoreComportamental"));
        perfil.put("score_predicao_internacao", execution.getVariable("scorePredicaoInternacao"));

        // Status clínicos
        perfil.put("status_cpt", execution.getVariable("statusCPT"));
        perfil.put("condicoes_suspeitas", execution.getVariable("condicoesSuspeitas"));
        perfil.put("condicoes_cronicas", execution.getVariable("condicoesCronicas"));
        perfil.put("fatores_risco", execution.getVariable("fatoresRisco"));

        // Dados comportamentais
        perfil.put("tabagista", execution.getVariable("tabagista"));
        perfil.put("sedentario", execution.getVariable("sedentario"));
        perfil.put("historico_familiar_positivo",
                  execution.getVariable("historicoFamiliarPositivo"));

        // Respostas do screening
        Map<String, Object> respostasScreening =
            (Map<String, Object>) execution.getVariable("respostasScreening");
        if (respostasScreening != null) {
            perfil.put("respostas_screening", respostasScreening);
        }

        // Plano de cuidados
        perfil.put("plano_cuidados", execution.getVariable("planoCuidados"));

        try {
            String url = dataLakeBaseUrl + "/api/v1/perfis";
            String response = restTemplate.postForObject(url, perfil, String.class);

            String perfilId = perfil.get("perfil_id").toString();
            execution.setVariable("perfilRegistradoId", perfilId);

            // Publicar evento de perfil completo
            execution.setVariable("eventoTipo", "BeneficiarioPerfilCompleto");
            kafkaPublisher.execute(execution);

            LOGGER.info("Perfil registrado com sucesso no Data Lake - ID: {}", perfilId);

        } catch (Exception e) {
            LOGGER.error("Erro ao registrar perfil no Data Lake", e);
            throw e;
        }
    }

    /**
     * Registra jornada completa do beneficiário
     *
     * Input:
     * - Dados consolidados da jornada
     *
     * Output:
     * - jornadaRegistradaId (String)
     */
    private void registrarJornada(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        LOGGER.info("Registrando jornada completa do beneficiário {} no Data Lake", beneficiarioId);

        Map<String, Object> jornada = new HashMap<>();
        jornada.put("jornada_id", UUID.randomUUID().toString());
        jornada.put("beneficiario_id", beneficiarioId);
        jornada.put("process_instance_id", execution.getProcessInstanceId());
        jornada.put("timestamp_inicio", execution.getVariable("dataInicio"));
        jornada.put("timestamp_fim", Instant.now().toString());

        // Métricas de tempo
        jornada.put("tempo_total_segundos", execution.getVariable("tempoTotal"));
        jornada.put("tempo_onboarding_segundos", execution.getVariable("tempoOnboarding"));

        // Touchpoints e interações
        jornada.put("total_touchpoints", execution.getVariable("totalTouchpoints"));
        jornada.put("canais_utilizados", execution.getVariable("canaisUtilizados"));
        jornada.put("total_interacoes", execution.getVariable("totalInteracoes"));

        // Desfechos
        jornada.put("desfecho", execution.getVariable("desfecho"));
        jornada.put("motivo_encerramento", execution.getVariable("motivoEncerramento"));

        // Métricas de experiência
        jornada.put("nps_score", execution.getVariable("npsScore"));
        jornada.put("ces_score", execution.getVariable("cesScore"));
        jornada.put("tentativas_resolucao", execution.getVariable("tentativasResolucao"));

        // Custos
        jornada.put("custo_total", execution.getVariable("custoTotal"));
        jornada.put("custo_por_touchpoint", execution.getVariable("custoPorTouchpoint"));

        // Estados transitados
        jornada.put("estados_transitados", execution.getVariable("estadosTransitados"));
        jornada.put("subprocessos_executados", execution.getVariable("subprocessosExecutados"));

        try {
            String url = dataLakeBaseUrl + "/api/v1/jornadas";
            String response = restTemplate.postForObject(url, jornada, String.class);

            String jornadaId = jornada.get("jornada_id").toString();
            execution.setVariable("jornadaRegistradaId", jornadaId);

            LOGGER.info("Jornada registrada com sucesso no Data Lake - ID: {}", jornadaId);

        } catch (Exception e) {
            LOGGER.error("Erro ao registrar jornada no Data Lake", e);
            throw e;
        }
    }

    /**
     * Registra interação individual no Data Lake
     *
     * Input:
     * - Dados da interação específica
     *
     * Output:
     * - interacaoRegistradaId (String)
     */
    private void registrarInteracao(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");

        LOGGER.info("Registrando interação do beneficiário {} no Data Lake", beneficiarioId);

        Map<String, Object> interacao = new HashMap<>();
        interacao.put("interacao_id", UUID.randomUUID().toString());
        interacao.put("beneficiario_id", beneficiarioId);
        interacao.put("timestamp", Instant.now().toString());

        // Dados da interação
        interacao.put("canal", execution.getVariable("canal"));
        interacao.put("tipo_interacao", execution.getVariable("tipoInteracao"));
        interacao.put("intencao_detectada", execution.getVariable("intencaoDetectada"));
        interacao.put("nivel_urgencia", execution.getVariable("nivelUrgencia"));

        // Classificação e roteamento
        interacao.put("camada_destino", execution.getVariable("camadaDestino"));
        interacao.put("complexidade", execution.getVariable("complexidadeInteracao"));
        interacao.put("sentimento_negativo", execution.getVariable("sentimentoNegativo"));

        // Resolução
        interacao.put("resolvido", execution.getVariable("casoResolvido"));
        interacao.put("tempo_resolucao", execution.getVariable("tempoResolucao"));
        interacao.put("tentativas_resolucao", execution.getVariable("tentativasResolucao"));

        // Conteúdo (anonimizado)
        interacao.put("conteudo_hash", execution.getVariable("conteudoHash"));
        interacao.put("palavras_chave", execution.getVariable("palavrasChave"));

        try {
            String url = dataLakeBaseUrl + "/api/v1/interacoes";
            String response = restTemplate.postForObject(url, interacao, String.class);

            String interacaoId = interacao.get("interacao_id").toString();
            execution.setVariable("interacaoRegistradaId", interacaoId);

            LOGGER.info("Interação registrada com sucesso no Data Lake - ID: {}", interacaoId);

        } catch (Exception e) {
            LOGGER.error("Erro ao registrar interação no Data Lake", e);
            throw e;
        }
    }

    /**
     * Consolida métricas agregadas do processo
     *
     * Input:
     * - Métricas coletadas durante o processo
     *
     * Output:
     * - metricasConsolidadas (Boolean)
     */
    private void consolidarMetricas(DelegateExecution execution) {
        String processDefinitionKey = execution.getProcessDefinitionId();

        LOGGER.info("Consolidando métricas para processo {}", processDefinitionKey);

        Map<String, Object> metricas = new HashMap<>();
        metricas.put("metrics_id", UUID.randomUUID().toString());
        metricas.put("process_definition_key", processDefinitionKey);
        metricas.put("process_instance_id", execution.getProcessInstanceId());
        metricas.put("timestamp", Instant.now().toString());

        // Métricas de performance
        metricas.put("duracao_total", execution.getVariable("duracaoTotal"));
        metricas.put("sla_cumprido", execution.getVariable("slaCumprido"));

        // Métricas de qualidade
        metricas.put("taxa_conclusao", execution.getVariable("taxaConclusao"));
        metricas.put("taxa_erro", execution.getVariable("taxaErro"));

        // Métricas de experiência
        metricas.put("nps_medio", execution.getVariable("npsMedio"));
        metricas.put("ces_medio", execution.getVariable("cesMedio"));

        // Métricas de custo
        metricas.put("custo_medio", execution.getVariable("custoMedio"));
        metricas.put("roi", execution.getVariable("roi"));

        try {
            String url = dataLakeBaseUrl + "/api/v1/metricas";
            String response = restTemplate.postForObject(url, metricas, String.class);

            execution.setVariable("metricasConsolidadas", true);

            LOGGER.info("Métricas consolidadas com sucesso no Data Lake");

        } catch (Exception e) {
            LOGGER.error("Erro ao consolidar métricas no Data Lake", e);
            throw e;
        }
    }
}
