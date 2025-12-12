package br.com.austa.experiencia.integration.delegate;

import br.com.austa.experiencia.BaseIntegrationTest;
import br.com.austa.experiencia.builder.TestDataBuilder;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for SUB-004 Self-Service Delegates
 *
 * Tests all 5 delegates:
 * 1. AutenticarBeneficiarioDelegate
 * 2. ValidarElegibilidadeDelegate
 * 3. ProcessarSolicitacaoDelegate
 * 4. GerarComprovanteDelegate
 * 5. EnviarConfirmacaoDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 872-927
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-004 Self-Service")
public class SelfServiceDelegatesIT extends BaseIntegrationTest {

    @Test
    @DisplayName("AutenticarBeneficiarioDelegate should authenticate user")
    void shouldAuthenticateUser() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("cpf")).thenReturn("12345678901");
        when(execution.getVariable("senha")).thenReturn("hash123");

        delegateRunner.execute("AutenticarBeneficiarioDelegate", execution);

        verify(execution).setVariable(eq("autenticado"), eq(true));
        verify(execution).setVariable(eq("beneficiarioId"), anyString());
    }

    @Test
    @DisplayName("ValidarElegibilidadeDelegate should check service eligibility")
    void shouldValidateEligibility() throws Exception {
        stubFor(get(urlMatching("/api/tasy/elegibilidade/.*"))
            .willReturn(aOk().withBody("{\"elegivel\": true, \"cobertura\": \"COMPLETA\"}")));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("servicoSolicitado")).thenReturn("SEGUNDA_VIA_CARTEIRINHA");

        delegateRunner.execute("ValidarElegibilidadeDelegate", execution);

        verify(execution).setVariable(eq("elegivel"), eq(true));
    }

    @Test
    @DisplayName("ProcessarSolicitacaoDelegate should process request")
    void shouldProcessRequest() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("tipoSolicitacao")).thenReturn("DECLARACAO_PLANO");
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");

        delegateRunner.execute("ProcessarSolicitacaoDelegate", execution);

        verify(execution).setVariable(eq("solicitacaoProcessada"), eq(true));
        verify(execution).setVariable(eq("protocolo"), anyString());
    }

    @Test
    @DisplayName("GerarComprovanteDelegate should generate proof document")
    void shouldGenerateProof() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("tipoDocumento")).thenReturn("DECLARACAO");
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");

        delegateRunner.execute("GerarComprovanteDelegate", execution);

        verify(execution).setVariable(eq("documentoUrl"), anyString());
        verify(execution).setVariable(eq("formato"), eq("PDF"));
    }

    @Test
    @DisplayName("EnviarConfirmacaoDelegate should send confirmation")
    void shouldSendConfirmation() throws Exception {
        stubFor(post(urlEqualTo("/api/email/send"))
            .willReturn(aOk().withBody("{\"sent\": true}")));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("email")).thenReturn("user@email.com");
        when(execution.getVariable("protocolo")).thenReturn("PROT123");

        delegateRunner.execute("EnviarConfirmacaoDelegate", execution);

        verify(1, postRequestedFor(urlEqualTo("/api/email/send")));
        verify(execution).setVariable(eq("confirmacaoEnviada"), eq(true));
    }
}
