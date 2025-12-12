package br.com.austa.experiencia.service.domain.agenteia;

import com.healthplan.services.ai.AiTriageService;
import com.healthplan.models.TransferContext;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Transferir com Contexto Delegate
 *
 * Transfere atendimento para humano com contexto completo da conversa IA.
 *
 * INPUT:
 * - triageSessionId (String): ID da sessão
 * - escalationReason (String): Motivo da transferência
 *
 * OUTPUT:
 * - transferContext (Object): Contexto completo para transferência
 * - transferredAt (DateTime): Momento da transferência
 * - assignedAgent (String): Agente humano designado
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-005 Agentes IA)
 */
@Slf4j
@Component("transferirComContextoDelegate")
public class TransferirComContextoDelegate implements JavaDelegate {

    @Autowired
    private AiTriageService aiTriageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String sessionId = (String) execution.getVariable("triageSessionId");
        String escalationReason = (String) execution.getVariable("escalationReason");

        log.info("Transferindo para atendimento humano - Sessão: {}, Motivo: {}",
            sessionId, escalationReason);

        try {
            // Preparar contexto de transferência
            TransferContext context = aiTriageService.prepareTransferContext(sessionId);

            // Armazenar contexto
            execution.setVariable("transferContext", context);
            execution.setVariable("transferredAt", java.time.LocalDateTime.now());
            execution.setVariable("conversationHistory", context.getConversationHistory());
            execution.setVariable("symptoms", context.getSymptoms());
            execution.setVariable("aiRecommendations", context.getAiRecommendations());

            // Status da transferência
            execution.setVariable("transferStatus", "PENDING_ASSIGNMENT");

            log.info("Contexto de transferência preparado - {} mensagens no histórico",
                context.getConversationHistory().size());

        } catch (Exception e) {
            log.error("Erro ao preparar transferência - Sessão: {}", sessionId, e);
            execution.setVariable("transferError", e.getMessage());
            throw e;
        }
    }
}
