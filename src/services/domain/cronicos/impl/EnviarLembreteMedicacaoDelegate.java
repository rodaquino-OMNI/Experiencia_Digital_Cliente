package br.com.austa.experiencia.service.domain.cronicos.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Delegate for sending medication reminders to chronic disease patients.
 *
 * <p>SUB-008 Gestão Crônicos - Medication Reminders</p>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("enviarLembreteMedicacaoDelegate")
public class EnviarLembreteMedicacaoDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Sending medication reminders - Process: {}", execution.getProcessInstanceId());

        try {
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");

            // Get medication schedule
            List<Map<String, Object>> medicamentos = getMedicationSchedule(beneficiarioId);

            // Get current time and determine which reminders to send
            LocalTime now = LocalTime.now();
            List<Map<String, Object>> dueMedications = filterDueMedications(medicamentos, now);

            int remindersSent = 0;
            for (Map<String, Object> med : dueMedications) {
                boolean sent = sendReminder(beneficiarioId, med);
                if (sent) {
                    remindersSent++;
                    logMedicationReminder(beneficiarioId, med);
                }
            }

            // Set output variables
            execution.setVariable("lembretesEnviados", remindersSent);
            execution.setVariable("timestampLembrete", LocalDateTime.now());
            execution.setVariable("medicamentosDevidos", dueMedications.size());

            log.info("Sent {} medication reminders to beneficiary {}", remindersSent, beneficiarioId);

        } catch (Exception e) {
            log.error("Error sending medication reminders: {}", e.getMessage(), e);
            execution.setVariable("errorMessage", "Failed to send reminders: " + e.getMessage());
            execution.setVariable("lembretesEnviados", 0);
            throw new RuntimeException("Error in EnviarLembreteMedicacaoDelegate", e);
        }
    }

    private List<Map<String, Object>> getMedicationSchedule(String beneficiarioId) {
        // Production: Query medication_schedule table
        List<Map<String, Object>> schedule = new ArrayList<>();

        schedule.add(createMedication("Metformina", "08:00", "500mg", "Tomar com café da manhã"));
        schedule.add(createMedication("Metformina", "20:00", "500mg", "Tomar com jantar"));
        schedule.add(createMedication("Losartana", "08:00", "50mg", "Tomar pela manhã"));

        return schedule;
    }

    private Map<String, Object> createMedication(String nome, String horario, String dose, String instrucoes) {
        Map<String, Object> med = new HashMap<>();
        med.put("nome", nome);
        med.put("horario", LocalTime.parse(horario));
        med.put("dose", dose);
        med.put("instrucoes", instrucoes);
        return med;
    }

    private List<Map<String, Object>> filterDueMedications(List<Map<String, Object>> meds, LocalTime now) {
        List<Map<String, Object>> due = new ArrayList<>();

        for (Map<String, Object> med : meds) {
            LocalTime scheduledTime = (LocalTime) med.get("horario");

            // Send reminder 30 minutes before scheduled time
            if (now.isAfter(scheduledTime.minusMinutes(30)) && now.isBefore(scheduledTime)) {
                due.add(med);
            }
        }

        return due;
    }

    private boolean sendReminder(String beneficiarioId, Map<String, Object> medication) {
        // Production: Send push notification, SMS, or in-app reminder
        log.debug("Sending reminder to {}: {} - {}",
                beneficiarioId, medication.get("nome"), medication.get("dose"));
        return true; // Simulated success
    }

    private void logMedicationReminder(String beneficiarioId, Map<String, Object> medication) {
        // Production: Log to medication_adherence_log table
        log.debug("Logged reminder for beneficiary: {}, medication: {}",
                beneficiarioId, medication.get("nome"));
    }
}
