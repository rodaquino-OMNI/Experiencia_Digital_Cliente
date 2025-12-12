package br.com.austa.experiencia.service.domain.navegacao;

import com.healthplan.services.journey.CareJourneyService;
import com.healthplan.models.CareJourney;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Criar Jornada Cuidado Delegate
 *
 * Cria jornada de cuidado personalizada para o beneficiário.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - carePlanId (String): ID do plano de cuidados
 * - navigatorId (String): ID do navegador
 * - healthConditions (List): Condições de saúde
 *
 * OUTPUT:
 * - journeyId (String): ID da jornada
 * - journeySteps (List): Etapas da jornada
 * - estimatedDuration (Integer): Duração estimada (dias)
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-007 Navegação)
 */
@Slf4j
@Component("criarJornadaCuidadoDelegate")
public class CriarJornadaCuidadoDelegate implements JavaDelegate {

    @Autowired
    private CareJourneyService journeyService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        String carePlanId = (String) execution.getVariable("carePlanId");
        String navigatorId = (String) execution.getVariable("navigatorId");
        @SuppressWarnings("unchecked")
        java.util.List<String> conditions = (java.util.List<String>) execution.getVariable("healthConditions");

        log.info("Criando jornada de cuidado - Beneficiário: {}, Plano: {}",
            beneficiaryId, carePlanId);

        try {
            // Criar jornada personalizada
            CareJourney journey = journeyService.createJourney(
                beneficiaryId,
                carePlanId,
                navigatorId,
                conditions
            );

            // Armazenar jornada
            execution.setVariable("journeyId", journey.getId());
            execution.setVariable("journeySteps", journey.getSteps());
            execution.setVariable("estimatedDuration", journey.getEstimatedDurationDays());
            execution.setVariable("journeyMilestones", journey.getMilestones());
            execution.setVariable("journeyStatus", "ACTIVE");
            execution.setVariable("journeyCreatedAt", journey.getCreatedAt());

            log.info("Jornada criada - ID: {}, {} etapas, {} dias estimados",
                journey.getId(),
                journey.getSteps().size(),
                journey.getEstimatedDurationDays());

        } catch (Exception e) {
            log.error("Erro ao criar jornada de cuidado - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("journeyCreationError", e.getMessage());
            throw e;
        }
    }
}
