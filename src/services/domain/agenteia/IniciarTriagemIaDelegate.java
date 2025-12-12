package br.com.austa.experiencia.service.domain.agenteia;

import com.healthplan.services.ai.AiTriageService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Iniciar Triagem IA Delegate
 *
 * Inicia conversa de triagem inteligente com IA para avaliação inicial de sintomas.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - initialSymptoms (String): Sintomas iniciais relatados
 *
 * OUTPUT:
 * - triageSessionId (String): ID da sessão de triagem
 * - aiResponse (String): Primeira resposta da IA
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-005 Agentes IA)
 */
@Slf4j
@Component("iniciarTriagemIaDelegate")
public class IniciarTriagemIaDelegate implements JavaDelegate {

    @Autowired
    private AiTriageService aiTriageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        String initialSymptoms = (String) execution.getVariable("initialSymptoms");

        log.info("Iniciando triagem IA - Beneficiário: {}", beneficiaryId);

        try {
            // Iniciar sessão de triagem
            String sessionId = aiTriageService.startTriageConversation(
                beneficiaryId,
                initialSymptoms
            );

            // Armazenar ID da sessão
            execution.setVariable("triageSessionId", sessionId);
            execution.setVariable("triageStatus", "ACTIVE");

            log.info("Triagem IA iniciada com sucesso - Sessão: {}", sessionId);

        } catch (Exception e) {
            log.error("Erro ao iniciar triagem IA para beneficiário: {}", beneficiaryId, e);
            execution.setVariable("triageError", e.getMessage());
            throw e;
        }
    }
}
