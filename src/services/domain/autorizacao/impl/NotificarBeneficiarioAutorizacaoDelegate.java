package br.com.austa.experiencia.service.domain.autorizacao.impl;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate responsible for notifying beneficiary of authorization decision.
 *
 * <p>SUB-006 Autorização - Beneficiary Notification</p>
 *
 * <p>Functionality:</p>
 * <ul>
 *   <li>Sends user-friendly authorization decision to beneficiary</li>
 *   <li>Explains decision in clear, accessible language</li>
 *   <li>Provides next steps and available options</li>
 *   <li>Enables beneficiary engagement and feedback</li>
 * </ul>
 *
 * <p>Input Variables:</p>
 * <ul>
 *   <li><b>beneficiarioId</b> (String): Beneficiary ID</li>
 *   <li><b>decisao</b> (String): Authorization decision</li>
 *   <li><b>codigoAutorizacao</b> (String): Authorization code if approved</li>
 *   <li><b>justificativa</b> (String): Decision justification</li>
 * </ul>
 *
 * <p>Output Variables:</p>
 * <ul>
 *   <li><b>notificacaoBeneficiarioEnviada</b> (Boolean): Notification sent flag</li>
 *   <li><b>canalPreferido</b> (String): Preferred communication channel</li>
 *   <li><b>timestampNotificacaoBeneficiario</b> (LocalDateTime): Notification timestamp</li>
 *   <li><b>protocoloBeneficiario</b> (String): Notification protocol number</li>
 * </ul>
 *
 * @author Digital Experience Team
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("notificarBeneficiarioAutorizacaoDelegate")
public class NotificarBeneficiarioAutorizacaoDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting beneficiary authorization notification - Process: {}, Execution: {}",
                execution.getProcessInstanceId(), execution.getId());

        try {
            // Extract input parameters
            String beneficiarioId = (String) execution.getVariable("beneficiarioId");
            String decisao = (String) execution.getVariable("decisao");
            String codigoAutorizacao = (String) execution.getVariable("codigoAutorizacao");
            String justificativa = (String) execution.getVariable("justificativa");

            log.debug("Notifying beneficiary: {}, decision: {}", beneficiarioId, decisao);

            // Get beneficiary preferences
            Map<String, Object> beneficiaryPrefs = getBeneficiaryPreferences(beneficiarioId);

            // Prepare user-friendly notification
            Map<String, Object> notificationMessage = prepareBeneficiaryNotification(
                    decisao, codigoAutorizacao, justificativa, execution);

            // Determine preferred channel
            String channel = (String) beneficiaryPrefs.get("preferredChannel");
            if (channel == null) {
                channel = "APP"; // Default to mobile app
            }

            // Send multi-channel notification
            String protocoloBeneficiario = sendBeneficiaryNotification(
                    beneficiarioId, notificationMessage, channel, beneficiaryPrefs);

            // Track beneficiary engagement
            trackBeneficiaryEngagement(beneficiarioId, protocoloBeneficiario, decisao);

            // Set output variables
            LocalDateTime timestamp = LocalDateTime.now();
            execution.setVariable("notificacaoBeneficiarioEnviada", true);
            execution.setVariable("canalPreferido", channel);
            execution.setVariable("timestampNotificacaoBeneficiario", timestamp);
            execution.setVariable("protocoloBeneficiario", protocoloBeneficiario);

            log.info("Successfully notified beneficiary: {}, protocol: {}, channel: {}",
                    beneficiarioId, protocoloBeneficiario, channel);

        } catch (Exception e) {
            log.error("Error notifying beneficiary - Process: {}, Error: {}",
                    execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("errorMessage", "Failed to notify beneficiary: " + e.getMessage());
            execution.setVariable("hasError", true);
            execution.setVariable("notificacaoBeneficiarioEnviada", false);

            throw new RuntimeException("Error in NotificarBeneficiarioAutorizacaoDelegate", e);
        }
    }

    /**
     * Retrieves beneficiary communication preferences.
     */
    private Map<String, Object> getBeneficiaryPreferences(String beneficiarioId) {
        // Production implementation would query beneficiary profile
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("preferredChannel", "APP");
        prefs.put("emailEnabled", true);
        prefs.put("smsEnabled", true);
        prefs.put("pushEnabled", true);
        prefs.put("language", "pt-BR");
        prefs.put("accessibilityMode", false);

        return prefs;
    }

    /**
     * Prepares user-friendly notification message.
     */
    private Map<String, Object> prepareBeneficiaryNotification(String decisao,
                                                               String codigoAutorizacao,
                                                               String justificativa,
                                                               DelegateExecution execution) {
        Map<String, Object> notification = new HashMap<>();

        // User-friendly title and message
        if ("APROVADA".equals(decisao)) {
            notification.put("titulo", "✓ Sua autorização foi aprovada!");
            notification.put("mensagem",
                "Ótimas notícias! Sua solicitação de procedimento foi autorizada. " +
                "Você já pode agendar com seu prestador.");
            notification.put("codigoAutorizacao", codigoAutorizacao);
            notification.put("validade", LocalDateTime.now().plusDays(30));
        } else if ("NEGADA".equals(decisao)) {
            notification.put("titulo", "Sua solicitação precisa de atenção");
            notification.put("mensagem",
                "Infelizmente não foi possível aprovar sua solicitação no momento. " +
                "Veja os detalhes e as opções disponíveis.");
            notification.put("motivoSimplificado", simplifyJustification(justificativa));
            notification.put("opcoesRecurso", true);
        } else if ("PENDENTE".equals(decisao)) {
            notification.put("titulo", "Sua solicitação está em análise");
            notification.put("mensagem",
                "Recebemos sua solicitação e estamos analisando. " +
                "Você será notificado assim que tivermos uma resposta.");
        }

        // Request details
        notification.put("procedimento", execution.getVariable("procedimento"));
        notification.put("prestador", execution.getVariable("prestador"));
        notification.put("solicitacaoId", execution.getVariable("solicitacaoId"));

        // Next steps for beneficiary
        if ("APROVADA".equals(decisao)) {
            notification.put("proximosPassos",
                "1. Anote seu código de autorização\n" +
                "2. Agende com seu prestador\n" +
                "3. Apresente o código no atendimento");
        } else if ("NEGADA".equals(decisao)) {
            notification.put("proximosPassos",
                "1. Revise o motivo da negação\n" +
                "2. Consulte seu médico sobre alternativas\n" +
                "3. Se necessário, solicite recurso em até 30 dias");
        }

        // Support options
        notification.put("suporte", Map.of(
            "chat", "Disponível 24/7 no app",
            "telefone", "0800 123 4567",
            "email", "atendimento@operadora.com.br"
        ));

        return notification;
    }

    /**
     * Sends notification through multiple channels.
     */
    private String sendBeneficiaryNotification(String beneficiarioId,
                                               Map<String, Object> message,
                                               String primaryChannel,
                                               Map<String, Object> prefs) {
        String protocol = "BEN-" + System.currentTimeMillis();

        // Send through primary channel
        sendThroughChannel(beneficiarioId, message, primaryChannel, protocol);

        // Send backup notification via email if critical
        if (Boolean.TRUE.equals(prefs.get("emailEnabled")) &&
            "APROVADA".equals(message.get("decisao"))) {
            sendThroughChannel(beneficiarioId, message, "EMAIL", protocol);
        }

        log.debug("Sent multi-channel notification to beneficiary: {}, protocol: {}",
                beneficiarioId, protocol);

        return protocol;
    }

    /**
     * Sends notification through specific channel.
     */
    private void sendThroughChannel(String beneficiarioId, Map<String, Object> message,
                                   String channel, String protocol) {
        // Production implementation would:
        // 1. Format message for channel (APP/EMAIL/SMS/PUSH)
        // 2. Send through notification service
        // 3. Handle delivery failures with retries
        // 4. Track delivery status

        log.debug("Sending notification via {} to beneficiary: {}", channel, beneficiarioId);
    }

    /**
     * Tracks beneficiary engagement with notification.
     */
    private void trackBeneficiaryEngagement(String beneficiarioId, String protocol, String decisao) {
        // Production implementation would:
        // 1. Log notification delivery
        // 2. Track open/read status
        // 3. Monitor satisfaction feedback
        // 4. Enable follow-up actions

        log.debug("Tracking engagement for beneficiary: {}, protocol: {}", beneficiarioId, protocol);
    }

    /**
     * Simplifies technical justification for beneficiary understanding.
     */
    private String simplifyJustification(String technicalJustification) {
        if (technicalJustification == null) {
            return "Informações adicionais são necessárias para aprovação.";
        }

        // Production implementation would use NLP to simplify medical/technical language
        return technicalJustification;
    }
}
