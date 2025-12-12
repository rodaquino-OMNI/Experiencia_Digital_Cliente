package br.com.austa.experiencia.service.domain.navegacao.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate responsible for closing care journey and finalizing patient care episode.
 *
 * <p>SUB-007 Navegação - Journey Closure</p>
 *
 * <p>Functionality:</p>
 * <ul>
 *   <li>Finalizes care journey and closes all open items</li>
 *   <li>Calculates journey metrics and outcomes</li>
 *   <li>Archives journey documentation</li>
 *   <li>Triggers post-journey follow-up and satisfaction survey</li>
 * </ul>
 *
 * <p>Input Variables:</p>
 * <ul>
 *   <li><b>jornadaId</b> (String): Care journey ID</li>
 *   <li><b>beneficiarioId</b> (String): Beneficiary ID</li>
 *   <li><b>motivoEncerramento</b> (String): Closure reason</li>
 *   <li><b>desfechoFinal</b> (String): Final outcome classification</li>
 * </ul>
 *
 * <p>Output Variables:</p>
 * <ul>
 *   <li><b>jornadaEncerrada</b> (Boolean): Journey closed flag</li>
 *   <li><b>dataEncerramento</b> (LocalDateTime): Closure timestamp</li>
 *   <li><b>metricasJornada</b> (Map): Journey performance metrics</li>
 *   <li><b>pesquisaSatisfacaoEnviada</b> (Boolean): Survey sent flag</li>
 *   <li><b>arquivamentoCompleto</b> (Boolean): Archival complete flag</li>
 * </ul>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("encerrarJornadaDelegate")
public class EncerrarJornadaDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting journey closure - Process: {}, Execution: {}",
                execution.getProcessInstanceId(), execution.getId());

        try {
            // Extract input parameters
            String jornadaId = (String) execution.getVariable("jornadaId");
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String motivoEncerramento = (String) execution.getVariable("motivoEncerramento");
            String desfechoFinal = (String) execution.getVariable("desfechoFinal");

            if (motivoEncerramento == null) {
                motivoEncerramento = "COMPLETION";
            }

            log.debug("Closing journey: {}, beneficiary: {}, reason: {}, outcome: {}",
                    jornadaId, beneficiarioId, motivoEncerramento, desfechoFinal);

            // Calculate journey metrics
            Map<String, Object> metricas = calculateJourneyMetrics(jornadaId, execution);

            // Close all open items
            closeOpenItems(jornadaId);

            // Archive journey documentation
            boolean arquivamentoCompleto = archiveJourneyDocumentation(jornadaId, metricas);

            // Update beneficiary record
            updateBeneficiaryRecord(beneficiarioId, jornadaId, desfechoFinal);

            // Send satisfaction survey
            boolean pesquisaEnviada = sendSatisfactionSurvey(beneficiarioId, jornadaId);

            // Schedule follow-up if needed
            scheduleFollowUp(jornadaId, beneficiarioId, desfechoFinal);

            // Update analytics and reporting
            updateAnalytics(jornadaId, metricas, desfechoFinal);

            // Set output variables
            LocalDateTime dataEncerramento = LocalDateTime.now();
            execution.setVariable("jornadaEncerrada", true);
            execution.setVariable("dataEncerramento", dataEncerramento);
            execution.setVariable("metricasJornada", metricas);
            execution.setVariable("pesquisaSatisfacaoEnviada", pesquisaEnviada);
            execution.setVariable("arquivamentoCompleto", arquivamentoCompleto);

            log.info("Successfully closed journey - ID: {}, Beneficiary: {}, Duration: {} days, Outcome: {}",
                    jornadaId, beneficiarioId, metricas.get("duracaoTotal"), desfechoFinal);

        } catch (Exception e) {
            log.error("Error closing care journey - Process: {}, Error: {}",
                    execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("errorMessage", "Failed to close journey: " + e.getMessage());
            execution.setVariable("hasError", true);
            execution.setVariable("jornadaEncerrada", false);

            throw new RuntimeException("Error in EncerrarJornadaDelegate", e);
        }
    }

    /**
     * Calculates comprehensive journey performance metrics.
     */
    private Map<String, Object> calculateJourneyMetrics(String jornadaId, DelegateExecution execution) {
        Map<String, Object> metricas = new HashMap<>();

        // Get journey start date
        LocalDateTime dataInicio = getJourneyStartDate(jornadaId);
        LocalDateTime dataFim = LocalDateTime.now();

        // Duration metrics
        long duracaoTotal = Duration.between(dataInicio, dataFim).toDays();
        metricas.put("duracaoTotal", duracaoTotal);
        metricas.put("dataInicio", dataInicio);
        metricas.put("dataFim", dataFim);

        // Stage completion metrics
        metricas.put("etapasCompletadas", countCompletedStages(jornadaId));
        metricas.put("etapasTotais", getTotalStages(jornadaId));
        metricas.put("taxaConclusao", calculateCompletionRate(jornadaId));

        // Adherence metrics
        metricas.put("aderenciaMedia", execution.getVariable("aderencia"));
        metricas.put("consultasRealizadas", countCompletedAppointments(jornadaId));
        metricas.put("consultasProgramadas", getTotalScheduledAppointments(jornadaId));

        // Quality metrics
        metricas.put("desfechoFinal", execution.getVariable("desfechoFinal"));
        metricas.put("promScoreFinal", execution.getVariable("promScore"));
        metricas.put("readmissoes", countReadmissions(jornadaId));
        metricas.put("complicacoes", countComplications(jornadaId));

        // Cost metrics
        metricas.put("custoTotal", calculateTotalCost(jornadaId));
        metricas.put("custoPrevisto", getEstimatedCost(jornadaId));
        metricas.put("varianceCusto", calculateCostVariance(jornadaId));

        // Engagement metrics
        metricas.put("interacoesApp", countAppInteractions(jornadaId));
        metricas.put("mensagensEnviadas", countSentMessages(jornadaId));
        metricas.put("respostasRecebidas", countReceivedResponses(jornadaId));

        return metricas;
    }

    /**
     * Closes all open items associated with the journey.
     */
    private void closeOpenItems(String jornadaId) {
        // Production: Close pending tasks, appointments, reminders, etc.
        log.debug("Closing all open items for journey: {}", jornadaId);
    }

    /**
     * Archives journey documentation for regulatory compliance.
     */
    private boolean archiveJourneyDocumentation(String jornadaId, Map<String, Object> metricas) {
        // Production: Archive to document management system
        // Ensure 10-year retention for regulatory compliance
        log.debug("Archiving journey documentation: {}", jornadaId);
        return true; // Simulated success
    }

    /**
     * Updates beneficiary record with journey completion.
     */
    private void updateBeneficiaryRecord(String beneficiarioId, String jornadaId, String desfecho) {
        // Production: Update beneficiary_journeys table
        log.debug("Updated beneficiary record - ID: {}, Journey: {}, Outcome: {}",
                beneficiarioId, jornadaId, desfecho);
    }

    /**
     * Sends satisfaction survey to beneficiary.
     */
    private boolean sendSatisfactionSurvey(String beneficiarioId, String jornadaId) {
        // Production: Trigger NPS/satisfaction survey via notification service
        log.info("Sending satisfaction survey to beneficiary: {}, journey: {}",
                beneficiarioId, jornadaId);
        return true; // Simulated success
    }

    /**
     * Schedules follow-up care if needed.
     */
    private void scheduleFollowUp(String jornadaId, String beneficiarioId, String desfecho) {
        // Schedule follow-up for certain outcomes
        if ("POOR_OUTCOME".equals(desfecho) || "ADVERSE_EVENT".equals(desfecho)) {
            log.info("Scheduling follow-up for journey: {}, beneficiary: {}", jornadaId, beneficiarioId);
        }
    }

    /**
     * Updates analytics and reporting systems.
     */
    private void updateAnalytics(String jornadaId, Map<String, Object> metricas, String desfecho) {
        // Production: Update data warehouse, BI dashboards
        log.debug("Updated analytics for journey: {}", jornadaId);
    }

    // Metric calculation helpers (simulated)
    private LocalDateTime getJourneyStartDate(String jornadaId) {
        return LocalDateTime.now().minusDays(45);
    }

    private int countCompletedStages(String jornadaId) { return 4; }
    private int getTotalStages(String jornadaId) { return 5; }
    private double calculateCompletionRate(String jornadaId) { return 80.0; }
    private int countCompletedAppointments(String jornadaId) { return 8; }
    private int getTotalScheduledAppointments(String jornadaId) { return 10; }
    private int countReadmissions(String jornadaId) { return 0; }
    private int countComplications(String jornadaId) { return 1; }
    private double calculateTotalCost(String jornadaId) { return 15000.0; }
    private double getEstimatedCost(String jornadaId) { return 18000.0; }
    private double calculateCostVariance(String jornadaId) { return -16.7; } // Under budget
    private int countAppInteractions(String jornadaId) { return 45; }
    private int countSentMessages(String jornadaId) { return 12; }
    private int countReceivedResponses(String jornadaId) { return 9; }
}
