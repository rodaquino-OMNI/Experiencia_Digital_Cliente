package br.com.austa.experiencia.service.domain.agenteia;

import com.healthplan.services.ai.AiTriageService;
import com.healthplan.models.TriageResponse;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processar Resposta Triagem Delegate
 *
 * Processa resposta do beneficiário na conversa de triagem IA.
 *
 * INPUT:
 * - triageSessionId (String): ID da sessão
 * - userResponse (String): Resposta do usuário
 *
 * OUTPUT:
 * - aiFollowUp (String): Próxima pergunta da IA
 * - triageComplete (Boolean): Triagem concluída?
 * - severityLevel (String): Nível de gravidade
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-005 Agentes IA)
 */
@Slf4j
@Component("processarRespostaTriagemDelegate")
public class ProcessarRespostaTriagemDelegate implements JavaDelegate {

    @Autowired
    private AiTriageService aiTriageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String sessionId = (String) execution.getVariable("triageSessionId");
        String userResponse = (String) execution.getVariable("userResponse");

        log.info("Processando resposta de triagem - Sessão: {}", sessionId);

        try {
            // Processar resposta
            TriageResponse response = aiTriageService.processResponse(
                sessionId,
                userResponse
            );

            // Armazenar resultados
            execution.setVariable("aiFollowUp", response.getNextQuestion());
            execution.setVariable("triageComplete", response.isComplete());
            execution.setVariable("severityLevel", response.getSeverityLevel());
            execution.setVariable("nextAction", response.getActionType());

            log.info("Resposta processada - Ação: {}, Completo: {}",
                response.getActionType(), response.isComplete());

        } catch (Exception e) {
            log.error("Erro ao processar resposta de triagem - Sessão: {}", sessionId, e);
            execution.setVariable("triageError", e.getMessage());
            throw e;
        }
    }
}
