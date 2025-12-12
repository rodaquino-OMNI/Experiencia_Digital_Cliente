package br.com.austa.experiencia.service.domain.onboarding;

import com.healthplan.services.messaging.MessagingService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Enviar Boas-Vindas Delegate
 *
 * Envia mensagem de boas-vindas personalizada ao novo beneficiário.
 *
 * INPUT:
 * - beneficiaryId (String): ID do beneficiário
 * - name (String): Nome do beneficiário
 * - email (String): Email
 * - phone (String): Telefone
 *
 * OUTPUT:
 * - welcomeEmailSent (Boolean): Email enviado?
 * - welcomeSmsSent (Boolean): SMS enviado?
 * - welcomeMessageId (String): ID da mensagem
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (SUB-001 Onboarding)
 */
@Slf4j
@Component("enviarBoasVindasDelegate")
public class EnviarBoasVindasDelegate implements JavaDelegate {

    @Autowired
    private MessagingService messagingService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String beneficiaryId = (String) execution.getVariable("beneficiaryId");
        String name = (String) execution.getVariable("name");
        String email = (String) execution.getVariable("email");
        String phone = (String) execution.getVariable("phone");

        log.info("Enviando boas-vindas - Beneficiário: {}", beneficiaryId);

        try {
            boolean emailSent = false;
            boolean smsSent = false;

            // Enviar email de boas-vindas
            if (email != null && !email.isEmpty()) {
                String emailContent = buildWelcomeEmail(name);
                messagingService.sendEmail(email, "Bem-vindo(a)!", emailContent);
                emailSent = true;
                log.info("Email de boas-vindas enviado para: {}", email);
            }

            // Enviar SMS de boas-vindas
            if (phone != null && !phone.isEmpty()) {
                String smsContent = buildWelcomeSms(name);
                messagingService.sendSms(phone, smsContent);
                smsSent = true;
                log.info("SMS de boas-vindas enviado para: {}", phone);
            }

            // Armazenar resultados
            execution.setVariable("welcomeEmailSent", emailSent);
            execution.setVariable("welcomeSmsSent", smsSent);
            execution.setVariable("welcomeSentAt", java.time.LocalDateTime.now());

        } catch (Exception e) {
            log.error("Erro ao enviar boas-vindas - Beneficiário: {}", beneficiaryId, e);
            execution.setVariable("welcomeError", e.getMessage());
            // Não propaga - mensagem de boas-vindas não deve bloquear onboarding
        }
    }

    private String buildWelcomeEmail(String name) {
        return String.format("""
            Olá %s,

            Seja bem-vindo(a) ao nosso plano de saúde!

            Estamos felizes em tê-lo(a) conosco. Sua jornada de cuidado personalizado começa agora.

            Nos próximos dias, você receberá:
            - Seu cartão digital de beneficiário
            - Informações sobre a rede credenciada
            - Acesso ao portal do beneficiário
            - Orientações sobre como utilizar seus benefícios

            Qualquer dúvida, estamos à disposição!

            Equipe de Experiência Digital
            """, name);
    }

    private String buildWelcomeSms(String name) {
        return String.format(
            "Olá %s! Bem-vindo(a) ao plano de saúde. " +
            "Acesse seu cartão digital em nosso app. " +
            "Dúvidas? Fale conosco pelo WhatsApp.",
            name
        );
    }
}
