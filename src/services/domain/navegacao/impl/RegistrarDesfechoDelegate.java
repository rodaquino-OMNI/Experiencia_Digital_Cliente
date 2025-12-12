package br.com.austa.experiencia.service.domain.navegacao.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate responsible for registering clinical outcomes and journey results.
 *
 * <p>SUB-007 Navegação - Outcome Registration</p>
 *
 * <p>Functionality:</p>
 * <ul>
 *   <li>Records clinical outcomes and treatment results</li>
 *   <li>Captures patient-reported outcome measures (PROMs)</li>
 *   <li>Documents adverse events and complications</li>
 *   <li>Enables outcome analysis and quality improvement</li>
 * </ul>
 *
 * <p>Input Variables:</p>
 * <ul>
 *   <li><b>jornadaId</b> (String): Care journey ID</li>
 *   <li><b>beneficiarioId</b> (String): Beneficiary ID</li>
 *   <li><b>tipoDesfecho</b> (String): Outcome type</li>
 *   <li><b>resultadoClinico</b> (Map): Clinical result data</li>
 *   <li><b>promScore</b> (Double): Patient-reported outcome score</li>
 * </ul>
 *
 * <p>Output Variables:</p>
 * <ul>
 *   <li><b>desfechoRegistrado</b> (Boolean): Outcome registered flag</li>
 *   <li><b>desfechoId</b> (String): Outcome record ID</li>
 *   <li><b>classificacaoDesfecho</b> (String): Outcome classification</li>
 *   <li><b>timestampDesfecho</b> (LocalDateTime): Registration timestamp</li>
 *   <li><b>followUpNecessario</b> (Boolean): Follow-up needed flag</li>
 * </ul>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("registrarDesfechoDelegate")
public class RegistrarDesfechoDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting clinical outcome registration - Process: {}, Execution: {}",
                execution.getProcessInstanceId(), execution.getId());

        try {
            // Extract input parameters
            String jornadaId = (String) execution.getVariable("jornadaId");
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String tipoDesfecho = (String) execution.getVariable("tipoDesfecho");

            @SuppressWarnings("unchecked")
            Map<String, Object> resultadoClinico = (Map<String, Object>)
                execution.getVariable("resultadoClinico");
            if (resultadoClinico == null) {
                resultadoClinico = new HashMap<>();
            }

            Double promScore = (Double) execution.getVariable("promScore");

            log.debug("Registering outcome: journey={}, type={}, PROM={}",
                    jornadaId, tipoDesfecho, promScore);

            // Create outcome record
            Map<String, Object> outcomeRecord = createOutcomeRecord(
                    jornadaId, beneficiarioId, tipoDesfecho, resultadoClinico, promScore);

            // Classify outcome
            String classificacao = classifyOutcome(tipoDesfecho, resultadoClinico, promScore);

            // Persist outcome data
            String desfechoId = persistOutcomeRecord(outcomeRecord);

            // Determine if follow-up needed
            boolean followUpNecessario = isFollowUpNeeded(classificacao, resultadoClinico);

            // Update journey metrics
            updateJourneyMetrics(jornadaId, classificacao);

            // Notify care team if needed
            if (followUpNecessario || "ADVERSE_EVENT".equals(classificacao)) {
                notifyCareTeam(jornadaId, beneficiarioId, classificacao);
            }

            // Set output variables
            LocalDateTime timestamp = LocalDateTime.now();
            execution.setVariable("desfechoRegistrado", true);
            execution.setVariable("desfechoId", desfechoId);
            execution.setVariable("classificacaoDesfecho", classificacao);
            execution.setVariable("timestampDesfecho", timestamp);
            execution.setVariable("followUpNecessario", followUpNecessario);

            log.info("Successfully registered outcome - Journey: {}, ID: {}, Classification: {}, Follow-up: {}",
                    jornadaId, desfechoId, classificacao, followUpNecessario);

        } catch (Exception e) {
            log.error("Error registering clinical outcome - Process: {}, Error: {}",
                    execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("errorMessage", "Failed to register outcome: " + e.getMessage());
            execution.setVariable("hasError", true);
            execution.setVariable("desfechoRegistrado", false);

            throw new RuntimeException("Error in RegistrarDesfechoDelegate", e);
        }
    }

    /**
     * Creates comprehensive outcome record.
     */
    private Map<String, Object> createOutcomeRecord(String jornadaId, String beneficiarioId,
                                                    String tipo, Map<String, Object> resultado,
                                                    Double promScore) {
        Map<String, Object> record = new HashMap<>();

        record.put("jornadaId", jornadaId);
        record.put("beneficiarioId", beneficiarioId);
        record.put("tipoDesfecho", tipo);
        record.put("timestamp", LocalDateTime.now());
        record.put("resultadoClinico", resultado);
        record.put("promScore", promScore);

        // Clinical metrics
        record.put("sintomasResolvidos", resultado.get("sintomasResolvidos"));
        record.put("funcionalidade", resultado.get("funcionalidade"));
        record.put("qualidadeVida", resultado.get("qualidadeVida"));

        // Complications
        record.put("complicacoes", resultado.get("complicacoes"));
        record.put("eventoAdverso", resultado.get("eventoAdverso"));
        record.put("readmissao", resultado.get("readmissao"));

        return record;
    }

    /**
     * Classifies outcome based on type and results.
     */
    private String classifyOutcome(String tipo, Map<String, Object> resultado, Double promScore) {
        // Check for adverse events first
        if (Boolean.TRUE.equals(resultado.get("eventoAdverso"))) {
            return "ADVERSE_EVENT";
        }

        if (Boolean.TRUE.equals(resultado.get("readmissao"))) {
            return "READMISSION";
        }

        // Classify based on PROM score
        if (promScore != null) {
            if (promScore >= 80.0) {
                return "EXCELLENT_OUTCOME";
            } else if (promScore >= 60.0) {
                return "GOOD_OUTCOME";
            } else if (promScore >= 40.0) {
                return "FAIR_OUTCOME";
            } else {
                return "POOR_OUTCOME";
            }
        }

        // Default classification
        return "STANDARD_OUTCOME";
    }

    /**
     * Persists outcome record to database.
     */
    private String persistOutcomeRecord(Map<String, Object> record) {
        // Production: Save to clinical_outcomes table
        String outcomeId = "OUTCOME-" + System.currentTimeMillis();
        log.debug("Persisting outcome record: {}", outcomeId);
        return outcomeId;
    }

    /**
     * Determines if follow-up is needed based on outcome.
     */
    private boolean isFollowUpNeeded(String classificacao, Map<String, Object> resultado) {
        // Follow-up needed for:
        // 1. Adverse events or readmissions
        // 2. Poor outcomes
        // 3. Ongoing complications

        if ("ADVERSE_EVENT".equals(classificacao) ||
            "READMISSION".equals(classificacao) ||
            "POOR_OUTCOME".equals(classificacao)) {
            return true;
        }

        if (Boolean.TRUE.equals(resultado.get("complicacoes"))) {
            return true;
        }

        return false;
    }

    /**
     * Updates journey-level metrics with outcome data.
     */
    private void updateJourneyMetrics(String jornadaId, String classificacao) {
        // Production: Update journey_metrics table
        log.debug("Updated journey metrics for: {}, classification: {}", jornadaId, classificacao);
    }

    /**
     * Notifies care team of significant outcomes.
     */
    private void notifyCareTeam(String jornadaId, String beneficiarioId, String classificacao) {
        // Production: Send alerts to care coordinators and clinical team
        log.info("Notifying care team - Journey: {}, Beneficiary: {}, Classification: {}",
                jornadaId, beneficiarioId, classificacao);
    }
}
