package br.com.austa.experiencia.integration.delegate;

import br.com.austa.experiencia.BaseIntegrationTest;
import br.com.austa.experiencia.builder.TestDataBuilder;
import br.com.austa.experiencia.model.*;
import br.com.austa.experiencia.repository.*;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration Tests for SUB-002 Proactive Campaign Delegates
 *
 * Tests all 7 delegates from SUB-002-Proativo-Campanhas:
 * 1. SegmentarBeneficiariosDelegate
 * 2. GerarMensagensPersonalizadasDelegate
 * 3. AgendarEnvioDelegate
 * 4. EnviarNotificacaoDelegate
 * 5. RastrearEngajamentoDelegate
 * 6. AjustarCampanhaDelegate
 * 7. GerarRelatorioEfetividadeDelegate
 *
 * @see PROMPT_TECNICO_3.MD Lines 729-797
 */
@ActiveProfiles("test")
@DisplayName("Delegate Tests: SUB-002 Proactive Campaigns")
public class ProativoDelegatesIT extends BaseIntegrationTest {

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private CampanhaRepository campanhaRepository;

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private EngajamentoRepository engajamentoRepository;

    @Test
    @DisplayName("SegmentarBeneficiariosDelegate should segment by age and health conditions")
    void shouldSegmentBeneficiarios() throws Exception {
        // Given: Various beneficiaries
        beneficiarioRepository.save(TestDataBuilder.buildBeneficiario()
            .cpf("111").idade(45).condicaoCronica("DIABETES_TIPO2").build());
        beneficiarioRepository.save(TestDataBuilder.buildBeneficiario()
            .cpf("222").idade(50).condicaoCronica("HIPERTENSAO").build());
        beneficiarioRepository.save(TestDataBuilder.buildBeneficiario()
            .cpf("333").idade(30).condicaoCronica(null).build());

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("criterioSegmentacao")).thenReturn("CRONICO");
        when(execution.getVariable("idadeMinima")).thenReturn(40);

        // When: Segment beneficiaries
        delegateRunner.execute("SegmentarBeneficiariosDelegate", execution);

        // Then: Should return chronic patients over 40
        verify(execution).setVariable(eq("beneficiariosSegmentados"), argThat(list ->
            ((java.util.List<?>) list).size() == 2
        ));
    }

    @Test
    @DisplayName("GerarMensagensPersonalizadasDelegate should create personalized messages")
    void shouldGeneratePersonalizedMessages() throws Exception {
        // Given: Segmented beneficiaries
        Beneficiario ben1 = beneficiarioRepository.save(
            TestDataBuilder.buildBeneficiario().cpf("111").nome("João").build()
        );

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("beneficiariosSegmentados"))
            .thenReturn(java.util.List.of(ben1.getId()));
        when(execution.getVariable("templateMensagem"))
            .thenReturn("Olá {{nome}}, lembre-se de medir sua glicose hoje!");

        // When: Generate messages
        delegateRunner.execute("GerarMensagensPersonalizadasDelegate", execution);

        // Then: Messages personalized
        verify(execution).setVariable(eq("mensagensGeradas"), argThat(msgs -> {
            java.util.List<String> messages = (java.util.List<String>) msgs;
            return messages.get(0).contains("João");
        }));
    }

    @Test
    @DisplayName("AgendarEnvioDelegate should schedule message delivery")
    void shouldScheduleMessageDelivery() throws Exception {
        // Given: Campaign with schedule
        Campanha campanha = campanhaRepository.save(
            TestDataBuilder.buildCampanha().build()
        );

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("campanhaId")).thenReturn(campanha.getId());
        when(execution.getVariable("horarioEnvio")).thenReturn("09:00");
        when(execution.getVariable("dataEnvio")).thenReturn("2025-12-15");

        // When: Schedule delivery
        delegateRunner.execute("AgendarEnvioDelegate", execution);

        // Then: Schedule created
        verify(execution).setVariable(eq("agendamentoId"), anyString());
        verify(execution).setVariable(eq("status"), eq("AGENDADO"));
    }

    @Test
    @DisplayName("EnviarNotificacaoDelegate should send via multiple channels")
    void shouldSendNotification() throws Exception {
        // Given: Message ready to send
        stubFor(post(urlEqualTo("/api/whatsapp/send"))
            .willReturn(aOk().withBody("{\"messageId\": \"WA123\"}")));
        stubFor(post(urlEqualTo("/api/sms/send"))
            .willReturn(aOk().withBody("{\"messageId\": \"SMS123\"}")));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("canal")).thenReturn("WHATSAPP");
        when(execution.getVariable("destinatario")).thenReturn("+5511999999999");
        when(execution.getVariable("mensagem")).thenReturn("Test message");

        // When: Send notification
        delegateRunner.execute("EnviarNotificacaoDelegate", execution);

        // Then: Notification sent
        verify(1, postRequestedFor(urlEqualTo("/api/whatsapp/send")));
        verify(execution).setVariable(eq("envioStatus"), eq("ENVIADO"));
        verify(execution).setVariable(eq("messageId"), eq("WA123"));
    }

    @Test
    @DisplayName("RastrearEngajamentoDelegate should track message interactions")
    void shouldTrackEngagement() throws Exception {
        // Given: Sent message
        Mensagem mensagem = mensagemRepository.save(
            TestDataBuilder.buildMensagem().status("ENVIADO").build()
        );

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("mensagemId")).thenReturn(mensagem.getId());
        when(execution.getVariable("evento")).thenReturn("VISUALIZADO");
        when(execution.getVariable("timestamp")).thenReturn(System.currentTimeMillis());

        // When: Track engagement
        delegateRunner.execute("RastrearEngajamentoDelegate", execution);

        // Then: Engagement recorded
        Engajamento eng = engajamentoRepository.findByMensagemId(mensagem.getId()).get(0);
        assertThat(eng.getTipo()).isEqualTo("VISUALIZADO");
    }

    @Test
    @DisplayName("AjustarCampanhaDelegate should optimize based on engagement")
    void shouldAdjustCampaign() throws Exception {
        // Given: Campaign with low engagement
        Campanha campanha = campanhaRepository.save(
            TestDataBuilder.buildCampanha().taxaEngajamento(15.0).build()
        );

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("campanhaId")).thenReturn(campanha.getId());
        when(execution.getVariable("taxaEngajamentoAtual")).thenReturn(15.0);

        // When: Adjust campaign
        delegateRunner.execute("AjustarCampanhaDelegate", execution);

        // Then: Campaign adjusted
        verify(execution).setVariable(eq("ajustesRecomendados"), argThat(adj ->
            ((java.util.List<?>) adj).size() > 0
        ));
    }

    @Test
    @DisplayName("GerarRelatorioEfetividadeDelegate should generate campaign report")
    void shouldGenerateEffectivenessReport() throws Exception {
        // Given: Completed campaign
        Campanha campanha = campanhaRepository.save(
            TestDataBuilder.buildCampanha()
                .quantidadeEnviada(1000)
                .quantidadeVisualizada(650)
                .quantidadeRespondida(120)
                .build()
        );

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("campanhaId")).thenReturn(campanha.getId());

        // When: Generate report
        delegateRunner.execute("GerarRelatorioEfetividadeDelegate", execution);

        // Then: Report generated
        verify(execution).setVariable(eq("taxaVisualizacao"), eq(65.0));
        verify(execution).setVariable(eq("taxaResposta"), eq(12.0));
        verify(execution).setVariable(eq("relatorioGerado"), eq(true));
    }
}
