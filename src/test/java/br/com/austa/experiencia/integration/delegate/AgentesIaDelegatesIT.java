package br.com.austa.experiencia.integration.delegate;

import br.com.austa.experiencia.BaseIntegrationTest;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for SUB-005 AI Agents Delegates
 *
 * Tests all 7 delegates:
 * 1. AnalisarContextoDelegate
 * 2. GerarRespostaIADelegate
 * 3. ValidarRespostaDelegate
 * 4. ExecutarAcaoDelegate
 * 5. AprenderInteracaoDelegate
 * 6. EscalarHumanoDelegate
 * 7. AtualizarKnowledgeBaseDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 928-1023
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-005 AI Agents")
public class AgentesIaDelegatesIT extends BaseIntegrationTest {

    @Test
    @DisplayName("AnalisarContextoDelegate should analyze conversation context")
    void shouldAnalyzeContext() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("historicoConversa")).thenReturn(
            java.util.List.of("Oi", "Preciso de ajuda", "Com autorização")
        );

        delegateRunner.execute("AnalisarContextoDelegate", execution);

        verify(execution).setVariable(eq("contextoAnalisado"), eq(true));
        verify(execution).setVariable(eq("topicoPrincipal"), eq("AUTORIZACAO"));
    }

    @Test
    @DisplayName("GerarRespostaIADelegate should generate AI response")
    void shouldGenerateAIResponse() throws Exception {
        stubFor(post(urlEqualTo("/api/ai/generate"))
            .willReturn(aOk().withBody(
                "{\"response\": \"Posso ajudar com sua autorização...\", \"confidence\": 0.95}"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("mensagemUsuario")).thenReturn("Como solicito autorização?");
        when(execution.getVariable("contexto")).thenReturn("AUTORIZACAO");

        delegateRunner.execute("GerarRespostaIADelegate", execution);

        verify(execution).setVariable(eq("respostaIA"), contains("autorização"));
        verify(execution).setVariable(eq("confianca"), eq(0.95));
    }

    @Test
    @DisplayName("ValidarRespostaDelegate should validate response quality")
    void shouldValidateResponse() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("respostaIA")).thenReturn("Response text");
        when(execution.getVariable("confianca")).thenReturn(0.95);

        delegateRunner.execute("ValidarRespostaDelegate", execution);

        verify(execution).setVariable(eq("respostaValida"), eq(true));
    }

    @Test
    @DisplayName("ExecutarAcaoDelegate should execute automated action")
    void shouldExecuteAction() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("acaoRecomendada")).thenReturn("AGENDAR_CALLBACK");
        when(execution.getVariable("parametros")).thenReturn(
            java.util.Map.of("telefone", "+5511999999999", "horario", "14:00")
        );

        delegateRunner.execute("ExecutarAcaoDelegate", execution);

        verify(execution).setVariable(eq("acaoExecutada"), eq(true));
    }

    @Test
    @DisplayName("AprenderInteracaoDelegate should learn from interaction")
    void shouldLearnFromInteraction() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("interacaoId")).thenReturn("INT123");
        when(execution.getVariable("feedbackPositivo")).thenReturn(true);

        delegateRunner.execute("AprenderInteracaoDelegate", execution);

        verify(execution).setVariable(eq("aprendizadoRegistrado"), eq(true));
    }

    @Test
    @DisplayName("EscalarHumanoDelegate should escalate to human agent")
    void shouldEscalateToHuman() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("motivoEscalacao")).thenReturn("CONFIANCA_BAIXA");
        when(execution.getVariable("confianca")).thenReturn(0.45);

        delegateRunner.execute("EscalarHumanoDelegate", execution);

        verify(execution).setVariable(eq("escalado"), eq(true));
        verify(execution).setVariable(eq("filaHumana"), eq("ATENDIMENTO"));
    }

    @Test
    @DisplayName("AtualizarKnowledgeBaseDelegate should update knowledge base")
    void shouldUpdateKnowledgeBase() throws Exception {
        stubFor(post(urlEqualTo("/api/kb/update"))
            .willReturn(aOk()));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("novoConhecimento")).thenReturn("FAQ entry");

        delegateRunner.execute("AtualizarKnowledgeBaseDelegate", execution);

        verify(1, postRequestedFor(urlEqualTo("/api/kb/update")));
        verify(execution).setVariable(eq("kbAtualizada"), eq(true));
    }
}
