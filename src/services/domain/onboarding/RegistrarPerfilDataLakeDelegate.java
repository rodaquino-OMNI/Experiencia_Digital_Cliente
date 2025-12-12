package br.com.austa.experiencia.service.domain.onboarding;

import com.healthplan.services.datalake.DataLakeService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Registrar Perfil Data Lake Delegate
 *
 * Registra perfil completo do beneficiário no Data Lake para analytics.
 *
 * INPUT:
 * - beneficiaryProfile (Object): Perfil completo
 * - riskScore (Double): Score de risco
 * - carePlanId (String): ID do plano de cuidados
 *
 * OUTPUT:
 * - dataLakeRecordId (String): ID do registro no Data Lake
 * - dataLakeTimestamp (DateTime): Timestamp do registro
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-001 Onboarding)
 */
@Slf4j
@Component("registrarPerfilDataLakeDelegate")
public class RegistrarPerfilDataLakeDelegate implements JavaDelegate {

    @Autowired
    private DataLakeService dataLakeService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        Object profile = execution.getVariable("beneficiaryProfile");

        log.info("Registrando perfil no Data Lake - Beneficiário: {}", beneficiaryId);

        try {
            // Consolidar dados para Data Lake
            Map<String, Object> dataLakeRecord = buildDataLakeRecord(execution);

            // Registrar no Data Lake
            String recordId = dataLakeService.registerBeneficiary(dataLakeRecord);

            // Armazenar confirmação
            execution.setVariable("dataLakeRecordId", recordId);
            execution.setVariable("dataLakeTimestamp", java.time.LocalDateTime.now());
            execution.setVariable("dataLakeRegistered", true);

            log.info("Perfil registrado no Data Lake - Record ID: {}", recordId);

        } catch (Exception e) {
            log.error("Erro ao registrar perfil no Data Lake - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("dataLakeError", e.getMessage());
            execution.setVariable("dataLakeRegistered", false);
            // Não propaga - Data Lake não deve bloquear onboarding
        }
    }

    private Map<String, Object> buildDataLakeRecord(DelegateExecution execution) {
        Map<String, Object> record = new java.util.HashMap<>();

        record.put("beneficiaryId", execution.getVariable("beneficiaryId"));
        record.put("onboardingDate", java.time.LocalDateTime.now());
        record.put("riskScore", execution.getVariable("riskScore"));
        record.put("riskLevel", execution.getVariable("riskLevel"));
        record.put("carePlanId", execution.getVariable("carePlanId"));
        record.put("carePlanType", execution.getVariable("carePlanType"));
        record.put("tasyPatientId", execution.getVariable("tasyPatientId"));
        record.put("extractedData", execution.getVariable("extractedData"));
        record.put("screeningScore", execution.getVariable("screeningScore"));
        record.put("healthFlags", execution.getVariable("healthFlags"));

        return record;
    }
}
