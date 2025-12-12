package br.com.austa.experiencia.service.domain.agenteia;

import com.healthplan.services.ai.AiTriageService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registrar Atendimento IA Delegate
 *
 * Registra interação de atendimento IA para analytics e auditoria.
 *
 * INPUT:
 * - triageSessionId (String): ID da sessão
 * - outcome (String): Resultado do atendimento
 * - resolved (Boolean): Resolvido pela IA?
 *
 * OUTPUT:
 * - interactionRecorded (Boolean): Registro concluído
 * - recordId (String): ID do registro
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-005 Agentes IA)
 */
@Slf4j
@Component("registrarAtendimentoIaDelegate")
public class RegistrarAtendimentoIaDelegate implements JavaDelegate {

    @Autowired
    private AiTriageService aiTriageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String sessionId = (String) execution.getVariable("triageSessionId");
        String outcome = (String) execution.getVariable("outcome");
        Boolean resolved = (Boolean) execution.getVariable("resolvedByAi");

        log.info("Registrando atendimento IA - Sessão: {}, Resultado: {}",
            sessionId, outcome);

        try {
            // Registrar interação
            aiTriageService.registerInteraction(sessionId, outcome);

            // Armazenar confirmação
            execution.setVariable("interactionRecorded", true);
            execution.setVariable("recordedAt", java.time.LocalDateTime.now());
            execution.setVariable("aiResolutionRate",
                resolved != null && resolved ? 1.0 : 0.0);

            log.info("Atendimento IA registrado com sucesso - Sessão: {}", sessionId);

        } catch (Exception e) {
            log.error("Erro ao registrar atendimento IA - Sessão: {}", sessionId, e);
            execution.setVariable("interactionRecorded", false);
            execution.setVariable("recordError", e.getMessage());
            // Não propaga erro - logging não deve bloquear fluxo
        }
    }
}
