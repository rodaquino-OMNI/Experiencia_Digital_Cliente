package br.com.austa.experiencia.service.domain.cronicos.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Delegate for adjusting treatment plan based on progress assessment.
 *
 * <p>SUB-008 Gestão Crônicos - Treatment Plan Adjustment</p>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("ajustarPlanoTratamentoDelegate")
public class AjustarPlanoTratamentoDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Adjusting treatment plan - Process: {}", execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String planoId = (String) execution.getVariable("planoId");
            Double scoreProgresso = (Double) execution.getVariable("scoreProgresso");
            Double scoreAderencia = (Double) execution.getVariable("scoreAderencia");

            @SuppressWarnings("unchecked")
            List<String> intervencoes = (List<String>) execution.getVariable("intervencoes");

            @SuppressWarnings("unchecked")
            Map<String, Object> relatorioProgresso = (Map<String, Object>)
                execution.getVariable("relatorioProgresso");

            // Determine adjustment strategy
            String estrategia = determineAdjustmentStrategy(scoreProgresso, scoreAderencia);

            // Create plan adjustments
            List<Map<String, Object>> ajustes = createPlanAdjustments(
                    estrategia, intervencoes, relatorioProgresso);

            // Update goals if needed
            if (shouldUpdateGoals(scoreProgresso, ajustes)) {
                updateTherapeuticGoals(planoId, ajustes);
            }

            // Adjust monitoring frequency
            String novaFrequencia = adjustMonitoringFrequency(scoreProgresso, scoreAderencia);

            // Update care team instructions
            updateCareTeamInstructions(beneficiarioId, ajustes, estrategia);

            // Notify beneficiary of changes
            notifyBeneficiaryOfAdjustments(beneficiarioId, ajustes);

            // Create new plan version
            String novoPlanoId = createPlanVersion(planoId, ajustes);

            // Set output variables
            execution.setVariable("planoAjustado", true);
            execution.setVariable("novoPlanoId", novoPlanoId);
            execution.setVariable("estrategiaAjuste", estrategia);
            execution.setVariable("ajustesRealizados", ajustes);
            execution.setVariable("novaFrequenciaMonitoramento", novaFrequencia);
            execution.setVariable("dataAjuste", LocalDateTime.now());

            log.info("Treatment plan adjusted - Beneficiary: {}, Strategy: {}, Adjustments: {}",
                    beneficiarioId, estrategia, ajustes.size());

        } catch (Exception e) {
            log.error("Error adjusting treatment plan: {}", e.getMessage(), e);
            execution.setVariable("errorMessage", "Failed to adjust plan: " + e.getMessage());
            execution.setVariable("planoAjustado", false);
            throw new RuntimeException("Error in AjustarPlanoTratamentoDelegate", e);
        }
    }

    private String determineAdjustmentStrategy(Double progressScore, Double adherenceScore) {
        if (progressScore >= 80.0 && adherenceScore >= 80.0) {
            return "MAINTAIN_CURRENT";
        } else if (progressScore < 40.0 || adherenceScore < 50.0) {
            return "INTENSIVE_INTERVENTION";
        } else if (adherenceScore < 70.0) {
            return "FOCUS_ON_ADHERENCE";
        } else if (progressScore < 60.0) {
            return "ADJUST_GOALS";
        }
        return "MODERATE_ADJUSTMENT";
    }

    private List<Map<String, Object>> createPlanAdjustments(String estrategia,
                                                            List<String> intervencoes,
                                                            Map<String, Object> progressReport) {
        List<Map<String, Object>> adjustments = new ArrayList<>();

        if ("INTENSIVE_INTERVENTION".equals(estrategia)) {
            adjustments.add(createAdjustment("INCREASE_CARE_COORDINATION",
                    "Aumentar frequência de contato com coordenador de cuidados", "HIGH"));
            adjustments.add(createAdjustment("ADDITIONAL_EDUCATION",
                    "Sessões educacionais sobre manejo da condição", "HIGH"));
        }

        if (intervencoes != null && intervencoes.contains("MEDICATION_ADHERENCE_COUNSELING")) {
            adjustments.add(createAdjustment("MEDICATION_COUNSELING",
                    "Aconselhamento farmacêutico sobre aderência", "MEDIUM"));
        }

        if ("ADJUST_GOALS".equals(estrategia)) {
            adjustments.add(createAdjustment("REVISE_GOALS",
                    "Revisar metas terapêuticas para torná-las mais realistas", "MEDIUM"));
        }

        if (adjustments.isEmpty()) {
            adjustments.add(createAdjustment("CONTINUE_CURRENT",
                    "Continuar plano atual com monitoramento regular", "LOW"));
        }

        return adjustments;
    }

    private Map<String, Object> createAdjustment(String tipo, String descricao, String prioridade) {
        Map<String, Object> adjustment = new HashMap<>();
        adjustment.put("tipo", tipo);
        adjustment.put("descricao", descricao);
        adjustment.put("prioridade", prioridade);
        adjustment.put("timestamp", LocalDateTime.now());
        return adjustment;
    }

    private boolean shouldUpdateGoals(Double progressScore, List<Map<String, Object>> ajustes) {
        if (progressScore < 30.0) return true; // Goals may be too aggressive

        for (Map<String, Object> ajuste : ajustes) {
            if ("REVISE_GOALS".equals(ajuste.get("tipo"))) {
                return true;
            }
        }

        return false;
    }

    private void updateTherapeuticGoals(String planoId, List<Map<String, Object>> ajustes) {
        log.debug("Updating therapeutic goals for plan: {}", planoId);
    }

    private String adjustMonitoringFrequency(Double progressScore, Double adherenceScore) {
        if (progressScore < 40.0 || adherenceScore < 50.0) {
            return "WEEKLY";
        } else if (progressScore < 60.0 || adherenceScore < 70.0) {
            return "BIWEEKLY";
        } else if (progressScore >= 80.0 && adherenceScore >= 80.0) {
            return "MONTHLY";
        }
        return "BIWEEKLY"; // Default
    }

    private void updateCareTeamInstructions(String beneficiarioId,
                                           List<Map<String, Object>> ajustes,
                                           String estrategia) {
        log.info("Updating care team instructions - Beneficiary: {}, Strategy: {}",
                beneficiarioId, estrategia);
    }

    private void notifyBeneficiaryOfAdjustments(String beneficiarioId,
                                                List<Map<String, Object>> ajustes) {
        log.info("Notifying beneficiary {} of {} plan adjustments",
                beneficiarioId, ajustes.size());
    }

    private String createPlanVersion(String planoId, List<Map<String, Object>> ajustes) {
        return planoId + "-V" + System.currentTimeMillis();
    }
}
