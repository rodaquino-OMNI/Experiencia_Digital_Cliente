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
 * Integration Tests for SUB-003 Reception and Triage Delegates
 *
 * Tests all 6 delegates from SUB-003-Recepcao-Triagem:
 * 1. ReceberInteracaoDelegate
 * 2. ClassificarIntencaoDelegate
 * 3. DeterminarUrgenciaDelegate
 * 4. RotearAtendimentoDelegate
 * 5. RegistrarInteracaoDelegate
 * 6. NotificarEquipeDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 798-871
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-003 Reception and Triage")
public class RecepcaoDelegatesIT extends BaseIntegrationTest {

    @Autowired
    private InteracaoRepository interacaoRepository;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private FilaAtendimentoRepository filaAtendimentoRepository;

    @Test
    @DisplayName("ReceberInteracaoDelegate should receive and validate interaction")
    void shouldReceiveInteraction() throws Exception {
        // Given: Incoming interaction
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("canal")).thenReturn("WHATSAPP");
        when(execution.getVariable("remetente")).thenReturn("+5511999999999");
        when(execution.getVariable("mensagem")).thenReturn("Preciso de ajuda");

        // When: Receive interaction
        delegateRunner.execute("ReceberInteracaoDelegate", execution);

        // Then: Interaction validated
        verify(execution).setVariable(eq("interacaoValida"), eq(true));
        verify(execution).setVariable(eq("timestamp"), anyLong());
    }

    @Test
    @DisplayName("ClassificarIntencaoDelegate should classify with NLP")
    void shouldClassifyIntent() throws Exception {
        // Given: Message to classify
        stubFor(post(urlEqualTo("/api/nlp/classify"))
            .willReturn(aOk().withBody(
                "{\"intent\": \"SOLICITAR_AUTORIZACAO\", \"confidence\": 0.92, \"entities\": []}"
            )));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("mensagem")).thenReturn("Preciso autorização para consulta");

        // When: Classify intent
        delegateRunner.execute("ClassificarIntencaoDelegate", execution);

        // Then: Intent classified
        verify(execution).setVariable(eq("intencao"), eq("SOLICITAR_AUTORIZACAO"));
        verify(execution).setVariable(eq("confianca"), eq(0.92));
    }

    @Test
    @DisplayName("DeterminarUrgenciaDelegate should determine priority")
    void shouldDetermineUrgency() throws Exception {
        // Given: Emergency keywords
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("mensagem")).thenReturn("Dor no peito muito forte");
        when(execution.getVariable("intencao")).thenReturn("SINTOMA_URGENTE");

        // When: Determine urgency
        delegateRunner.execute("DeterminarUrgenciaDelegate", execution);

        // Then: High urgency detected
        verify(execution).setVariable(eq("urgencia"), eq("ALTA"));
        verify(execution).setVariable(eq("prioridade"), eq(10));
    }

    @Test
    @DisplayName("RotearAtendimentoDelegate should route to appropriate queue")
    void shouldRouteToQueue() throws Exception {
        // Given: Classified interaction
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("intencao")).thenReturn("SOLICITAR_AUTORIZACAO");
        when(execution.getVariable("urgencia")).thenReturn("NORMAL");

        // When: Route interaction
        delegateRunner.execute("RotearAtendimentoDelegate", execution);

        // Then: Routed to authorization queue
        verify(execution).setVariable(eq("fila"), eq("AUTORIZACAO"));
        verify(execution).setVariable(eq("equipeResponsavel"), eq("AUTORIZACAO_TEAM"));
    }

    @Test
    @DisplayName("RegistrarInteracaoDelegate should persist interaction")
    void shouldRegisterInteraction() throws Exception {
        // Given: Interaction data
        Beneficiario ben = beneficiarioRepository.save(
            TestDataBuilder.buildBeneficiario().cpf("123").build()
        );

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiarioId")).thenReturn(ben.getId());
        when(execution.getVariable("canal")).thenReturn("WHATSAPP");
        when(execution.getVariable("mensagem")).thenReturn("Test message");
        when(execution.getVariable("intencao")).thenReturn("INFO");

        // When: Register interaction
        delegateRunner.execute("RegistrarInteracaoDelegate", execution);

        // Then: Interaction persisted
        Interacao interacao = interacaoRepository.findByBeneficiarioId(ben.getId()).get(0);
        assertThat(interacao.getCanal()).isEqualTo(CanalAtendimento.WHATSAPP);
        assertThat(interacao.getIntencao()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("NotificarEquipeDelegate should notify responsible team")
    void shouldNotifyTeam() throws Exception {
        // Given: High priority interaction
        stubFor(post(urlEqualTo("/api/notifications/team"))
            .willReturn(aOk()));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("equipeResponsavel")).thenReturn("AUTORIZACAO_TEAM");
        when(execution.getVariable("urgencia")).thenReturn("ALTA");
        when(execution.getVariable("interacaoId")).thenReturn("INT123");

        // When: Notify team
        delegateRunner.execute("NotificarEquipeDelegate", execution);

        // Then: Team notified
        verify(1, postRequestedFor(urlEqualTo("/api/notifications/team")));
        verify(execution).setVariable(eq("notificacaoEnviada"), eq(true));
    }
}
