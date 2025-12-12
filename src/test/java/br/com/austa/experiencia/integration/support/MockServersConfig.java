package br.com.austa.experiencia.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Configuration for Mock Servers used in Integration Tests
 *
 * Provides WireMock servers for external integrations:
 * - NLP Service (sentiment analysis, intent classification)
 * - ANS API (regulatory reporting)
 * - External Health Systems (EMR, lab results)
 * - Payment Gateway (compensation processing)
 * - Notification Services (SMS, email, WhatsApp)
 * - Analytics Platform (metrics, reporting)
 *
 * @see PROMPT_TECNICO_3.MD Lines 1364-1390
 */
@Configuration
@Profile("test")
public class MockServersConfig {

    /**
     * Mock server for NLP services (intent classification, sentiment analysis)
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer nlpMockServer() {
        WireMockServer server = new WireMockServer(
            WireMockConfiguration.options()
                .port(8081)
                .usingFilesUnderClasspath("wiremock/nlp")
        );

        // Default stubs
        server.stubFor(post(urlEqualTo("/api/nlp/classify-intent"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"intent\": \"AGENDAMENTO\", \"confidence\": 0.95}")));

        server.stubFor(post(urlEqualTo("/api/nlp/analyze-sentiment"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"sentiment\": \"POSITIVO\", \"score\": 0.85}")));

        return server;
    }

    /**
     * Mock server for ANS (Agência Nacional de Saúde Suplementar) API
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer ansMockServer() {
        WireMockServer server = new WireMockServer(
            WireMockConfiguration.options()
                .port(8082)
                .usingFilesUnderClasspath("wiremock/ans")
        );

        // ANS complaint registration
        server.stubFor(post(urlEqualTo("/api/ans/reclamacao/registrar"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"protocoloANS\": \"ANS123456789\", \"prazoResposta\": \"2025-12-31\"}")));

        // ANS resolution notification
        server.stubFor(post(urlMatching("/api/ans/reclamacao/.*/resolucao"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"aceito\": true, \"dataRecebimento\": \"2025-12-11T10:00:00\"}")));

        return server;
    }

    /**
     * Mock server for External Health Systems (EMR, Lab Results)
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer healthSystemMockServer() {
        WireMockServer server = new WireMockServer(
            WireMockConfiguration.options()
                .port(8083)
                .usingFilesUnderClasspath("wiremock/health")
        );

        // Get beneficiary health records
        server.stubFor(get(urlMatching("/api/emr/beneficiario/.*/records"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"records\": [], \"totalCount\": 0}")));

        // Get lab results
        server.stubFor(get(urlMatching("/api/lab/results/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"results\": [], \"status\": \"AVAILABLE\"}")));

        return server;
    }

    /**
     * Mock server for Payment Gateway (compensation processing)
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer paymentMockServer() {
        WireMockServer server = new WireMockServer(
            WireMockConfiguration.options()
                .port(8084)
                .usingFilesUnderClasspath("wiremock/payment")
        );

        // Process refund
        server.stubFor(post(urlEqualTo("/api/payment/refund"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"transactionId\": \"TXN123456\", \"status\": \"APPROVED\", \"amount\": 450.00}")));

        // Check payment status
        server.stubFor(get(urlMatching("/api/payment/status/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\": \"COMPLETED\", \"processedAt\": \"2025-12-11T10:00:00\"}")));

        return server;
    }

    /**
     * Mock server for Notification Services (SMS, Email, WhatsApp)
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer notificationMockServer() {
        WireMockServer server = new WireMockServer(
            WireMockConfiguration.options()
                .port(8085)
                .usingFilesUnderClasspath("wiremock/notification")
        );

        // Send SMS
        server.stubFor(post(urlEqualTo("/api/notification/sms"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"messageId\": \"MSG123\", \"status\": \"SENT\"}")));

        // Send Email
        server.stubFor(post(urlEqualTo("/api/notification/email"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"messageId\": \"EMAIL123\", \"status\": \"SENT\"}")));

        // Send WhatsApp
        server.stubFor(post(urlEqualTo("/api/notification/whatsapp"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"messageId\": \"WA123\", \"status\": \"DELIVERED\"}")));

        return server;
    }

    /**
     * Mock server for Analytics Platform (metrics, reporting)
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer analyticsMockServer() {
        WireMockServer server = new WireMockServer(
            WireMockConfiguration.options()
                .port(8086)
                .usingFilesUnderClasspath("wiremock/analytics")
        );

        // Record event
        server.stubFor(post(urlEqualTo("/api/analytics/event"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"eventId\": \"EVT123\", \"recorded\": true}")));

        // Get metrics
        server.stubFor(get(urlMatching("/api/analytics/metrics.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"metrics\": {}, \"period\": \"DAILY\"}")));

        return server;
    }
}
