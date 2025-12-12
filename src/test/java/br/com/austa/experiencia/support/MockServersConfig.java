package br.com.austa.experiencia.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock configuration for mocking external API dependencies.
 *
 * Provides mock servers for:
 * - Tasy ERP API (patient records, billing)
 * - WhatsApp Business API (messaging)
 * - NLP/GPT-4 API (natural language processing)
 * - OCR/Computer Vision API (document processing)
 *
 * Each mock server runs on a dynamic port and can be configured with stubs for testing.
 *
 * @see com.github.tomakehurst.wiremock.WireMockServer
 */
@TestConfiguration
public class MockServersConfig {

    private static WireMockServer tasyMockServer;
    private static WireMockServer whatsappMockServer;
    private static WireMockServer nlpMockServer;
    private static WireMockServer ocrMockServer;

    /**
     * Initializes all mock servers with default stub configurations.
     * Runs after Spring context initialization.
     */
    @PostConstruct
    public void startMockServers() {
        if (tasyMockServer == null) {
            initializeTasyMockServer();
            initializeWhatsAppMockServer();
            initializeNlpMockServer();
            initializeOcrMockServer();
        }
    }

    /**
     * Stops all mock servers gracefully.
     * Runs before Spring context destruction.
     */
    @PreDestroy
    public void stopMockServers() {
        if (tasyMockServer != null) tasyMockServer.stop();
        if (whatsappMockServer != null) whatsappMockServer.stop();
        if (nlpMockServer != null) nlpMockServer.stop();
        if (ocrMockServer != null) ocrMockServer.stop();
    }

    /**
     * Configures Spring properties with mock server URLs.
     *
     * @param registry Spring's dynamic property registry
     */
    @DynamicPropertySource
    static void configureMockServerProperties(DynamicPropertyRegistry registry) {
        if (tasyMockServer != null) {
            registry.add("integration.tasy.base-url",
                () -> "http://localhost:" + tasyMockServer.port());
        }
        if (whatsappMockServer != null) {
            registry.add("integration.whatsapp.base-url",
                () -> "http://localhost:" + whatsappMockServer.port());
        }
        if (nlpMockServer != null) {
            registry.add("integration.nlp.base-url",
                () -> "http://localhost:" + nlpMockServer.port());
        }
        if (ocrMockServer != null) {
            registry.add("integration.ocr.base-url",
                () -> "http://localhost:" + ocrMockServer.port());
        }
    }

    // ========== TASY ERP MOCK SERVER ==========

    private void initializeTasyMockServer() {
        tasyMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .dynamicPort()
                .usingFilesUnderClasspath("wiremock/tasy")
        );
        tasyMockServer.start();
        WireMock.configureFor("localhost", tasyMockServer.port());

        setupTasyDefaultStubs();
    }

    private void setupTasyDefaultStubs() {
        // Mock: Buscar paciente por CPF
        tasyMockServer.stubFor(get(urlPathMatching("/api/v1/patients/cpf/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": "PAT-12345",
                      "cpf": "12345678901",
                      "nome": "João Silva",
                      "dataNascimento": "1990-01-01",
                      "telefone": "11999999999",
                      "email": "joao.silva@email.com",
                      "plano": "PREMIUM",
                      "situacao": "ATIVO"
                    }
                    """)));

        // Mock: Consultar cobertura do plano
        tasyMockServer.stubFor(get(urlPathMatching("/api/v1/coverage/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "procedimentoId": "PROC-001",
                      "coberto": true,
                      "percentualCobertura": 100,
                      "valorCoparticipacao": 0,
                      "restricoes": []
                    }
                    """)));

        // Mock: Criar solicitação de autorização
        tasyMockServer.stubFor(post(urlEqualTo("/api/v1/authorizations"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "authorizationId": "AUTH-12345",
                      "status": "PENDING",
                      "protocolo": "PROT-2024-001",
                      "dataExpiracao": "2024-12-31"
                    }
                    """)));
    }

    // ========== WHATSAPP BUSINESS MOCK SERVER ==========

    private void initializeWhatsAppMockServer() {
        whatsappMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .dynamicPort()
                .usingFilesUnderClasspath("wiremock/whatsapp")
        );
        whatsappMockServer.start();

        setupWhatsAppDefaultStubs();
    }

    private void setupWhatsAppDefaultStubs() {
        // Mock: Enviar mensagem
        whatsappMockServer.stubFor(post(urlEqualTo("/v1/messages"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "messageId": "wamid.HBgLNTU5OTk5OTk5OTkVAgASGBQzQTdDRjg5RjdFNzNCOTQ3RTlFNAA=",
                      "status": "sent"
                    }
                    """)));

        // Mock: Webhook de status de mensagem
        whatsappMockServer.stubFor(post(urlEqualTo("/webhook"))
            .willReturn(aResponse()
                .withStatus(200)));
    }

    // ========== NLP/GPT-4 MOCK SERVER ==========

    private void initializeNlpMockServer() {
        nlpMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .dynamicPort()
                .usingFilesUnderClasspath("wiremock/nlp")
        );
        nlpMockServer.start();

        setupNlpDefaultStubs();
    }

    private void setupNlpDefaultStubs() {
        // Mock: Análise de sentimento
        nlpMockServer.stubFor(post(urlEqualTo("/v1/sentiment"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "sentiment": "POSITIVE",
                      "score": 0.85,
                      "confidence": 0.92
                    }
                    """)));

        // Mock: Classificação de intenção
        nlpMockServer.stubFor(post(urlEqualTo("/v1/intent"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "intent": "SOLICITAR_CONSULTA",
                      "confidence": 0.89,
                      "entities": [
                        {"type": "ESPECIALIDADE", "value": "cardiologia"},
                        {"type": "URGENCIA", "value": "normal"}
                      ]
                    }
                    """)));

        // Mock: Geração de resposta GPT-4
        nlpMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": "chatcmpl-123",
                      "object": "chat.completion",
                      "created": 1677652288,
                      "model": "gpt-4",
                      "choices": [{
                        "index": 0,
                        "message": {
                          "role": "assistant",
                          "content": "Entendi que você precisa de uma consulta com cardiologista. Posso ajudar a agendar. Qual sua disponibilidade?"
                        },
                        "finish_reason": "stop"
                      }]
                    }
                    """)));
    }

    // ========== OCR/COMPUTER VISION MOCK SERVER ==========

    private void initializeOcrMockServer() {
        ocrMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .dynamicPort()
                .usingFilesUnderClasspath("wiremock/ocr")
        );
        ocrMockServer.start();

        setupOcrDefaultStubs();
    }

    private void setupOcrDefaultStubs() {
        // Mock: Extrair texto de documento
        ocrMockServer.stubFor(post(urlEqualTo("/v1/ocr/extract"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "text": "RECEITA MÉDICA\\nPaciente: João Silva\\nCPF: 123.456.789-01\\nMedicamento: Losartana 50mg\\nPosologia: 1 comprimido ao dia",
                      "confidence": 0.95,
                      "documentType": "MEDICAL_PRESCRIPTION"
                    }
                    """)));

        // Mock: Detectar tipo de documento
        ocrMockServer.stubFor(post(urlEqualTo("/v1/vision/classify"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "documentType": "RG",
                      "confidence": 0.97,
                      "detectedFields": ["numero", "orgaoEmissor", "dataEmissao"]
                    }
                    """)));
    }

    // ========== PUBLIC ACCESSORS ==========

    @Bean
    public WireMockServer tasyMockServer() {
        return tasyMockServer;
    }

    @Bean
    public WireMockServer whatsappMockServer() {
        return whatsappMockServer;
    }

    @Bean
    public WireMockServer nlpMockServer() {
        return nlpMockServer;
    }

    @Bean
    public WireMockServer ocrMockServer() {
        return ocrMockServer;
    }

    /**
     * Reset all mock servers to default state.
     * Useful between test methods to ensure clean state.
     */
    public static void resetAllMockServers() {
        if (tasyMockServer != null) tasyMockServer.resetAll();
        if (whatsappMockServer != null) whatsappMockServer.resetAll();
        if (nlpMockServer != null) nlpMockServer.resetAll();
        if (ocrMockServer != null) ocrMockServer.resetAll();
    }

    /**
     * Get status of all mock servers.
     *
     * @return formatted status string
     */
    public static String getMockServersStatus() {
        return String.format(
            "Mock Servers Status:\n" +
            "  Tasy ERP: %s (Port: %d)\n" +
            "  WhatsApp: %s (Port: %d)\n" +
            "  NLP/GPT-4: %s (Port: %d)\n" +
            "  OCR/Vision: %s (Port: %d)",
            tasyMockServer != null && tasyMockServer.isRunning() ? "RUNNING" : "STOPPED",
            tasyMockServer != null ? tasyMockServer.port() : 0,
            whatsappMockServer != null && whatsappMockServer.isRunning() ? "RUNNING" : "STOPPED",
            whatsappMockServer != null ? whatsappMockServer.port() : 0,
            nlpMockServer != null && nlpMockServer.isRunning() ? "RUNNING" : "STOPPED",
            nlpMockServer != null ? nlpMockServer.port() : 0,
            ocrMockServer != null && ocrMockServer.isRunning() ? "RUNNING" : "STOPPED",
            ocrMockServer != null ? ocrMockServer.port() : 0
        );
    }
}
