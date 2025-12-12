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
 * Integration Tests for SUB-008 Chronic Disease Management Delegates
 *
 * Tests all 6 delegates:
 * 1. AvaliarElegibilidadeDelegate
 * 2. DefinirMetasDelegate
 * 3. MonitorarIndicadoresDelegate
 * 4. IntervirDelegate
 * 5. AvaliarProgressoDelegate
 * 6. AjustarPlanoDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 1214-1278
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-008 Chronic Disease Management")
public class CronicosDelegatesIT extends BaseIntegrationTest {

    @Autowired
    private ProgramaCronicoRepository programaCronicoRepository;

    @Autowired
    private MetaSaudeRepository metaSaudeRepository;

    @Test
    @DisplayName("AvaliarElegibilidadeDelegate should assess program eligibility")
    void shouldAssessEligibility() throws Exception {
        stubFor(get(urlMatching("/api/tasy/beneficiario/.*/historico-clinico"))
            .willReturn(aOk().withBody(
                "{\"diagnosticos\": [\"E11\"], \"internacoes\": 2}"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("condicao")).thenReturn("DIABETES_TIPO2");

        delegateRunner.execute("AvaliarElegibilidadeDelegate", execution);

        verify(execution).setVariable(eq("elegivel"), eq(true));
        verify(execution).setVariable(eq("criteriosAtendidos"), anyList());
    }

    @Test
    @DisplayName("DefinirMetasDelegate should define health goals")
    void shouldDefineGoals() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("condicao")).thenReturn("DIABETES_TIPO2");
        when(execution.getVariable("hba1cAtual")).thenReturn(8.5);

        delegateRunner.execute("DefinirMetasDelegate", execution);

        verify(execution).setVariable(eq("metasDefinidas"), anyList());
        verify(execution).setVariable(eq("prazoMonitoramento"), anyString());
    }

    @Test
    @DisplayName("MonitorarIndicadoresDelegate should track health indicators")
    void shouldMonitorIndicators() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("tipo")).thenReturn("GLICEMIA_JEJUM");
        when(execution.getVariable("valor")).thenReturn(145.0);

        delegateRunner.execute("MonitorarIndicadoresDelegate", execution);

        verify(execution).setVariable(eq("indicadorRegistrado"), eq(true));
        verify(execution).setVariable(eq("dentroMeta"), anyBoolean());
    }

    @Test
    @DisplayName("IntervirDelegate should trigger intervention when needed")
    void shouldIntervene() throws Exception {
        stubFor(post(urlEqualTo("/api/notifications/team"))
            .willReturn(aOk()));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("alertaTipo")).thenReturn("GLICEMIA_ALTA");
        when(execution.getVariable("valor")).thenReturn(210.0);

        delegateRunner.execute("IntervirDelegate", execution);

        verify(execution).setVariable(eq("intervencaoNecessaria"), eq(true));
        verify(execution).setVariable(eq("tipoIntervencao"), anyString());
    }

    @Test
    @DisplayName("AvaliarProgressoDelegate should evaluate patient progress")
    void shouldEvaluateProgress() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("programaId")).thenReturn("PROG123");
        when(execution.getVariable("hba1cInicial")).thenReturn(8.5);
        when(execution.getVariable("hba1cAtual")).thenReturn(7.2);

        delegateRunner.execute("AvaliarProgressoDelegate", execution);

        verify(execution).setVariable(eq("progressoPositivo"), eq(true));
        verify(execution).setVariable(eq("percentualMelhora"), anyDouble());
    }

    @Test
    @DisplayName("AjustarPlanoDelegate should adjust care plan")
    void shouldAdjustPlan() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("programaId")).thenReturn("PROG123");
        when(execution.getVariable("progressoInsuficiente")).thenReturn(true);

        delegateRunner.execute("AjustarPlanoDelegate", execution);

        verify(execution).setVariable(eq("planoAjustado"), eq(true));
        verify(execution).setVariable(eq("novasIntervencoes"), anyList());
    }
}
