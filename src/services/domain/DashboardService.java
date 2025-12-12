package br.com.austa.experiencia.service.domain;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard Service - Executive Dashboard Update Delegate
 *
 * Updates real-time KPIs and metrics on executive dashboards
 * and BI systems for monitoring and reporting.
 *
 * BPMN Coverage:
 * - dashboardService.atualizar (Update dashboard metrics)
 * - dashboardService.atualizarNPS (Update NPS metrics)
 */
@Component("dashboardService")
public class DashboardService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("dashboardMethod");

        if ("atualizar".equals(method)) {
            atualizar(execution);
        } else if ("atualizarNPS".equals(method)) {
            atualizarNPS(execution);
        } else {
            logger.warn("Unknown dashboard method: {}", method);
            throw new IllegalArgumentException("Invalid dashboard method: " + method);
        }
    }

    /**
     * Update dashboard metrics
     *
     * Pushes real-time KPIs to executive dashboard and BI system.
     * Metrics include: SLA compliance, resolution rates, satisfaction, volumes.
     *
     * @param execution Process execution context
     */
    public void atualizar(DelegateExecution execution) throws Exception {
        logger.info("Executing dashboardService.atualizar for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract metric data
            String tipoMetrica = (String) execution.getVariable("tipoMetrica");
            String categoria = (String) execution.getVariable("categoriaMetrica");
            Object valor = execution.getVariable("valorMetrica");
            String unidade = (String) execution.getVariable("unidadeMetrica");
            String periodo = (String) execution.getVariable("periodoMetrica");

            // Build dashboard update payload
            Map<String, Object> dashboardUpdate = new HashMap<>();
            dashboardUpdate.put("tipoMetrica", tipoMetrica);
            dashboardUpdate.put("categoria", categoria);
            dashboardUpdate.put("valor", valor);
            dashboardUpdate.put("unidade", unidade);
            dashboardUpdate.put("periodo", periodo);
            dashboardUpdate.put("timestamp", LocalDateTime.now().toString());
            dashboardUpdate.put("fonte", "BPMN_PROCESS");
            dashboardUpdate.put("processInstanceId", execution.getProcessInstanceId());

            // Enrich with dimensional context
            dashboardUpdate.put("canal", execution.getVariable("canal"));
            dashboardUpdate.put("produto", execution.getVariable("produto"));
            dashboardUpdate.put("fluxo", execution.getProcessDefinitionId());

            // Calculate derived metrics
            enrichMetrics(dashboardUpdate, execution);

            // Publish to Kafka for BI ingestion
            kafkaPublisher.publicar(execution, "dashboard-metrics", dashboardUpdate);

            // Store in Data Lake for historical analysis
            execution.setVariable("metricaPublicada", true);
            execution.setVariable("dashboardAtualizadoEm", LocalDateTime.now().toString());

            logger.info("Dashboard metric published: {} = {} {}", tipoMetrica, valor, unidade);

        } catch (Exception e) {
            logger.error("Error updating dashboard for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("dashboardErro", e.getMessage());
            // Non-critical error - don't throw to avoid breaking process flow
        }
    }

    /**
     * Update NPS metrics
     *
     * Specialized update for NPS (Net Promoter Score) metrics with
     * segmentation by customer profile and journey stage.
     *
     * @param execution Process execution context
     */
    public void atualizarNPS(DelegateExecution execution) throws Exception {
        logger.info("Executing dashboardService.atualizarNPS for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract NPS data
            Integer npsScore = (Integer) execution.getVariable("npsScore");
            String npsSegmento = (String) execution.getVariable("npsSegmento");
            String etapaJornada = (String) execution.getVariable("etapaJornada");
            String comentario = (String) execution.getVariable("npsComentario");

            // Classify NPS response
            String classificacaoNPS;
            if (npsScore >= 9) {
                classificacaoNPS = "PROMOTOR";
            } else if (npsScore >= 7) {
                classificacaoNPS = "NEUTRO";
            } else {
                classificacaoNPS = "DETRATOR";
            }

            // Build NPS update payload
            Map<String, Object> npsUpdate = new HashMap<>();
            npsUpdate.put("tipoMetrica", "NPS");
            npsUpdate.put("score", npsScore);
            npsUpdate.put("classificacao", classificacaoNPS);
            npsUpdate.put("segmento", npsSegmento);
            npsUpdate.put("etapaJornada", etapaJornada);
            npsUpdate.put("comentario", comentario);
            npsUpdate.put("timestamp", LocalDateTime.now().toString());
            npsUpdate.put("canal", execution.getVariable("canal"));
            npsUpdate.put("beneficiarioId", execution.getVariable("beneficiarioId"));
            npsUpdate.put("processInstanceId", execution.getProcessInstanceId());

            // Add sentiment analysis flag if negative
            if (npsScore <= 6 && comentario != null && !comentario.isEmpty()) {
                npsUpdate.put("requerAnaliseDetalhada", true);
                npsUpdate.put("prioridadeAnalise", "ALTA");
            }

            // Publish NPS event
            kafkaPublisher.publicar(execution, "nps-feedback", npsUpdate);

            execution.setVariable("npsPublicado", true);
            execution.setVariable("classificacaoNPS", classificacaoNPS);

            logger.info("NPS metric published: score={}, classification={}, stage={}",
                npsScore, classificacaoNPS, etapaJornada);

        } catch (Exception e) {
            logger.error("Error updating NPS for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("npsErro", e.getMessage());
            // Non-critical error - don't throw
        }
    }

    /**
     * Enrich metrics with calculated KPIs
     */
    private void enrichMetrics(Map<String, Object> metrics, DelegateExecution execution) {
        // Add SLA compliance indicator
        Boolean slaAtendido = (Boolean) execution.getVariable("slaAtendido");
        if (slaAtendido != null) {
            metrics.put("slaCompliance", slaAtendido ? 100 : 0);
        }

        // Add resolution indicator
        Boolean resolvido = (Boolean) execution.getVariable("resolvido");
        if (resolvido != null) {
            metrics.put("taxaResolucao", resolvido ? 100 : 0);
        }

        // Add efficiency score
        Object tempoProcessamento = execution.getVariable("tempoProcessamento");
        if (tempoProcessamento != null) {
            metrics.put("tempoProcessamentoMinutos", tempoProcessamento);
        }
    }
}
