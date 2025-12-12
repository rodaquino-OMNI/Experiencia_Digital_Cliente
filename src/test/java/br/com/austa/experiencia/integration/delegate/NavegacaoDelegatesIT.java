package br.com.austa.experiencia.integration.delegate;

import br.com.austa.experiencia.BaseIntegrationTest;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for SUB-007 Navigation and Guidance Delegates
 *
 * Tests all 8 delegates:
 * 1. IdentificarNecessidadeDelegate
 * 2. LocalizarPrestadorDelegate
 * 3. VerificarDisponibilidadeDelegate
 * 4. AgendarConsultaDelegate
 * 5. EnviarLembreteDelegate
 * 6. GerarRotaDelegate
 * 7. FornecerOrientacoesDelegate
 * 8. ConfirmarComparecimentoDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 1124-1213
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-007 Navigation and Guidance")
public class NavegacaoDelegatesIT extends BaseIntegrationTest {

    @Test
    @DisplayName("IdentificarNecessidadeDelegate should identify patient need")
    void shouldIdentifyNeed() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("sintomas")).thenReturn(java.util.List.of("dor", "febre"));

        delegateRunner.execute("IdentificarNecessidadeDelegate", execution);

        verify(execution).setVariable(eq("especialidadeRecomendada"), anyString());
    }

    @Test
    @DisplayName("LocalizarPrestadorDelegate should find nearby providers")
    void shouldFindProviders() throws Exception {
        stubFor(get(urlMatching("/api/prestadores/buscar.*"))
            .willReturn(aOk().withBody(
                "[{\"id\": \"PREST001\", \"nome\": \"Hospital A\", \"distancia\": 2.5}]"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("especialidade")).thenReturn("CARDIOLOGIA");
        when(execution.getVariable("latitude")).thenReturn(-23.550520);
        when(execution.getVariable("longitude")).thenReturn(-46.633308);

        delegateRunner.execute("LocalizarPrestadorDelegate", execution);

        verify(execution).setVariable(eq("prestadoresEncontrados"), anyList());
    }

    @Test
    @DisplayName("VerificarDisponibilidadeDelegate should check availability")
    void shouldCheckAvailability() throws Exception {
        stubFor(get(urlMatching("/api/agenda/.*"))
            .willReturn(aOk().withBody(
                "{\"horarios\": [\"09:00\", \"14:00\", \"16:30\"]}"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("prestadorId")).thenReturn("PREST001");
        when(execution.getVariable("data")).thenReturn("2025-12-15");

        delegateRunner.execute("VerificarDisponibilidadeDelegate", execution);

        verify(execution).setVariable(eq("horariosDisponiveis"), anyList());
    }

    @Test
    @DisplayName("AgendarConsultaDelegate should schedule appointment")
    void shouldScheduleAppointment() throws Exception {
        stubFor(post(urlEqualTo("/api/agenda/agendar"))
            .willReturn(aOk().withBody("{\"confirmado\": true, \"protocolo\": \"AGD123\"}")));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("prestadorId")).thenReturn("PREST001");
        when(execution.getVariable("dataHora")).thenReturn("2025-12-15T09:00");

        delegateRunner.execute("AgendarConsultaDelegate", execution);

        verify(execution).setVariable(eq("agendado"), eq(true));
        verify(execution).setVariable(eq("protocolo"), eq("AGD123"));
    }

    @Test
    @DisplayName("EnviarLembreteDelegate should send reminder")
    void shouldSendReminder() throws Exception {
        stubFor(post(urlEqualTo("/api/sms/send")).willReturn(aOk()));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("telefone")).thenReturn("+5511999999999");
        when(execution.getVariable("dataConsulta")).thenReturn("2025-12-15T09:00");

        delegateRunner.execute("EnviarLembreteDelegate", execution);

        verify(1, postRequestedFor(urlEqualTo("/api/sms/send")));
    }

    @Test
    @DisplayName("GerarRotaDelegate should generate route")
    void shouldGenerateRoute() throws Exception {
        stubFor(get(urlMatching("/api/maps/route.*"))
            .willReturn(aOk().withBody(
                "{\"distancia\": 2500, \"tempo\": 15, \"instrucoes\": []}"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("origem")).thenReturn("-23.550520,-46.633308");
        when(execution.getVariable("destino")).thenReturn("-23.560520,-46.643308");

        delegateRunner.execute("GerarRotaDelegate", execution);

        verify(execution).setVariable(eq("rota"), anyMap());
    }

    @Test
    @DisplayName("FornecerOrientacoesDelegate should provide guidance")
    void shouldProvideGuidance() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("tipoConsulta")).thenReturn("EXAME_SANGUE");

        delegateRunner.execute("FornecerOrientacoesDelegate", execution);

        verify(execution).setVariable(eq("orientacoes"), anyList());
        verify(execution).setVariable(eq("preparo"), anyString());
    }

    @Test
    @DisplayName("ConfirmarComparecimentoDelegate should confirm attendance")
    void shouldConfirmAttendance() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("agendamentoId")).thenReturn("AGD123");
        when(execution.getVariable("compareceu")).thenReturn(true);

        delegateRunner.execute("ConfirmarComparecimentoDelegate", execution);

        verify(execution).setVariable(eq("confirmado"), eq(true));
        verify(execution).setVariable(eq("timestampConfirmacao"), anyLong());
    }
}
