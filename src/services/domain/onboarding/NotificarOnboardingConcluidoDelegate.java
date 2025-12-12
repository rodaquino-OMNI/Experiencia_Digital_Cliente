package br.com.austa.experiencia.service.domain.onboarding;

import com.healthplan.services.events.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Notificar Onboarding Concluído Delegate
 *
 * Publica evento de onboarding concluído para sistemas downstream.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - carePlanId (String): ID do plano de cuidados
 * - tasyPatientId (String): ID Tasy
 *
 * OUTPUT:
 * - eventPublished (Boolean): Evento publicado?
 * - eventId (String): ID do evento
 * - completionTimestamp (DateTime): Timestamp de conclusão
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-001 Onboarding)
 */
@Slf4j
@Component("notificarOnboardingConcluidoDelegate")
public class NotificarOnboardingConcluidoDelegate implements JavaDelegate {

    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        String carePlanId = (String) execution.getVariable("carePlanId");
        String tasyPatientId = (String) execution.getVariable("tasyPatientId");

        log.info("Publicando evento de onboarding concluído - Beneficiário: {}", beneficiaryId);

        try {
            // Construir payload do evento
            Map<String, Object> eventPayload = Map.of(
                "eventType", "ONBOARDING_COMPLETED",
                "beneficiaryId", beneficiaryId,
                "carePlanId", carePlanId != null ? carePlanId : "",
                "tasyPatientId", tasyPatientId != null ? tasyPatientId : "",
                "riskLevel", execution.getVariable("riskLevel") != null ?
                    execution.getVariable("riskLevel") : "UNKNOWN",
                "completedAt", java.time.LocalDateTime.now(),
                "source", "ONBOARDING_WORKFLOW"
            );

            // Publicar evento
            String eventId = eventPublisher.publish(
                "healthplan.onboarding.completed",
                eventPayload
            );

            // Armazenar confirmação
            execution.setVariable("eventPublished", true);
            execution.setVariable("eventId", eventId);
            execution.setVariable("completionTimestamp", java.time.LocalDateTime.now());

            log.info("Evento de onboarding publicado - Event ID: {}", eventId);

        } catch (Exception e) {
            log.error("Erro ao publicar evento de onboarding - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("eventPublished", false);
            execution.setVariable("eventError", e.getMessage());
            // Não propaga - publicação de evento não deve bloquear conclusão
        }
    }
}
