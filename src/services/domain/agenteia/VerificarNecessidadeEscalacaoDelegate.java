package br.com.austa.experiencia.service.domain.agenteia;

import com.healthplan.services.ai.AiTriageService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Verificar Necessidade Escalação Delegate
 *
 * Verifica se caso requer escalação para atendimento humano.
 *
 * INPUT:
 * - triageSessionId (String): ID da sessão
 *
 * OUTPUT:
 * - needsEscalation (Boolean): Requer escalação?
 * - escalationReason (String): Motivo da escalação
 * - urgencyLevel (String): Nível de urgência
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-005 Agentes IA)
 */
@Slf4j
@Component("verificarNecessidadeEscalacaoDelegate")
public class VerificarNecessidadeEscalacaoDelegate implements JavaDelegate {

    @Autowired
    private AiTriageService aiTriageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String sessionId = (String) execution.getVariable("triageSessionId");

        log.info("Verificando necessidade de escalação - Sessão: {}", sessionId);

        try {
            // Verificar se precisa escalar
            boolean needsEscalation = aiTriageService.needsEscalation(sessionId);

            // Determinar motivo e urgência
            String escalationReason = determineEscalationReason(execution);
            String urgencyLevel = determineUrgencyLevel(execution);

            // Armazenar resultados
            execution.setVariable("needsEscalation", needsEscalation);
            execution.setVariable("escalationReason", escalationReason);
            execution.setVariable("urgencyLevel", urgencyLevel);

            log.info("Verificação concluída - Escalar: {}, Motivo: {}",
                needsEscalation, escalationReason);

        } catch (Exception e) {
            log.error("Erro ao verificar escalação - Sessão: {}", sessionId, e);
            // Em caso de erro, escalar por segurança
            execution.setVariable("needsEscalation", true);
            execution.setVariable("escalationReason", "SYSTEM_ERROR");
            execution.setVariable("urgencyLevel", "HIGH");
        }
    }

    private String determineEscalationReason(DelegateExecution execution) {
        String severity = (String) execution.getVariable("severity");

        if ("CRITICAL".equals(severity)) {
            return "CRITICAL_SEVERITY";
        }

        Double confidence = (Double) execution.getVariable("aiConfidence");
        if (confidence != null && confidence < 0.6) {
            return "LOW_CONFIDENCE";
        }

        return "COMPLEX_CASE";
    }

    private String determineUrgencyLevel(DelegateExecution execution) {
        String severity = (String) execution.getVariable("severity");

        return switch (severity != null ? severity : "UNKNOWN") {
            case "CRITICAL" -> "IMMEDIATE";
            case "HIGH" -> "URGENT";
            case "MEDIUM" -> "PRIORITY";
            default -> "NORMAL";
        };
    }
}
