package br.com.austa.experiencia.service.domain.navegacao;

import com.healthplan.services.navigator.CareNavigatorService;
import com.healthplan.models.NavigatorAssignment;
import com.healthplan.models.BeneficiaryProfile;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Atribuir Navegador Delegate
 *
 * Atribui navegador de cuidados especializado ao beneficiário.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - beneficiaryProfile (Object): Perfil do beneficiário
 * - riskLevel (String): Nível de risco
 *
 * OUTPUT:
 * - navigatorId (String): ID do navegador atribuído
 * - navigatorName (String): Nome do navegador
 * - navigatorContact (Object): Contatos do navegador
 * - assignmentScore (Double): Score de compatibilidade
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-007 Navegação)
 */
@Slf4j
@Component("atribuirNavegadorDelegate")
public class AtribuirNavegadorDelegate implements JavaDelegate {

    @Autowired
    private CareNavigatorService navigatorService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        BeneficiaryProfile profile = (BeneficiaryProfile) execution.getVariable("beneficiaryProfile");

        log.info("Atribuindo navegador - Beneficiário: {}", beneficiaryId);

        try {
            // Atribuir navegador otimizado
            NavigatorAssignment assignment = navigatorService.assignNavigator(
                beneficiaryId,
                profile
            );

            // Armazenar atribuição
            execution.setVariable("navigatorId", assignment.getNavigatorId());
            execution.setVariable("navigatorName", assignment.getNavigatorName());
            execution.setVariable("navigatorPhone", assignment.getNavigatorPhone());
            execution.setVariable("navigatorEmail", assignment.getNavigatorEmail());
            execution.setVariable("navigatorSpecializations", assignment.getSpecializations());
            execution.setVariable("assignmentScore", assignment.getMatchScore());
            execution.setVariable("assignmentReasons", assignment.getMatchReasons());
            execution.setVariable("navigatorAssignedAt", assignment.getAssignedAt());

            log.info("Navegador atribuído - ID: {}, Nome: {}, Score: {:.2f}",
                assignment.getNavigatorId(),
                assignment.getNavigatorName(),
                assignment.getMatchScore());

        } catch (Exception e) {
            log.error("Erro ao atribuir navegador - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("navigatorAssignmentError", e.getMessage());
            throw e;
        }
    }
}
