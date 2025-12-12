package br.com.austa.experiencia.service.domain.cronicos.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Delegate responsible for enrolling beneficiary in chronic disease management program.
 *
 * <p>SUB-008 Gestão Crônicos - Program Enrollment</p>
 *
 * <p>Functionality:</p>
 * <ul>
 *   <li>Enrolls beneficiary in appropriate chronic disease program</li>
 *   <li>Validates eligibility criteria</li>
 *   <li>Creates personalized care plan</li>
 *   <li>Assigns care team and resources</li>
 * </ul>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("inscreverProgramaCronicoDelegate")
public class InscreverProgramaCronicoDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting chronic program enrollment - Process: {}", execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String programaId = (String) execution.getVariable("programaId");
            String condicaoCronica = (String) execution.getVariable("condicaoCronica");

            // Validate eligibility
            boolean elegivel = validateEligibility(beneficiarioId, programaId);
            if (!elegivel) {
                execution.setVariable("inscricaoRealizada", false);
                execution.setVariable("motivoRejeicao", "Beneficiário não atende critérios de elegibilidade");
                return;
            }

            // Create enrollment record
            String inscricaoId = UUID.randomUUID().toString();
            Map<String, Object> enrollmentData = createEnrollmentRecord(
                    inscricaoId, beneficiarioId, programaId, condicaoCronica);

            // Assign care team
            Map<String, String> careTeam = assignCareTeam(programaId, condicaoCronica);

            // Create initial care plan
            String planoId = createInitialCarePlan(beneficiarioId, programaId, condicaoCronica);

            // Setup monitoring schedule
            setupMonitoringSchedule(inscricaoId, programaId);

            // Set output variables
            execution.setVariable("inscricaoRealizada", true);
            execution.setVariable("inscricaoId", inscricaoId);
            execution.setVariable("dataInscricao", LocalDateTime.now());
            execution.setVariable("careTeam", careTeam);
            execution.setVariable("planoId", planoId);

            log.info("Successfully enrolled beneficiary {} in program {}", beneficiarioId, programaId);

        } catch (Exception e) {
            log.error("Error enrolling in chronic program - Process: {}, Error: {}",
                    execution.getProcessInstanceId(), e.getMessage(), e);
            execution.setVariable("errorMessage", "Failed to enroll: " + e.getMessage());
            execution.setVariable("inscricaoRealizada", false);
            throw new RuntimeException("Error in InscreverProgramaCronicoDelegate", e);
        }
    }

    private boolean validateEligibility(String beneficiarioId, String programaId) {
        // Production: Check diagnosis, lab results, medication history
        return true; // Simulated
    }

    private Map<String, Object> createEnrollmentRecord(String inscricaoId, String beneficiarioId,
                                                       String programaId, String condicao) {
        Map<String, Object> record = new HashMap<>();
        record.put("inscricaoId", inscricaoId);
        record.put("beneficiarioId", beneficiarioId);
        record.put("programaId", programaId);
        record.put("condicaoCronica", condicao);
        record.put("dataInscricao", LocalDateTime.now());
        record.put("status", "ACTIVE");
        return record;
    }

    private Map<String, String> assignCareTeam(String programaId, String condicao) {
        Map<String, String> team = new HashMap<>();
        team.put("medico", "DR-001");
        team.put("enfermeiro", "ENF-001");
        team.put("nutricionista", "NUT-001");
        return team;
    }

    private String createInitialCarePlan(String beneficiarioId, String programaId, String condicao) {
        return "PLAN-" + System.currentTimeMillis();
    }

    private void setupMonitoringSchedule(String inscricaoId, String programaId) {
        log.debug("Setting up monitoring schedule for enrollment: {}", inscricaoId);
    }
}
