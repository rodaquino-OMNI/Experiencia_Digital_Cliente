package com.healthinsurance.experienciadigital.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ANS Service - Regulatory Agency Notification Delegate
 *
 * Handles notifications to ANS (Agência Nacional de Saúde Suplementar)
 * for regulatory compliance and mandatory reporting.
 *
 * BPMN Coverage:
 * - ansService.notificar (ANS regulatory notification)
 *
 * Retry Policy: R3/PT1M (3 retries, 1 minute interval)
 */
@Component("ansService")
public class AnsService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(AnsService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ans.api.baseUrl:https://api.ans.gov.br}")
    private String ansApiBaseUrl;

    @Value("${ans.api.token}")
    private String ansApiToken;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String method = (String) execution.getVariable("ansMethod");

        if ("notificar".equals(method)) {
            notificar(execution);
        } else {
            logger.warn("Unknown ANS method: {}", method);
            throw new IllegalArgumentException("Invalid ANS method: " + method);
        }
    }

    /**
     * Notify ANS regulatory agency
     *
     * Sends mandatory notifications to ANS portal for regulatory compliance.
     * Examples: complaints, SLA violations, quality issues, coverage denials.
     *
     * @param execution Process execution context
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 60000) // 1 minute
    )
    public void notificar(DelegateExecution execution) throws Exception {
        logger.info("Executing ansService.notificar for process {}",
            execution.getProcessInstanceId());

        try {
            // Extract notification parameters
            String tipoNotificacao = (String) execution.getVariable("tipoNotificacao");
            String registroAns = (String) execution.getVariable("registroAns");
            String beneficiarioCpf = (String) execution.getVariable("beneficiarioCpf");
            String descricaoIncidente = (String) execution.getVariable("descricaoIncidente");
            String protocolo = (String) execution.getVariable("protocolo");

            // Build notification payload
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("tipoNotificacao", tipoNotificacao);
            notificationPayload.put("registroOperadora", registroAns);
            notificationPayload.put("cpfBeneficiario", beneficiarioCpf);
            notificationPayload.put("descricao", descricaoIncidente);
            notificationPayload.put("protocoloInterno", protocolo);
            notificationPayload.put("dataHoraOcorrencia", LocalDateTime.now().toString());
            notificationPayload.put("canalOrigem", execution.getVariable("canal"));

            // Prepare HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ansApiToken);
            headers.set("X-Request-ID", execution.getProcessInstanceId());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(notificationPayload, headers);

            // Send notification to ANS
            String ansEndpoint = ansApiBaseUrl + "/notificacoes/operadoras";
            ResponseEntity<Map> response = restTemplate.exchange(
                ansEndpoint,
                HttpMethod.POST,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK ||
                response.getStatusCode() == HttpStatus.CREATED) {

                Map<String, Object> responseBody = response.getBody();
                String protocoloAns = (String) responseBody.get("protocoloAns");
                String statusNotificacao = (String) responseBody.get("status");

                // Store ANS protocol number
                execution.setVariable("protocoloAns", protocoloAns);
                execution.setVariable("ansNotificacaoStatus", statusNotificacao);
                execution.setVariable("ansNotificadoEm", LocalDateTime.now().toString());
                execution.setVariable("ansNotificadoSucesso", true);

                logger.info("ANS notification successful. Protocol: {}, Status: {}",
                    protocoloAns, statusNotificacao);

            } else {
                logger.error("ANS notification failed with status: {}", response.getStatusCode());
                execution.setVariable("ansNotificadoSucesso", false);
                execution.setVariable("ansErroMensagem", "HTTP " + response.getStatusCode());
                throw new RuntimeException("ANS notification failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error notifying ANS for process {}: {}",
                execution.getProcessInstanceId(), e.getMessage(), e);

            execution.setVariable("ansNotificadoSucesso", false);
            execution.setVariable("ansErroMensagem", e.getMessage());

            // On final retry failure, flag for manual intervention
            if (execution.hasVariable("ansRetryCount")) {
                int retryCount = (int) execution.getVariable("ansRetryCount");
                execution.setVariable("ansRetryCount", retryCount + 1);

                if (retryCount >= 3) {
                    execution.setVariable("ansRequerIntervencaoManual", true);
                    logger.error("ANS notification failed after 3 retries. Manual intervention required.");
                }
            } else {
                execution.setVariable("ansRetryCount", 1);
            }

            throw e;
        }
    }
}
