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
 * Metricas Service - Performance Metrics Aggregation Delegate
 *
 * Consolidates metrics for SLA tracking, performance dashboards,
 * and operational analytics.
 *
 * BPMN Coverage:
 * - metricasService.consolidar (Consolidate general metrics)
 * - metricasService.consolidarProatividade (Consolidate proactive engine metrics)
 * - metricasService.consolidarFeedback (Consolidate feedback metrics)
 */
@Component("metricasService")
public class MetricasService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(MetricasService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("metricasMethod");

        if ("consolidar".equals(method)) {
            consolidar(execution);
        } else if ("consolidarProatividade".equals(method)) {
            consolidarProatividade(execution);
        } else if ("consolidarFeedback".equals(method)) {
            consolidarFeedback(execution);
        } else {
            logger.warn("Unknown metricas method: {}", method);
            throw new IllegalArgumentException("Invalid metricas method: " + method);
        }
    }

    /**
     * Consolidate general metrics
     *
     * Aggregates process performance metrics for SLA tracking
     * and operational reporting.
     *
     * @param execution Process execution context
     */
    public void consolidar(DelegateExecution execution) throws Exception {
        logger.info("Executing metricasService.consolidar for process {}",
            execution.getProcessInstanceId());

        try {
            Map<String, Object> metricas = new HashMap<>();

            // Process identification
            metricas.put("processInstanceId", execution.getProcessInstanceId());
            metricas.put("processDefinitionId", execution.getProcessDefinitionId());
            metricas.put("tipoProcesso", execution.getVariable("tipoProcesso"));
            metricas.put("timestamp", LocalDateTime.now().toString());

            // Performance metrics
            metricas.put("tempoExecucao", execution.getVariable("tempoExecucaoMinutos"));
            metricas.put("slaAlvo", execution.getVariable("slaAlvoMinutos"));
            metricas.put("slaAtendido", execution.getVariable("slaAtendido"));
            metricas.put("numeroInteracoes", execution.getVariable("numeroInteracoes"));

            // Quality metrics
            metricas.put("erros", execution.getVariable("numeroErros"));
            metricas.put("retentativas", execution.getVariable("numeroRetentativas"));
            metricas.put("escalacoes", execution.getVariable("numeroEscalacoes"));

            // Business metrics
            metricas.put("statusFinal", execution.getVariable("statusFinal"));
            metricas.put("resolvido", execution.getVariable("resolvido"));
            metricas.put("valorProcessado", execution.getVariable("valorProcessado"));

            // Dimensional context
            metricas.put("canal", execution.getVariable("canal"));
            metricas.put("produto", execution.getVariable("produto"));
            metricas.put("segmento", execution.getVariable("segmento"));

            // Save to Data Lake
            dataLakeService.salvar(execution, "metricas_processos",
                execution.getProcessInstanceId(), metricas);

            // Publish metrics event
            kafkaPublisher.publicar(execution, "metricas-consolidadas", metricas);

            execution.setVariable("metricasConsolidadas", true);

            logger.info("Metrics consolidated: process={}, SLA={}, duration={}min",
                execution.getProcessDefinitionId(),
                metricas.get("slaAtendido"),
                metricas.get("tempoExecucao"));

        } catch (Exception e) {
            logger.error("Error consolidating metrics for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("metricasErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Consolidate proactive engine metrics
     *
     * Aggregates metrics from proactive health management campaigns
     * including outreach effectiveness and engagement rates.
     *
     * @param execution Process execution context
     */
    public void consolidarProatividade(DelegateExecution execution) throws Exception {
        logger.info("Executing metricasService.consolidarProatividade for process {}",
            execution.getProcessInstanceId());

        try {
            Map<String, Object> metricasProatividade = new HashMap<>();

            // Campaign identification
            metricasProatividade.put("campanhaId", execution.getVariable("campanhaId"));
            metricasProatividade.put("tipoCampanha", execution.getVariable("tipoCampanha"));
            metricasProatividade.put("dataExecucao", LocalDateTime.now().toString());

            // Volume metrics
            metricasProatividade.put("baseAlvo", execution.getVariable("baseAlvoTotal"));
            metricasProatividade.put("contatosTentados", execution.getVariable("contatosTentados"));
            metricasProatividade.put("contatosRealizados", execution.getVariable("contatosRealizados"));
            metricasProatividade.put("contatosFalhados", execution.getVariable("contatosFalhados"));

            // Effectiveness metrics
            Integer contatosRealizados = (Integer) execution.getVariable("contatosRealizados");
            Integer contatosTentados = (Integer) execution.getVariable("contatosTentados");

            if (contatosTentados != null && contatosTentados > 0) {
                double taxaConexao = (contatosRealizados != null ? contatosRealizados : 0) * 100.0 / contatosTentados;
                metricasProatividade.put("taxaConexao", taxaConexao);
            }

            // Engagement metrics
            metricasProatividade.put("adesoes", execution.getVariable("numeroAdesoes"));
            metricasProatividade.put("recusas", execution.getVariable("numeroRecusas"));
            metricasProatividade.put("agendamentos", execution.getVariable("numeroAgendamentos"));

            Integer adesoes = (Integer) execution.getVariable("numeroAdesoes");
            if (contatosRealizados != null && contatosRealizados > 0) {
                double taxaAdesao = (adesoes != null ? adesoes : 0) * 100.0 / contatosRealizados;
                metricasProatividade.put("taxaAdesao", taxaAdesao);
            }

            // Cost and ROI
            metricasProatividade.put("custoTotal", execution.getVariable("custoTotalCampanha"));
            metricasProatividade.put("custoContato", execution.getVariable("custoMedioContato"));
            metricasProatividade.put("roiEstimado", execution.getVariable("roiEstimado"));

            // Save proactive metrics
            dataLakeService.salvar(execution, "metricas_proatividade",
                (String) execution.getVariable("campanhaId"), metricasProatividade);

            kafkaPublisher.publicar(execution, "proatividade-metricas", metricasProatividade);

            execution.setVariable("metricasProatividadeConsolidadas", true);

            logger.info("Proactive metrics consolidated: campaign={}, contacts={}, adhesion={}%",
                execution.getVariable("campanhaId"),
                contatosRealizados,
                metricasProatividade.get("taxaAdesao"));

        } catch (Exception e) {
            logger.error("Error consolidating proactive metrics for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("metricasProatividadeErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Consolidate feedback metrics
     *
     * Aggregates customer feedback metrics including NPS, CSAT,
     * and sentiment analysis for quality monitoring.
     *
     * @param execution Process execution context
     */
    public void consolidarFeedback(DelegateExecution execution) throws Exception {
        logger.info("Executing metricasService.consolidarFeedback for process {}",
            execution.getProcessInstanceId());

        try {
            Map<String, Object> metricasFeedback = new HashMap<>();

            // Feedback identification
            metricasFeedback.put("periodoColeta", execution.getVariable("periodoColeta"));
            metricasFeedback.put("canalFeedback", execution.getVariable("canalFeedback"));
            metricasFeedback.put("tipoJornada", execution.getVariable("tipoJornada"));
            metricasFeedback.put("timestamp", LocalDateTime.now().toString());

            // NPS metrics
            metricasFeedback.put("npsScore", execution.getVariable("npsScore"));
            metricasFeedback.put("npsPromotores", execution.getVariable("numeroPromotores"));
            metricasFeedback.put("npsDetratores", execution.getVariable("numeroDetratores"));
            metricasFeedback.put("npsNeutros", execution.getVariable("numeroNeutros"));

            Integer promotores = (Integer) execution.getVariable("numeroPromotores");
            Integer detratores = (Integer) execution.getVariable("numeroDetratores");
            Integer total = (Integer) execution.getVariable("totalRespostas");

            if (total != null && total > 0) {
                int npsCalculado = ((promotores != null ? promotores : 0) -
                                   (detratores != null ? detratores : 0)) * 100 / total;
                metricasFeedback.put("npsCalculado", npsCalculado);
            }

            // CSAT metrics
            metricasFeedback.put("csatMedio", execution.getVariable("csatMedio"));
            metricasFeedback.put("csatDistribuicao", execution.getVariable("csatDistribuicao"));

            // CES (Customer Effort Score) metrics
            metricasFeedback.put("cesScore", execution.getVariable("cesScore"));
            metricasFeedback.put("esforcoMedio", execution.getVariable("esforcoMedio"));

            // Sentiment analysis
            metricasFeedback.put("sentimentoPositivo", execution.getVariable("percentualPositivo"));
            metricasFeedback.put("sentimentoNegativo", execution.getVariable("percentualNegativo"));
            metricasFeedback.put("sentimentoNeutro", execution.getVariable("percentualNeutro"));

            // Themes and topics
            metricasFeedback.put("temasFrequentes", execution.getVariable("temasFrequentes"));
            metricasFeedback.put("palavrasChave", execution.getVariable("palavrasChave"));

            // Response rate
            Integer respostas = (Integer) execution.getVariable("totalRespostas");
            Integer solicitacoes = (Integer) execution.getVariable("totalSolicitacoes");

            if (solicitacoes != null && solicitacoes > 0) {
                double taxaResposta = (respostas != null ? respostas : 0) * 100.0 / solicitacoes;
                metricasFeedback.put("taxaResposta", taxaResposta);
            }

            // Save feedback metrics
            dataLakeService.salvar(execution, "metricas_feedback",
                execution.getProcessInstanceId(), metricasFeedback);

            kafkaPublisher.publicar(execution, "feedback-metricas", metricasFeedback);

            execution.setVariable("metricasFeedbackConsolidadas", true);

            logger.info("Feedback metrics consolidated: NPS={}, CSAT={}, responses={}",
                metricasFeedback.get("npsCalculado"),
                metricasFeedback.get("csatMedio"),
                respostas);

        } catch (Exception e) {
            logger.error("Error consolidating feedback metrics for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("metricasFeedbackErro", e.getMessage());
            throw e;
        }
    }
}
