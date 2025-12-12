package br.com.austa.experiencia.service.domain.onboarding;

import com.healthplan.services.care.CarePlanService;
import com.healthplan.models.CarePlan;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Criar Plano Cuidados Delegate
 *
 * Cria plano de cuidados personalizado baseado no perfil de risco.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - riskScore (Double): Score de risco
 * - riskLevel (String): Nível de risco
 * - healthFlags (List): Flags de saúde
 *
 * OUTPUT:
 * - carePlanId (String): ID do plano de cuidados
 * - carePlanType (String): Tipo do plano
 * - interventions (List): Intervenções planejadas
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-001 Onboarding)
 */
@Slf4j
@Component("criarPlanoCuidadosDelegate")
public class CriarPlanoCuidadosDelegate implements JavaDelegate {

    @Autowired
    private CarePlanService carePlanService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        Double riskScore = (Double) execution.getVariable("riskScore");
        String riskLevel = (String) execution.getVariable("riskLevel");
        @SuppressWarnings("unchecked")
        java.util.List<String> healthFlags = (java.util.List<String>) execution.getVariable("healthFlags");

        log.info("Criando plano de cuidados - Beneficiário: {}, Risco: {}",
            beneficiaryId, riskLevel);

        try {
            // Criar plano personalizado
            CarePlan carePlan = carePlanService.createCarePlan(
                beneficiaryId,
                riskScore,
                riskLevel,
                healthFlags
            );

            // Armazenar plano
            execution.setVariable("carePlanId", carePlan.getId());
            execution.setVariable("carePlanType", carePlan.getType());
            execution.setVariable("interventions", carePlan.getInterventions());
            execution.setVariable("followUpFrequency", carePlan.getFollowUpFrequency());
            execution.setVariable("assignedPrograms", carePlan.getHealthPrograms());

            log.info("Plano de cuidados criado - ID: {}, Tipo: {}, {} intervenções",
                carePlan.getId(), carePlan.getType(), carePlan.getInterventions().size());

        } catch (Exception e) {
            log.error("Erro ao criar plano de cuidados - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("carePlanError", e.getMessage());
            throw e;
        }
    }
}
