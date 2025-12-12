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
 * Integration Tests for SUB-009 Complaint Management Delegates
 *
 * Tests all 7 delegates:
 * 1. RegistrarReclamacaoDelegate
 * 2. ClassificarGravidadeDelegate
 * 3. AnalisarCausaRaizDelegate
 * 4. ImplementarSolucaoDelegate
 * 5. ValidarResolucaoDelegate
 * 6. EscalarANSDelegate
 * 7. GerarRelatorioDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 1279-1353
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-009 Complaint Management")
public class ReclamacoesDelegatesIT extends BaseIntegrationTest {

    @Autowired
    private ReclamacaoRepository reclamacaoRepository;

    @Autowired
    private CompensacaoRepository compensacaoRepository;

    @Test
    @DisplayName("RegistrarReclamacaoDelegate should register complaint")
    void shouldRegisterComplaint() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn("BEN123");
        when(execution.getVariable("categoria")).thenReturn("ATENDIMENTO");
        when(execution.getVariable("descricao")).thenReturn("Complaint description");

        delegateRunner.execute("RegistrarReclamacaoDelegate", execution);

        verify(execution).setVariable(eq("reclamacaoRegistrada"), eq(true));
        verify(execution).setVariable(eq("protocolo"), anyString());
    }

    @Test
    @DisplayName("ClassificarGravidadeDelegate should classify severity")
    void shouldClassifySeverity() throws Exception {
        stubFor(post(urlEqualTo("/api/nlp/classify-complaint"))
            .willReturn(aOk().withBody(
                "{\"gravidade\": \"ALTA\", \"sentimento\": \"NEGATIVO\"}"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("descricao")).thenReturn("Very serious issue");
        when(execution.getVariable("categoria")).thenReturn("NEGATIVA_AUTORIZACAO");

        delegateRunner.execute("ClassificarGravidadeDelegate", execution);

        verify(execution).setVariable(eq("gravidade"), eq("ALTA"));
        verify(execution).setVariable(eq("prioridade"), anyInt());
    }

    @Test
    @DisplayName("AnalisarCausaRaizDelegate should analyze root cause")
    void shouldAnalyzeRootCause() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("reclamacaoId")).thenReturn("REC123");
        when(execution.getVariable("categoria")).thenReturn("ATENDIMENTO");

        delegateRunner.execute("AnalisarCausaRaizDelegate", execution);

        verify(execution).setVariable(eq("causaRaiz"), anyString());
        verify(execution).setVariable(eq("departamentoResponsavel"), anyString());
    }

    @Test
    @DisplayName("ImplementarSolucaoDelegate should implement solution")
    void shouldImplementSolution() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("reclamacaoId")).thenReturn("REC123");
        when(execution.getVariable("causaRaiz")).thenReturn("FALTA_AGENTES");
        when(execution.getVariable("acoes")).thenReturn(java.util.List.of("action1", "action2"));

        delegateRunner.execute("ImplementarSolucaoDelegate", execution);

        verify(execution).setVariable(eq("solucaoImplementada"), eq(true));
        verify(execution).setVariable(eq("dataImplementacao"), anyLong());
    }

    @Test
    @DisplayName("ValidarResolucaoDelegate should validate resolution")
    void shouldValidateResolution() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("reclamacaoId")).thenReturn("REC123");
        when(execution.getVariable("satisfacaoBeneficiario")).thenReturn(4);

        delegateRunner.execute("ValidarResolucaoDelegate", execution);

        verify(execution).setVariable(eq("resolvida"), eq(true));
        verify(execution).setVariable(eq("dataResolucao"), anyLong());
    }

    @Test
    @DisplayName("EscalarANSDelegate should escalate to ANS when needed")
    void shouldEscalateToANS() throws Exception {
        stubFor(post(urlEqualTo("/api/ans/reclamacao/registrar"))
            .willReturn(aOk().withBody(
                "{\"protocoloANS\": \"ANS123456\", \"prazoResposta\": \"2025-12-31\"}"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("gravidade")).thenReturn("ALTA");
        when(execution.getVariable("tempoSemResolucao")).thenReturn(8);

        delegateRunner.execute("EscalarANSDelegate", execution);

        verify(execution).setVariable(eq("escaladoANS"), eq(true));
        verify(execution).setVariable(eq("protocoloANS"), eq("ANS123456"));
    }

    @Test
    @DisplayName("GerarRelatorioDelegate should generate complaint report")
    void shouldGenerateReport() throws Exception {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("reclamacaoId")).thenReturn("REC123");
        when(execution.getVariable("periodo")).thenReturn("MENSAL");

        delegateRunner.execute("GerarRelatorioDelegate", execution);

        verify(execution).setVariable(eq("relatorioGerado"), eq(true));
        verify(execution).setVariable(eq("relatorioUrl"), anyString());
    }
}
