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
 * Jornada Service - Customer Journey Tracking Delegate
 *
 * Tracks journey milestones, consolidates outcomes, and maintains
 * comprehensive audit trail of customer interactions.
 *
 * BPMN Coverage:
 * - jornadaService.consolidarDesfechos (Consolidate journey outcomes)
 * - jornadaService.registrarEtapa (Register journey milestone)
 */
@Component("jornadaService")
public class JornadaService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(JornadaService.class);

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private KafkaPublisherService kafkaPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("jornadaMethod");

        if ("consolidarDesfechos".equals(method)) {
            consolidarDesfechos(execution);
        } else if ("registrarEtapa".equals(method)) {
            registrarEtapa(execution);
        } else {
            logger.warn("Unknown jornada method: {}", method);
            throw new IllegalArgumentException("Invalid jornada method: " + method);
        }
    }

    /**
     * Consolidate journey outcomes
     *
     * Aggregates all outcomes and metrics from completed journey
     * for reporting, analytics, and continuous improvement.
     *
     * @param execution Process execution context
     */
    public void consolidarDesfechos(DelegateExecution execution) throws Exception {
        logger.info("Executing jornadaService.consolidarDesfechos for process {}",
            execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String tipoJornada = (String) execution.getVariable("tipoJornada");

            // Collect all journey data
            Map<String, Object> desfechoConsolidado = new HashMap<>();

            // Basic journey info
            desfechoConsolidado.put("beneficiarioId", beneficiarioId);
            desfechoConsolidado.put("processInstanceId", execution.getProcessInstanceId());
            desfechoConsolidado.put("tipoJornada", tipoJornada);
            desfechoConsolidado.put("dataInicio", execution.getVariable("dataInicioJornada"));
            desfechoConsolidado.put("dataFim", LocalDateTime.now().toString());

            // Outcomes
            desfechoConsolidado.put("statusFinal", execution.getVariable("statusFinal"));
            desfechoConsolidado.put("resolvido", execution.getVariable("resolvido"));
            desfechoConsolidado.put("motivoFinalizacao", execution.getVariable("motivoFinalizacao"));

            // Performance metrics
            desfechoConsolidado.put("slaAtendido", execution.getVariable("slaAtendido"));
            desfechoConsolidado.put("tempoTotal", execution.getVariable("tempoTotalMinutos"));
            desfechoConsolidado.put("numeroInteracoes", execution.getVariable("numeroInteracoes"));
            desfechoConsolidado.put("canaisUtilizados", execution.getVariable("canaisUtilizados"));

            // Quality metrics
            desfechoConsolidado.put("npsScore", execution.getVariable("npsScore"));
            desfechoConsolidado.put("satisfacao", execution.getVariable("satisfacao"));
            desfechoConsolidado.put("esforcoCliente", execution.getVariable("esforcoCliente"));

            // Business outcomes
            desfechoConsolidado.put("autorizacoesConcedidas", execution.getVariable("autorizacoesConcedidas"));
            desfechoConsolidado.put("valorTotal", execution.getVariable("valorTotalAutorizado"));
            desfechoConsolidado.put("negativas", execution.getVariable("numeroNegativas"));
            desfechoConsolidado.put("motivosNegativas", execution.getVariable("motivosNegativas"));

            // Milestones achieved
            List<Map<String, Object>> etapas = (List<Map<String, Object>>)
                execution.getVariable("etapasPercorridas");
            desfechoConsolidado.put("etapasPercorridas", etapas != null ? etapas : new ArrayList<>());
            desfechoConsolidado.put("numeroEtapas", etapas != null ? etapas.size() : 0);

            // Cost and efficiency
            desfechoConsolidado.put("custoOperacional", calcularCustoOperacional(execution));
            desfechoConsolidado.put("eficienciaScore", calcularEficienciaScore(execution));

            // Save consolidated outcome to Data Lake
            dataLakeService.salvar(execution,
                "jornadas_consolidadas",
                execution.getProcessInstanceId(),
                desfechoConsolidado);

            // Publish outcome event
            kafkaPublisher.publicar(execution, "jornada-concluida", desfechoConsolidado);

            execution.setVariable("desfechoConsolidado", true);
            execution.setVariable("desfechoData", desfechoConsolidado);

            logger.info("Journey outcome consolidated: type={}, status={}, duration={}min",
                tipoJornada,
                desfechoConsolidado.get("statusFinal"),
                desfechoConsolidado.get("tempoTotal"));

        } catch (Exception e) {
            logger.error("Error consolidating journey outcomes for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("consolidacaoErro", e.getMessage());
            throw e;
        }
    }

    /**
     * Register journey milestone
     *
     * Records significant milestones in customer journey for tracking
     * progress and enabling process analytics.
     *
     * @param execution Process execution context
     */
    public void registrarEtapa(DelegateExecution execution) throws Exception {
        logger.info("Executing jornadaService.registrarEtapa for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract milestone data
            String etapaNome = (String) execution.getVariable("etapaNome");
            String etapaDescricao = (String) execution.getVariable("etapaDescricao");
            String statusEtapa = (String) execution.getVariable("statusEtapa");

            // Build milestone record
            Map<String, Object> etapa = new HashMap<>();
            etapa.put("nome", etapaNome);
            etapa.put("descricao", etapaDescricao);
            etapa.put("status", statusEtapa);
            etapa.put("timestamp", LocalDateTime.now().toString());
            etapa.put("activityId", execution.getCurrentActivityId());
            etapa.put("activityName", execution.getCurrentActivityName());

            // Add contextual data
            etapa.put("canal", execution.getVariable("canal"));
            etapa.put("agenteResponsavel", execution.getVariable("agenteResponsavel"));
            etapa.put("tempoNaEtapa", execution.getVariable("tempoNaEtapa"));

            // Retrieve existing milestones
            List<Map<String, Object>> etapasPercorridas =
                (List<Map<String, Object>>) execution.getVariable("etapasPercorridas");

            if (etapasPercorridas == null) {
                etapasPercorridas = new ArrayList<>();
            }

            // Add new milestone
            etapasPercorridas.add(etapa);
            execution.setVariable("etapasPercorridas", etapasPercorridas);
            execution.setVariable("ultimaEtapa", etapaNome);
            execution.setVariable("numeroEtapas", etapasPercorridas.size());

            // Publish milestone event for real-time monitoring
            Map<String, Object> milestoneEvent = new HashMap<>();
            milestoneEvent.put("processInstanceId", execution.getProcessInstanceId());
            milestoneEvent.put("beneficiarioId", execution.getVariable("beneficiarioId"));
            milestoneEvent.put("etapa", etapa);
            milestoneEvent.put("progressoPercentual", calcularProgresso(etapaNome));

            kafkaPublisher.publicar(execution, "jornada-milestone", milestoneEvent);

            logger.info("Journey milestone registered: {} - {}", etapaNome, statusEtapa);

        } catch (Exception e) {
            logger.error("Error registering journey milestone for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("registroEtapaErro", e.getMessage());
            // Non-critical - don't throw
        }
    }

    /**
     * Calculate operational cost of journey
     */
    private double calcularCustoOperacional(DelegateExecution execution) {
        // Simplified cost calculation
        Integer numeroInteracoes = (Integer) execution.getVariable("numeroInteracoes");
        Integer tempoTotal = (Integer) execution.getVariable("tempoTotalMinutos");

        if (numeroInteracoes == null) numeroInteracoes = 0;
        if (tempoTotal == null) tempoTotal = 0;

        // Cost per interaction: R$ 5.00, Cost per minute: R$ 0.50
        return (numeroInteracoes * 5.0) + (tempoTotal * 0.5);
    }

    /**
     * Calculate efficiency score (0-100)
     */
    private int calcularEficienciaScore(DelegateExecution execution) {
        int score = 100;

        // Deduct points for SLA misses
        Boolean slaAtendido = (Boolean) execution.getVariable("slaAtendido");
        if (Boolean.FALSE.equals(slaAtendido)) {
            score -= 30;
        }

        // Deduct points for excessive interactions
        Integer numeroInteracoes = (Integer) execution.getVariable("numeroInteracoes");
        if (numeroInteracoes != null && numeroInteracoes > 3) {
            score -= (numeroInteracoes - 3) * 5;
        }

        // Deduct points for low satisfaction
        Integer npsScore = (Integer) execution.getVariable("npsScore");
        if (npsScore != null && npsScore < 7) {
            score -= 20;
        }

        return Math.max(score, 0);
    }

    /**
     * Calculate journey progress percentage
     */
    private int calcularProgresso(String etapaNome) {
        // Simplified progress calculation based on milestone
        Map<String, Integer> etapasPeso = Map.of(
            "INICIO", 10,
            "VALIDACAO", 25,
            "ANALISE", 50,
            "DECISAO", 75,
            "CONCLUSAO", 90,
            "FINALIZACAO", 100
        );

        return etapasPeso.getOrDefault(etapaNome.toUpperCase(), 50);
    }
}
