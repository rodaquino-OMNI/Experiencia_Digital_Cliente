package br.com.austa.experiencia.integration.delegate;

import br.com.austa.experiencia.BaseIntegrationTest;
import br.com.austa.experiencia.builder.TestDataBuilder;
import br.com.austa.experiencia.model.*;
import br.com.austa.experiencia.repository.*;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for SUB-010 Follow-up and Survey Delegates
 *
 * Tests all 5 delegates:
 * 1. AgendarFollowUpDelegate
 * 2. RealizarContatoDelegate
 * 3. ColetarSatisfacaoDelegate
 * 4. AnalisarFeedbackDelegate
 * 5. IdentificarMelhoriaDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 1354-1407
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-010 Follow-up and Survey")
public class FollowUpDelegatesIT extends BaseIntegrationTest {

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private SatisfacaoRepository satisfacaoRepository;

    @Test
    @DisplayName("AgendarFollowUpDelegate should schedule follow-up")
    void shouldScheduleFollowUp() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("tipo")).thenReturn("POS_AUTORIZACAO");
        when(execution.getVariable("diasApos")).thenReturn(2);

        delegateRunner.execute("AgendarFollowUpDelegate", execution);

        verify(execution).setVariable(eq("followUpAgendado"), eq(true));
        verify(execution).setVariable(eq("dataFollowUp"), any());
    }

    @Test
    @DisplayName("RealizarContatoDelegate should make contact attempt")
    void shouldMakeContact() throws Exception {
        stubFor(post(urlEqualTo("/api/sms/send"))
            .willReturn(aOk().withBody("{\"sent\": true}")));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("telefone")).thenReturn("+5511999999999");
        when(execution.getVariable("canalPreferencial")).thenReturn("SMS");

        delegateRunner.execute("RealizarContatoDelegate", execution);

        verify(execution).setVariable(eq("contatoRealizado"), eq(true));
        verify(execution).setVariable(eq("timestampContato"), anyLong());
    }

    @Test
    @DisplayName("ColetarSatisfacaoDelegate should collect satisfaction data")
    void shouldCollectSatisfaction() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("notaSatisfacao")).thenReturn(5);
        when(execution.getVariable("comentarios")).thenReturn("Excellent service");

        delegateRunner.execute("ColetarSatisfacaoDelegate", execution);

        verify(execution).setVariable(eq("satisfacaoColetada"), eq(true));
        verify(execution).setVariable(eq("nps"), anyInt());
    }

    @Test
    @DisplayName("AnalisarFeedbackDelegate should analyze feedback")
    void shouldAnalyzeFeedback() throws Exception {
        stubFor(post(urlEqualTo("/api/nlp/sentiment"))
            .willReturn(aOk().withBody(
                "{\"sentiment\": \"POSITIVE\", \"score\": 0.95}"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("comentarios")).thenReturn("Great experience!");

        delegateRunner.execute("AnalisarFeedbackDelegate", execution);

        verify(execution).setVariable(eq("sentimento"), eq("POSITIVE"));
        verify(execution).setVariable(eq("scoreConfianca"), eq(0.95));
    }

    @Test
    @DisplayName("IdentificarMelhoriaDelegate should identify improvements")
    void shouldIdentifyImprovements() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("notaSatisfacao")).thenReturn(2);
        when(execution.getVariable("comentarios")).thenReturn("Long wait times");
        when(execution.getVariable("categoria")).thenReturn("ATENDIMENTO");

        delegateRunner.execute("IdentificarMelhoriaDelegate", execution);

        verify(execution).setVariable(eq("melhoriaIdentificada"), eq(true));
        verify(execution).setVariable(eq("areaMelhoria"), eq("ATENDIMENTO"));
    }
}
