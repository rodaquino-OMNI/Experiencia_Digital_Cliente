package br.com.austa.experiencia.service.common;

import br.com.austa.experiencia.service.integration.WhatsAppService;
import br.com.austa.experiencia.exception.IntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Delegate responsável por enviar mensagens via WhatsApp Business API.
 * Utilizado em múltiplos subprocessos para comunicação com beneficiários.
 *
 * Referenciado em: Todos os subprocessos (common delegate)
 * Activity ID: Activity_EnviarWhatsapp
 *
 * Variáveis de entrada:
 * - telefone (String): Número do telefone do destinatário (formato internacional)
 * - mensagem (String): Conteúdo da mensagem a ser enviada
 * - templateId (String, opcional): ID do template WhatsApp aprovado
 * - parametrosTemplate (Map, opcional): Parâmetros para preenchimento do template
 * - anexos (List<String>, opcional): URLs de arquivos anexados
 *
 * Variáveis de saída:
 * - whatsappEnviado (Boolean): Flag de sucesso do envio
 * - messageId (String): ID da mensagem enviada
 * - timestampEnvio (LocalDateTime): Timestamp do envio
 * - erroEnvio (String): Mensagem de erro em caso de falha
 *
 * @author AI Agent
 * @version 1.0
 * @since 2025-12-11
 */
@Slf4j
@Component("enviarWhatsappDelegate")
@RequiredArgsConstructor
public class EnviarWhatsappDelegate implements JavaDelegate {

    private final WhatsAppService whatsAppService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Iniciando envio de mensagem WhatsApp - ProcessInstance: {}",
                 execution.getProcessInstanceId());

        try {
            // 1. Extrair variáveis de entrada
            String telefone = (String) execution.getVariable("telefone");
            String mensagem = (String) execution.getVariable("mensagem");
            String templateId = (String) execution.getVariable("templateId");

            @SuppressWarnings("unchecked")
            Map<String, Object> parametrosTemplate =
                (Map<String, Object>) execution.getVariable("parametrosTemplate");

            // 2. Validar dados obrigatórios
            validateInputs(telefone, mensagem, templateId);

            // 3. Enviar mensagem (com ou sem template)
            String messageId;
            if (templateId != null && !templateId.isBlank()) {
                messageId = whatsAppService.enviarComTemplate(telefone, templateId, parametrosTemplate);
                log.debug("Mensagem enviada usando template: {}", templateId);
            } else {
                messageId = whatsAppService.enviarMensagem(telefone, mensagem);
                log.debug("Mensagem de texto livre enviada");
            }

            // 4. Definir variáveis de saída
            execution.setVariable("whatsappEnviado", true);
            execution.setVariable("messageId", messageId);
            execution.setVariable("timestampEnvio", java.time.LocalDateTime.now());

            log.info("Mensagem WhatsApp enviada com sucesso - MessageId: {}, Telefone: {}",
                     messageId, maskPhone(telefone));

        } catch (IntegrationException e) {
            log.error("Erro de integração ao enviar WhatsApp: {}", e.getMessage(), e);
            execution.setVariable("whatsappEnviado", false);
            execution.setVariable("erroEnvio", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Erro inesperado ao enviar WhatsApp: {}", e.getMessage(), e);
            execution.setVariable("whatsappEnviado", false);
            execution.setVariable("erroEnvio", "Erro interno ao enviar mensagem");
            throw new RuntimeException("Falha ao enviar mensagem WhatsApp", e);
        }
    }

    private void validateInputs(String telefone, String mensagem, String templateId) {
        if (telefone == null || telefone.isBlank()) {
            throw new IllegalArgumentException("telefone é obrigatório");
        }
        if ((mensagem == null || mensagem.isBlank()) &&
            (templateId == null || templateId.isBlank())) {
            throw new IllegalArgumentException("mensagem ou templateId é obrigatório");
        }
        if (!telefone.matches("\\+\\d{10,15}")) {
            throw new IllegalArgumentException("telefone deve estar em formato internacional (+55...)");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 2);
    }
}
