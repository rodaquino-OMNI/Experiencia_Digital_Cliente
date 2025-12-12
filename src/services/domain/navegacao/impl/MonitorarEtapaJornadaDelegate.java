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
 * Delegate responsible for monitoring care journey stage progress.
 *
 * <p>SUB-007 Navegação - Journey Stage Monitoring</p>
 *
 * <p>Functionality:</p>
 * <ul>
 *   <li>Tracks beneficiary progress through care journey stages</li>
 *   <li>Monitors stage completion and adherence</li>
 *   <li>Identifies delays and blockers in the journey</li>
 *   <li>Triggers interventions for at-risk patients</li>
 * </ul>
 *
 * <p>Input Variables:</p>
 * <ul>
 *   <li><b>jornadaId</b> (String): Care journey ID</li>
 *   <li><b>etapaAtual</b> (String): Current journey stage</li>
 *   <li><b>beneficiarioId</b> (String): Beneficiary ID</li>
 *   <li><b>prazoEtapa</b> (Integer): Expected stage duration in days</li>
 * </ul>
 *
 * <p>Output Variables:</p>
 * <ul>
 *   <li><b>statusEtapa</b> (String): Stage status (ON_TIME/DELAYED/COMPLETED)</li>
 *   <li><b>diasPassados</b> (Integer): Days elapsed in current stage</li>
 *   <li><b>aderencia</b> (Double): Adherence percentage</li>
 *   <li><b>intervencaoNecessaria</b> (Boolean): Intervention required flag</li>
 *   <li><b>proximaEtapa</b> (String): Next stage recommendation</li>
 * </ul>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("monitorarEtapaJornadaDelegate")
public class MonitorarEtapaJornadaDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting journey stage monitoring - Process: {}, Execution: {}",
                execution.getProcessInstanceId(), execution.getId());

        try {
            // Extract input parameters
            String jornadaId = (String) execution.getVariable("jornadaId");
            String etapaAtual = (String) execution.getVariable("etapaAtual");
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            Integer prazoEtapa = (Integer) execution.getVariable("prazoEtapa");

            if (prazoEtapa == null) {
                prazoEtapa = 30; // Default 30 days
            }

            log.debug("Monitoring journey stage: {}, stage: {}, beneficiary: {}",
                    jornadaId, etapaAtual, beneficiarioId);

            // Get stage start date
            LocalDateTime stageStartDate = getStageStartDate(jornadaId, etapaAtual);

            // Calculate elapsed time
            long diasPassados = Duration.between(stageStartDate, LocalDateTime.now()).toDays();

            // Determine stage status
            String statusEtapa = determineStageStatus(diasPassados, prazoEtapa);

            // Calculate adherence
            double aderencia = calculateAdherence(jornadaId, beneficiarioId);

            // Check if intervention needed
            boolean intervencaoNecessaria = isInterventionNeeded(
                    statusEtapa, aderencia, diasPassados, prazoEtapa);

            // Determine next stage
            String proximaEtapa = determineNextStage(etapaAtual, statusEtapa, aderencia);

            // Log monitoring data
            logMonitoringData(jornadaId, etapaAtual, statusEtapa, diasPassados, aderencia);

            // Set output variables
            execution.setVariable("statusEtapa", statusEtapa);
            execution.setVariable("diasPassados", (int) diasPassados);
            execution.setVariable("aderencia", aderencia);
            execution.setVariable("intervencaoNecessaria", intervencaoNecessaria);
            execution.setVariable("proximaEtapa", proximaEtapa);

            log.info("Journey stage monitoring complete - Journey: {}, Status: {}, Adherence: {:.1f}%, Intervention: {}",
                    jornadaId, statusEtapa, aderencia, intervencaoNecessaria);

        } catch (Exception e) {
            log.error("Error monitoring journey stage - Process: {}, Error: {}",
                    execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("errorMessage", "Failed to monitor journey stage: " + e.getMessage());
            execution.setVariable("hasError", true);
            execution.setVariable("intervencaoNecessaria", true); // Escalate on error

            throw new RuntimeException("Error in MonitorarEtapaJornadaDelegate", e);
        }
    }

    /**
     * Retrieves the start date of the current stage.
     */
    private LocalDateTime getStageStartDate(String jornadaId, String etapa) {
        // Production: Query journey_stages table for stage_start_date
        return LocalDateTime.now().minusDays(15); // Simulated: 15 days ago
    }

    /**
     * Determines stage status based on elapsed time vs expected duration.
     */
    private String determineStageStatus(long diasPassados, int prazoEtapa) {
        if (diasPassados <= prazoEtapa) {
            return "ON_TIME";
        } else if (diasPassados <= prazoEtapa * 1.2) { // 20% buffer
            return "AT_RISK";
        } else {
            return "DELAYED";
        }
    }

    /**
     * Calculates journey adherence percentage.
     */
    private double calculateAdherence(String jornadaId, String beneficiarioId) {
        // Production: Calculate based on completed milestones, appointments kept, etc.
        // Simulated adherence calculation
        Map<String, Integer> metrics = getAdherenceMetrics(jornadaId);

        int completedMilestones = metrics.get("completedMilestones");
        int totalMilestones = metrics.get("totalMilestones");
        int keptAppointments = metrics.get("keptAppointments");
        int totalAppointments = metrics.get("totalAppointments");

        if (totalMilestones == 0 && totalAppointments == 0) {
            return 100.0; // No milestones yet
        }

        double milestoneAdherence = totalMilestones > 0 ?
                (completedMilestones * 100.0 / totalMilestones) : 100.0;
        double appointmentAdherence = totalAppointments > 0 ?
                (keptAppointments * 100.0 / totalAppointments) : 100.0;

        // Weighted average: 60% milestones, 40% appointments
        return (milestoneAdherence * 0.6) + (appointmentAdherence * 0.4);
    }

    /**
     * Gets adherence metrics for the journey.
     */
    private Map<String, Integer> getAdherenceMetrics(String jornadaId) {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("completedMilestones", 7);
        metrics.put("totalMilestones", 10);
        metrics.put("keptAppointments", 3);
        metrics.put("totalAppointments", 4);
        return metrics;
    }

    /**
     * Determines if intervention is needed based on stage status and adherence.
     */
    private boolean isInterventionNeeded(String status, double aderencia,
                                        long diasPassados, int prazoEtapa) {
        // Intervention needed if:
        // 1. Stage is delayed
        // 2. Adherence below 70%
        // 3. 80% of time elapsed with <50% adherence

        if ("DELAYED".equals(status)) {
            return true;
        }

        if (aderencia < 70.0) {
            return true;
        }

        if (diasPassados >= prazoEtapa * 0.8 && aderencia < 50.0) {
            return true;
        }

        return false;
    }

    /**
     * Determines the next recommended stage based on current progress.
     */
    private String determineNextStage(String currentStage, String status, double aderencia) {
        // Production: Use journey definition to determine next stage
        Map<String, String> stageFlow = Map.of(
            "PRE_PROCEDURE", "PROCEDURE",
            "PROCEDURE", "RECOVERY",
            "RECOVERY", "FOLLOW_UP",
            "FOLLOW_UP", "COMPLETION"
        );

        if ("ON_TIME".equals(status) && aderencia >= 80.0) {
            return stageFlow.getOrDefault(currentStage, "COMPLETION");
        } else if ("DELAYED".equals(status) || aderencia < 50.0) {
            return "INTERVENTION_REQUIRED";
        } else {
            return currentStage; // Stay in current stage
        }
    }

    /**
     * Logs monitoring data for analytics and reporting.
     */
    private void logMonitoringData(String jornadaId, String etapa, String status,
                                   long diasPassados, double aderencia) {
        // Production: Log to journey_monitoring table for analytics
        log.debug("Journey monitoring data - Journey: {}, Stage: {}, Status: {}, Days: {}, Adherence: {:.1f}%",
                jornadaId, etapa, status, diasPassados, aderencia);
    }
}
