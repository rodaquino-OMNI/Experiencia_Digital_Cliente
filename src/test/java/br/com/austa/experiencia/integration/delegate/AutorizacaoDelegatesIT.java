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
 * Integration Tests for SUB-006 Authorization Management Delegates
 *
 * Tests all 9 delegates:
 * 1. ValidarSolicitacaoDelegate
 * 2. ConsultarElegibilidadeDelegate
 * 3. ClassificarComplexidadeDelegate
 * 4. AprovarAutomaticamenteDelegate
 * 5. RotearAuditoriaDelegate
 * 6. IntegrarTasyDelegate
 * 7. GerarGuiaDelegate
 * 8. NotificarPartesDelegate
 * 9. RegistrarDecisaoDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 1024-1123
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-006 Authorization Management")
public class AutorizacaoDelegatesIT extends BaseIntegrationTest {

    @Autowired
    private AutorizacaoRepository autorizacaoRepository;

    @Test
    @DisplayName("ValidarSolicitacaoDelegate should validate authorization request")
    void shouldValidateRequest() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("procedimento")).thenReturn("CONSULTA");
        when(execution.getVariable("prestadorId")).thenReturn("PREST001");

        delegateRunner.execute("ValidarSolicitacaoDelegate", execution);

        verify(execution).setVariable(eq("solicitacaoValida"), eq(true));
    }

    @Test
    @DisplayName("ConsultarElegibilidadeDelegate should check coverage")
    void shouldCheckEligibility() throws Exception {
        stubFor(post(urlEqualTo("/api/tasy/elegibilidade/verificar"))
            .willReturn(aOk().withBody("{\"coberto\": true, \"coparticipacao\": 50.0}")));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("procedimento")).thenReturn("CONSULTA");

        delegateRunner.execute("ConsultarElegibilidadeDelegate", execution);

        verify(execution).setVariable(eq("coberto"), eq(true));
        verify(execution).setVariable(eq("coparticipacao"), eq(50.0));
    }

    @Test
    @DisplayName("ClassificarComplexidadeDelegate should classify complexity")
    void shouldClassifyComplexity() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("valorEstimado")).thenReturn(45000.0);
        when(execution.getVariable("tipoSolicitacao")).thenReturn("CIRURGIA");

        delegateRunner.execute("ClassificarComplexidadeDelegate", execution);

        verify(execution).setVariable(eq("complexidade"), eq("ALTA"));
        verify(execution).setVariable(eq("requererAuditoria"), eq(true));
    }

    @Test
    @DisplayName("AprovarAutomaticamenteDelegate should auto-approve low value")
    void shouldAutoApprove() throws Exception {
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/validar"))
            .willReturn(aOk().withBody("{\"aprovada\": true, \"numeroGuia\": \"GUIA123\"}")));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("valorEstimado")).thenReturn(250.0);
        when(execution.getVariable("complexidade")).thenReturn("BAIXA");

        delegateRunner.execute("AprovarAutomaticamenteDelegate", execution);

        verify(execution).setVariable(eq("aprovada"), eq(true));
        verify(execution).setVariable(eq("tipoAprovacao"), eq("AUTOMATICA"));
    }

    @Test
    @DisplayName("RotearAuditoriaDelegate should route to auditor")
    void shouldRouteToAuditor() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("complexidade")).thenReturn("ALTA");
        when(execution.getVariable("especialidade")).thenReturn("CARDIOLOGIA");

        delegateRunner.execute("RotearAuditoriaDelegate", execution);

        verify(execution).setVariable(eq("auditorDesignado"), anyString());
        verify(execution).setVariable(eq("prazoAnalise"), any());
    }

    @Test
    @DisplayName("IntegrarTasyDelegate should integrate with Tasy ERP")
    void shouldIntegrateWithTasy() throws Exception {
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/criar"))
            .willReturn(aOk().withBody("{\"id\": \"TASY123\", \"status\": \"CRIADA\"}")));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("autorizacaoId")).thenReturn("AUT123");

        delegateRunner.execute("IntegrarTasyDelegate", execution);

        verify(1, postRequestedFor(urlEqualTo("/api/tasy/autorizacao/criar")));
        verify(execution).setVariable(eq("tasyId"), eq("TASY123"));
    }

    @Test
    @DisplayName("GerarGuiaDelegate should generate authorization guide")
    void shouldGenerateGuide() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("autorizacaoAprovada")).thenReturn(true);
        when(execution.getVariable("procedimento")).thenReturn("CONSULTA");

        delegateRunner.execute("GerarGuiaDelegate", execution);

        verify(execution).setVariable(eq("numeroGuia"), anyString());
        verify(execution).setVariable(eq("guiaUrl"), anyString());
    }

    @Test
    @DisplayName("NotificarPartesDelegate should notify all parties")
    void shouldNotifyParties() throws Exception {
        stubFor(post(urlEqualTo("/api/sms/send")).willReturn(aOk()));
        stubFor(post(urlEqualTo("/api/email/send")).willReturn(aOk()));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioTelefone")).thenReturn("+5511999999999");
        when(execution.getVariable("prestadorEmail")).thenReturn("prestador@email.com");
        when(execution.getVariable("statusAutorizacao")).thenReturn("APROVADA");

        delegateRunner.execute("NotificarPartesDelegate", execution);

        verify(atLeast(1), postRequestedFor(urlEqualTo("/api/sms/send")));
        verify(atLeast(1), postRequestedFor(urlEqualTo("/api/email/send")));
    }

    @Test
    @DisplayName("RegistrarDecisaoDelegate should persist decision")
    void shouldRegisterDecision() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("autorizacaoId")).thenReturn("AUT123");
        when(execution.getVariable("decisao")).thenReturn("APROVADA");
        when(execution.getVariable("justificativa")).thenReturn("Procedimento coberto");

        delegateRunner.execute("RegistrarDecisaoDelegate", execution);

        verify(execution).setVariable(eq("decisaoRegistrada"), eq(true));
        verify(execution).setVariable(eq("timestamp"), anyLong());
    }
}
