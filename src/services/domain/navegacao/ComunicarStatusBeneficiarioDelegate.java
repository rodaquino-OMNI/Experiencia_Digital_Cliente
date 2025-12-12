package br.com.austa.experiencia.service.domain.navegacao;

import com.healthplan.services.messaging.MessagingService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Comunicar Status Beneficiário Delegate
 *
 * Comunica status e próximos passos ao beneficiário.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - navigatorName (String): Nome do navegador
 * - appointmentDateTime (DateTime): Data da consulta
 * - confirmationCode (String): Código de confirmação
 *
 * OUTPUT:
 * - notificationSent (Boolean): Notificação enviada?
 * - notificationChannel (String): Canal usado (SMS/Email/WhatsApp)
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-007 Navegação)
 */
@Slf4j
@Component("comunicarStatusBeneficiarioDelegate")
public class ComunicarStatusBeneficiarioDelegate implements JavaDelegate {

    @Autowired
    private MessagingService messagingService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        String navigatorName = (String) execution.getVariable("navigatorName");
        String email = (String) execution.getVariable("email");
        String phone = (String) execution.getVariable("phone");

        log.info("Comunicando status ao beneficiário: {}", beneficiaryId);

        try {
            boolean emailSent = false;
            boolean smsSent = false;

            // Construir mensagem
            String message = buildStatusMessage(execution);

            // Enviar por email
            if (email != null && !email.isEmpty()) {
                messagingService.sendEmail(
                    email,
                    "Atualização da sua jornada de cuidado",
                    message
                );
                emailSent = true;
                log.info("Email de status enviado para: {}", email);
            }

            // Enviar SMS
            if (phone != null && !phone.isEmpty()) {
                String smsMessage = buildSmsMessage(execution);
                messagingService.sendSms(phone, smsMessage);
                smsSent = true;
                log.info("SMS de status enviado para: {}", phone);
            }

            // Armazenar confirmação
            execution.setVariable("notificationSent", emailSent || smsSent);
            execution.setVariable("emailSent", emailSent);
            execution.setVariable("smsSent", smsSent);
            execution.setVariable("notificationTimestamp", java.time.LocalDateTime.now());

        } catch (Exception e) {
            log.error("Erro ao comunicar status - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("notificationSent", false);
            execution.setVariable("notificationError", e.getMessage());
            // Não propaga - comunicação não deve bloquear fluxo
        }
    }

    private String buildStatusMessage(DelegateExecution execution) {
        String navigatorName = (String) execution.getVariable("navigatorName");
        String providerName = (String) execution.getVariable("providerName");
        Object appointmentDateTime = execution.getVariable("appointmentDateTime");
        String confirmationCode = (String) execution.getVariable("confirmationCode");

        return String.format("""
            Olá!

            Sua jornada de cuidado foi iniciada com sucesso.

            Navegador de Cuidados: %s

            Consulta Agendada:
            - Prestador: %s
            - Data/Hora: %s
            - Código de Confirmação: %s

            Seu navegador entrará em contato em breve para acompanhar sua jornada.

            Qualquer dúvida, estamos à disposição!

            Equipe de Experiência Digital
            """,
            navigatorName,
            providerName,
            appointmentDateTime,
            confirmationCode
        );
    }

    private String buildSmsMessage(DelegateExecution execution) {
        String navigatorName = (String) execution.getVariable("navigatorName");
        String confirmationCode = (String) execution.getVariable("confirmationCode");

        return String.format(
            "Sua jornada de cuidado foi iniciada! " +
            "Navegador: %s. " +
            "Consulta confirmada: %s. " +
            "Detalhes enviados por email.",
            navigatorName,
            confirmationCode
        );
    }
}
