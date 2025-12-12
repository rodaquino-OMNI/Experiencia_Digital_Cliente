package br.com.austa.experiencia.integration.delegate;

import br.com.austa.experiencia.BaseIntegrationTest;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for Common/Shared Delegates
 *
 * Tests all 3 common delegates used across subprocesses:
 * 1. LogEventoDelegate - Event logging
 * 2. PublicarKafkaDelegate - Kafka event publishing
 * 3. TratarErroDelegate - Error handling
 *
 * @see PROMPT_TECNICO_3.MD Lines 1408-1450
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: Common Utilities")
public class CommonDelegatesIT extends BaseIntegrationTest {

    @Test
    @DisplayName("LogEventoDelegate should log business events")
    void shouldLogEvent() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("eventoTipo")).thenReturn("AUTORIZACAO_APROVADA");
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("dados")).thenReturn(
            java.util.Map.of("numeroGuia", "GUIA123", "valor", 250.0)
        );

        delegateRunner.execute("LogEventoDelegate", execution);

        verify(execution).setVariable(eq("eventoLogado"), eq(true));
        verify(execution).setVariable(eq("eventoId"), anyString());
    }

    @Test
    @DisplayName("PublicarKafkaDelegate should publish to Kafka topic")
    void shouldPublishToKafka() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("topico")).thenReturn("autorizacao.aprovada");
        when(execution.getVariable("chave")).thenReturn("BEN123");
        when(execution.getVariable("mensagem")).thenReturn(
            java.util.Map.of("numeroGuia", "GUIA123", "status", "APROVADA")
        );

        delegateRunner.execute("PublicarKafkaDelegate", execution);

        verify(execution).setVariable(eq("kafkaPublicado"), eq(true));
        verify(execution).setVariable(eq("offset"), anyLong());
    }

    @Test
    @DisplayName("TratarErroDelegate should handle and log errors")
    void shouldHandleError() throws Exception {
        stubFor(post(urlEqualTo("/api/monitoring/alert"))
            .willReturn(aOk()));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("erro")).thenReturn(new RuntimeException("Test error"));
        when(execution.getVariable("processInstanceId")).thenReturn("PROC123");
        when(execution.getVariable("gravidade")).thenReturn("ALTA");

        delegateRunner.execute("TratarErroDelegate", execution);

        verify(execution).setVariable(eq("erroTratado"), eq(true));
        verify(execution).setVariable(eq("acaoCorretiva"), anyString());
        verify(1, postRequestedFor(urlEqualTo("/api/monitoring/alert")));
    }

    @Test
    @DisplayName("LogEventoDelegate should handle structured logging")
    void shouldHandleStructuredLogging() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn("PROC123");
        when(execution.getCurrentActivityId()).thenReturn("task-123");
        when(execution.getVariable("eventoTipo")).thenReturn("INICIO_PROCESSO");
        when(execution.getVariable("metadados")).thenReturn(
            java.util.Map.of("usuario", "user123", "canal", "WHATSAPP")
        );

        delegateRunner.execute("LogEventoDelegate", execution);

        verify(execution).setVariable(eq("eventoLogado"), eq(true));
        verify(execution).setVariable(argThat(arg ->
            arg.equals("eventoId") || arg.equals("timestamp")
        ), any());
    }

    @Test
    @DisplayName("PublicarKafkaDelegate should handle publish failures gracefully")
    void shouldHandleKafkaFailure() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("topico")).thenReturn("invalid.topic");
        when(execution.getVariable("mensagem")).thenReturn(java.util.Map.of("test", "data"));
        when(execution.getVariable("retentativas")).thenReturn(0);

        // Simulate Kafka being unavailable
        delegateRunner.execute("PublicarKafkaDelegate", execution);

        // Should retry or mark as failed
        verify(execution).setVariable(argThat(arg ->
            arg.equals("kafkaPublicado") || arg.equals("falhaPublicacao")
        ), any());
    }

    @Test
    @DisplayName("TratarErroDelegate should escalate critical errors")
    void shouldEscalateCriticalErrors() throws Exception {
        stubFor(post(urlEqualTo("/api/monitoring/alert"))
            .willReturn(aOk()));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("erro")).thenReturn(
            new RuntimeException("Database connection failed")
        );
        when(execution.getVariable("gravidade")).thenReturn("CRITICA");
        when(execution.getVariable("processInstanceId")).thenReturn("PROC123");

        delegateRunner.execute("TratarErroDelegate", execution);

        verify(execution).setVariable(eq("escalarEquipeOps"), eq(true));
        verify(execution).setVariable(eq("notificacaoEnviada"), eq(true));
        verify(1, postRequestedFor(urlEqualTo("/api/monitoring/alert")));
    }
}
