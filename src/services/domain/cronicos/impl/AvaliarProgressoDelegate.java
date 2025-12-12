package br.com.austa.experiencia.service.domain.cronicos.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Delegate for assessing patient progress in chronic disease management program.
 *
 * <p>SUB-008 Gestão Crônicos - Progress Assessment</p>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("avaliarProgressoDelegate")
public class AvaliarProgressoDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Assessing program progress - Process: {}", execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String inscricaoId = (String) execution.getVariable("inscricaoId");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> metas = (List<Map<String, Object>>) execution.getVariable("metas");

            @SuppressWarnings("unchecked")
            Map<String, Object> marcadores = (Map<String, Object>) execution.getVariable("marcadoresColetados");

            // Assess goal achievement
            Map<String, Object> goalProgress = assessGoalAchievement(metas, marcadores);

            // Calculate overall progress score
            double progressScore = calculateOverallProgress(goalProgress);

            // Evaluate adherence
            double adherenceScore = evaluateAdherence(beneficiarioId, inscricaoId);

            // Determine intervention needs
            List<String> interventions = determineInterventions(progressScore, adherenceScore, goalProgress);

            // Generate progress report
            Map<String, Object> report = generateProgressReport(
                    beneficiarioId, progressScore, adherenceScore, goalProgress, interventions);

            // Set output variables
            execution.setVariable("progressoAvaliado", true);
            execution.setVariable("scoreProgresso", progressScore);
            execution.setVariable("scoreAderencia", adherenceScore);
            execution.setVariable("intervencoes", interventions);
            execution.setVariable("relatorioProgresso", report);
            execution.setVariable("dataAvaliacao", LocalDateTime.now());

            log.info("Progress assessment complete - Beneficiary: {}, Score: {:.1f}%, Interventions: {}",
                    beneficiarioId, progressScore, interventions.size());

        } catch (Exception e) {
            log.error("Error assessing progress: {}", e.getMessage(), e);
            execution.setVariable("errorMessage", "Failed to assess progress: " + e.getMessage());
            execution.setVariable("progressoAvaliado", false);
            throw new RuntimeException("Error in AvaliarProgressoDelegate", e);
        }
    }

    private Map<String, Object> assessGoalAchievement(List<Map<String, Object>> metas,
                                                      Map<String, Object> marcadores) {
        Map<String, Object> progress = new HashMap<>();
        int metasAlcancadas = 0;

        for (Map<String, Object> meta : metas) {
            String metaId = (String) meta.get("id");
            Double valorAlvo = (Double) meta.get("valorAlvo");

            // Check if marker exists and goal is achieved
            Object valorAtual = marcadores.get(metaId);
            if (valorAtual != null) {
                boolean achieved = isGoalAchieved(valorAtual, valorAlvo, metaId);
                if (achieved) {
                    metasAlcancadas++;
                }

                Map<String, Object> metaProgress = new HashMap<>();
                metaProgress.put("valorAtual", valorAtual);
                metaProgress.put("valorAlvo", valorAlvo);
                metaProgress.put("alcancada", achieved);
                metaProgress.put("percentualAlcancado", calculateGoalPercentage(valorAtual, valorAlvo));

                progress.put(metaId, metaProgress);
            }
        }

        progress.put("totalMetas", metas.size());
        progress.put("metasAlcancadas", metasAlcancadas);
        progress.put("taxaAlcance", (metasAlcancadas * 100.0) / metas.size());

        return progress;
    }

    private boolean isGoalAchieved(Object atual, Double alvo, String metaId) {
        // Different logic based on goal type
        if (atual instanceof Double) {
            if (metaId.contains("HbA1c") || metaId.contains("PA") || metaId.contains("PESO")) {
                return (Double) atual <= alvo; // Lower is better
            } else {
                return (Double) atual >= alvo; // Higher is better
            }
        }
        return false;
    }

    private double calculateGoalPercentage(Object atual, Double alvo) {
        if (atual instanceof Double) {
            return Math.min(100.0, ((Double) atual / alvo) * 100.0);
        }
        return 0.0;
    }

    private double calculateOverallProgress(Map<String, Object> goalProgress) {
        return (Double) goalProgress.getOrDefault("taxaAlcance", 0.0);
    }

    private double evaluateAdherence(String beneficiarioId, String inscricaoId) {
        // Production: Calculate from medication logs, appointment attendance, etc.
        return 78.5; // Simulated adherence score
    }

    private List<String> determineInterventions(double progressScore, double adherenceScore,
                                                Map<String, Object> goalProgress) {
        List<String> interventions = new ArrayList<>();

        if (progressScore < 50.0) {
            interventions.add("SCHEDULE_CARE_COORDINATOR_CALL");
        }

        if (adherenceScore < 70.0) {
            interventions.add("MEDICATION_ADHERENCE_COUNSELING");
        }

        if (progressScore < 30.0 && adherenceScore < 50.0) {
            interventions.add("INTENSIVE_CASE_MANAGEMENT");
        }

        return interventions;
    }

    private Map<String, Object> generateProgressReport(String beneficiarioId, double progressScore,
                                                       double adherenceScore, Map<String, Object> goalProgress,
                                                       List<String> interventions) {
        Map<String, Object> report = new HashMap<>();
        report.put("beneficiarioId", beneficiarioId);
        report.put("dataRelatorio", LocalDateTime.now());
        report.put("scoreProgresso", progressScore);
        report.put("scoreAderencia", adherenceScore);
        report.put("progressoMetas", goalProgress);
        report.put("intervencoes", interventions);
        report.put("statusGeral", determineOverallStatus(progressScore, adherenceScore));
        return report;
    }

    private String determineOverallStatus(double progressScore, double adherenceScore) {
        double avgScore = (progressScore + adherenceScore) / 2.0;

        if (avgScore >= 80.0) return "EXCELLENT";
        if (avgScore >= 60.0) return "GOOD";
        if (avgScore >= 40.0) return "NEEDS_ATTENTION";
        return "AT_RISK";
    }
}
