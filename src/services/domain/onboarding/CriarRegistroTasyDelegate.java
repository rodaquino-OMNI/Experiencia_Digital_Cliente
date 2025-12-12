package br.com.austa.experiencia.service.domain.onboarding;

import com.healthplan.services.integration.TasyIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Criar Registro Tasy Delegate
 *
 * Cria registro do beneficiário no sistema ERP Tasy.
 *
 * INPUT:
 * - beneficiaryData (Object): Dados completos do beneficiário
 * - cpf (String): CPF do beneficiário
 * - name (String): Nome completo
 * - birthDate (Date): Data de nascimento
 *
 * OUTPUT:
 * - tasyPatientId (String): ID no sistema Tasy
 * - tasyRegistrationSuccess (Boolean): Registro bem-sucedido?
 * - tasyRegistrationDate (DateTime): Data do registro
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-001 Onboarding)
 */
@Slf4j
@Component("criarRegistroTasyDelegate")
public class CriarRegistroTasyDelegate implements JavaDelegate {

    @Autowired
    private TasyIntegrationService tasyService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String cpf = (String) execution.getVariable("cpf");
        String name = (String) execution.getVariable("name");
        Object beneficiaryData = execution.getVariable("beneficiaryData");

        log.info("Criando registro Tasy - CPF: {}, Nome: {}", cpf, name);

        try {
            // Criar paciente no Tasy
            String tasyPatientId = tasyService.createPatient(beneficiaryData);

            // Armazenar resultado
            execution.setVariable("tasyPatientId", tasyPatientId);
            execution.setVariable("tasyRegistrationSuccess", true);
            execution.setVariable("tasyRegistrationDate", java.time.LocalDateTime.now());

            log.info("Registro Tasy criado com sucesso - ID: {}", tasyPatientId);

        } catch (Exception e) {
            log.error("Erro ao criar registro Tasy - CPF: {}", cpf, e);
            execution.setVariable("tasyRegistrationSuccess", false);
            execution.setVariable("tasyError", e.getMessage());
            throw e;
        }
    }
}
