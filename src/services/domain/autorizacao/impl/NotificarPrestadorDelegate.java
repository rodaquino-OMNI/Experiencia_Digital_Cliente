package br.com.austa.experiencia.service.domain.autorizacao.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate responsible for notifying healthcare provider of authorization decision.
 *
 * <p>SUB-006 Autorização - Provider Notification</p>
 *
 * <p>Functionality:</p>
 * <ul>
 *   <li>Sends authorization decision to healthcare provider</li>
 *   <li>Includes decision details and next steps</li>
 *   <li>Provides authorization codes and validity periods</li>
 *   <li>Tracks notification delivery and acknowledgment</li>
 * </ul>
 *
 * <p>Input Variables:</p>
 * <ul>
 *   <li><b>prestadorId</b> (String): Provider ID</li>
 *   <li><b>decisao</b> (String): Authorization decision</li>
 *   <li><b>codigoAutorizacao</b> (String): Authorization code if approved</li>
 *   <li><b>justificativa</b> (String): Decision justification</li>
 * </ul>
 *
 * <p>Output Variables:</p>
 * <ul>
 *   <li><b>notificacaoPrestadorEnviada</b> (Boolean): Notification sent flag</li>
 *   <li><b>canalNotificacao</b> (String): Notification channel used</li>
 *   <li><b>timestampNotificacao</b> (LocalDateTime): Notification timestamp</li>
 *   <li><b>protocoloNotificacao</b> (String): Notification protocol number</li>
 * </ul>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("notificarPrestadorDelegate")
public class NotificarPrestadorDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting provider notification - Process: {}, Execution: {}",
                execution.getProcessInstanceId(), execution.getId());

        try {
            // Extract input parameters
            String prestadorId = (String) execution.getVariable("prestadorId");
            String decisao = (String) execution.getVariable("decisao");
            String codigoAutorizacao = (String) execution.getVariable("codigoAutorizacao");
            String justificativa = (String) execution.getVariable("justificativa");

            log.debug("Notifying provider: {}, decision: {}", prestadorId, decisao);

            // Get provider contact information
            Map<String, String> providerContact = getProviderContactInfo(prestadorId);

            // Prepare notification message
            Map<String, Object> notificationMessage = prepareProviderNotification(
                    decisao, codigoAutorizacao, justificativa, execution);

            // Determine best notification channel
            String channel = determineNotificationChannel(providerContact);

            // Send notification
            String protocoloNotificacao = sendProviderNotification(
                    prestadorId, notificationMessage, channel);

            // Track notification
            trackNotificationDelivery(prestadorId, protocoloNotificacao, channel);

            // Set output variables
            LocalDateTime timestamp = LocalDateTime.now();
            execution.setVariable("notificacaoPrestadorEnviada", true);
            execution.setVariable("canalNotificacao", channel);
            execution.setVariable("timestampNotificacao", timestamp);
            execution.setVariable("protocoloNotificacao", protocoloNotificacao);

            log.info("Successfully notified provider: {}, protocol: {}, channel: {}",
                    prestadorId, protocoloNotificacao, channel);

        } catch (Exception e) {
            log.error("Error notifying provider - Process: {}, Error: {}",
                    execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("errorMessage", "Failed to notify provider: " + e.getMessage());
            execution.setVariable("hasError", true);
            execution.setVariable("notificacaoPrestadorEnviada", false);

            throw new RuntimeException("Error in NotificarPrestadorDelegate", e);
        }
    }

    /**
     * Retrieves provider contact information.
     */
    private Map<String, String> getProviderContactInfo(String prestadorId) {
        // Production implementation would query provider database
        Map<String, String> contact = new HashMap<>();
        contact.put("email", "provider@healthcare.com");
        contact.put("phone", "+55 11 98765-4321");
        contact.put("portal", "https://portal.provider.com");
        contact.put("preferredChannel", "PORTAL");

        return contact;
    }

    /**
     * Prepares comprehensive notification message for provider.
     */
    private Map<String, Object> prepareProviderNotification(String decisao,
                                                            String codigoAutorizacao,
                                                            String justificativa,
                                                            DelegateExecution execution) {
        Map<String, Object> notification = new HashMap<>();

        // Decision information
        notification.put("decisao", decisao);
        notification.put("justificativa", justificativa);
        notification.put("dataDecisao", LocalDateTime.now());

        // Authorization details if approved
        if ("APROVADA".equals(decisao) && codigoAutorizacao != null) {
            notification.put("codigoAutorizacao", codigoAutorizacao);
            notification.put("validade", LocalDateTime.now().plusDays(30));
            notification.put("instrucoes", "Proceder com o procedimento conforme solicitado");
        } else if ("NEGADA".equals(decisao)) {
            notification.put("motivoNegacao", justificativa);
            notification.put("recursoPossivel", true);
            notification.put("prazoRecurso", "30 dias");
        }

        // Request information
        notification.put("solicitacaoId", execution.getVariable("solicitacaoId"));
        notification.put("beneficiarioId", execution.getVariable("beneficiarioId"));
        notification.put("procedimento", execution.getVariable("procedimento"));

        // Next steps
        if ("APROVADA".equals(decisao)) {
            notification.put("proximosPassos",
                "1. Executar procedimento\n2. Registrar atendimento\n3. Enviar faturamento");
        } else {
            notification.put("proximosPassos",
                "1. Revisar justificativa\n2. Considerar recurso\n3. Contatar operadora se necessário");
        }

        return notification;
    }

    /**
     * Determines best notification channel based on provider preferences.
     */
    private String determineNotificationChannel(Map<String, String> contact) {
        String preferred = contact.get("preferredChannel");

        if ("PORTAL".equals(preferred)) {
            return "PORTAL";
        } else if (contact.containsKey("email")) {
            return "EMAIL";
        } else if (contact.containsKey("phone")) {
            return "SMS";
        }

        return "PORTAL"; // Default fallback
    }

    /**
     * Sends notification to provider through selected channel.
     */
    private String sendProviderNotification(String prestadorId,
                                           Map<String, Object> message,
                                           String channel) {
        // Production implementation would:
        // 1. Format message for channel
        // 2. Send through notification service
        // 3. Handle delivery failures
        // 4. Return protocol number

        String protocol = "PROV-" + System.currentTimeMillis();
        log.debug("Sent notification via {} to provider: {}, protocol: {}",
                channel, prestadorId, protocol);

        return protocol;
    }

    /**
     * Tracks notification delivery for audit trail.
     */
    private void trackNotificationDelivery(String prestadorId, String protocol, String channel) {
        // Production implementation would:
        // 1. Log to notification_history table
        // 2. Track delivery status
        // 3. Enable acknowledgment tracking
        // 4. Support notification resend if needed

        log.debug("Tracked notification delivery: provider={}, protocol={}", prestadorId, protocol);
    }
}
