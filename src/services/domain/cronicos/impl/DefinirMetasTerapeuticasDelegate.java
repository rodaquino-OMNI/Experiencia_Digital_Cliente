package br.com.austa.experiencia.service.domain.cronicos.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Delegate for defining personalized therapeutic goals for chronic disease management.
 *
 * <p>SUB-008 Gestão Crônicos - Therapeutic Goal Setting</p>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("definirMetasTerapeuticasDelegate")
public class DefinirMetasTerapeuticasDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Defining therapeutic goals - Process: {}", execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String condicaoCronica = (String) execution.getVariable("condicaoCronica");
            String planoId = (String) execution.getVariable("planoId");

            // Get baseline health metrics
            Map<String, Object> baselineMetrics = getBaselineMetrics(beneficiarioId);

            // Define goals based on condition
            List<Map<String, Object>> metas = defineTherapeuticGoals(condicaoCronica, baselineMetrics);

            // Set target dates for each goal
            for (Map<String, Object> meta : metas) {
                meta.put("dataAlvo", LocalDateTime.now().plusMonths(3));
                meta.put("status", "ACTIVE");
            }

            // Persist goals
            String metasId = persistTherapeuticGoals(planoId, metas);

            // Setup progress tracking
            setupProgressTracking(metasId, metas);

            // Set output variables
            execution.setVariable("metasDefinidas", true);
            execution.setVariable("metasId", metasId);
            execution.setVariable("metas", metas);
            execution.setVariable("totalMetas", metas.size());

            log.info("Defined {} therapeutic goals for beneficiary {}", metas.size(), beneficiarioId);

        } catch (Exception e) {
            log.error("Error defining therapeutic goals: {}", e.getMessage(), e);
            execution.setVariable("errorMessage", "Failed to define goals: " + e.getMessage());
            execution.setVariable("metasDefinidas", false);
            throw new RuntimeException("Error in DefinirMetasTerapeuticasDelegate", e);
        }
    }

    private Map<String, Object> getBaselineMetrics(String beneficiarioId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("hbA1c", 8.5);
        metrics.put("pressaoArterial", "145/90");
        metrics.put("peso", 85.0);
        metrics.put("imc", 28.5);
        return metrics;
    }

    private List<Map<String, Object>> defineTherapeuticGoals(String condicao, Map<String, Object> baseline) {
        List<Map<String, Object>> goals = new ArrayList<>();

        if ("DIABETES".equalsIgnoreCase(condicao)) {
            goals.add(createGoal("HbA1c", "Reduzir HbA1c para < 7%", 7.0, "PERCENTAGE"));
            goals.add(createGoal("GLICEMIA", "Glicemia em jejum < 130 mg/dL", 130.0, "MG_DL"));
            goals.add(createGoal("PESO", "Reduzir 5% do peso corporal", 80.75, "KG"));
        } else if ("HIPERTENSAO".equalsIgnoreCase(condicao)) {
            goals.add(createGoal("PA_SISTOLICA", "Pressão sistólica < 130 mmHg", 130.0, "MMHG"));
            goals.add(createGoal("PA_DIASTOLICA", "Pressão diastólica < 80 mmHg", 80.0, "MMHG"));
        }

        goals.add(createGoal("ATIVIDADE_FISICA", "150 min/semana de atividade física", 150.0, "MINUTES"));
        goals.add(createGoal("MEDICACAO", "Aderência medicamentosa > 80%", 80.0, "PERCENTAGE"));

        return goals;
    }

    private Map<String, Object> createGoal(String id, String descricao, Double valorAlvo, String unidade) {
        Map<String, Object> goal = new HashMap<>();
        goal.put("id", id);
        goal.put("descricao", descricao);
        goal.put("valorAlvo", valorAlvo);
        goal.put("unidade", unidade);
        goal.put("prioridade", "HIGH");
        return goal;
    }

    private String persistTherapeuticGoals(String planoId, List<Map<String, Object>> metas) {
        return "METAS-" + System.currentTimeMillis();
    }

    private void setupProgressTracking(String metasId, List<Map<String, Object>> metas) {
        log.debug("Setup progress tracking for goals: {}", metasId);
    }
}
