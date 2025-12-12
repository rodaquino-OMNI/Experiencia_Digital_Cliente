package br.com.austa.experiencia.integration.e2e;

import br.com.austa.experiencia.BaseIntegrationTest;
import br.com.austa.experiencia.builder.TestDataBuilder;
import br.com.austa.experiencia.model.*;
import br.com.austa.experiencia.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * E2E Integration Test for NPS (Net Promoter Score) Journey
 *
 * Tests the complete NPS measurement flow:
 * 1. Survey Triggering - Post-interaction survey dispatch
 * 2. Response Collection - Multi-channel survey responses
 * 3. NPS Calculation - Score computation and classification
 * 4. Detractor Recovery - Immediate action for detractors
 * 5. Analytics - Trend analysis and reporting
 *
 * Scenarios:
 * - Promoter journey (score 9-10)
 * - Passive journey (score 7-8)
 * - Detractor journey (score 0-6) with recovery
 * - Multi-touchpoint NPS tracking
 * - Segmented NPS analysis
 *
 * @see PROMPT_TECNICO_3.MD Lines 1170-1210 (SUB-010)
 */
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    "nps.pesquisa_enviada",
    "nps.resposta_recebida",
    "nps.detrator_identificado",
    "nps.recuperacao_iniciada",
    "nps.score_calculado"
})
@DisplayName("E2E: NPS Journey")
public class NpsE2EIT extends BaseIntegrationTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private PesquisaNpsRepository pesquisaNpsRepository;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private InteracaoRepository interacaoRepository;

    @Autowired
    private RecuperacaoRepository recuperacaoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    private Beneficiario beneficiario;
    private Interacao interacao;

    @BeforeEach
    void setupTestData() {
        beneficiario = TestDataBuilder.buildBeneficiario()
            .cpf("12345678901")
            .nome("Maria Santos")
            .codigoExterno("BEN654321")
            .email("maria.santos@example.com")
            .telefone("11987654321")
            .build();
        beneficiarioRepository.save(beneficiario);

        interacao = TestDataBuilder.buildInteracao()
            .beneficiarioId("BEN654321")
            .canal("TELEFONE")
            .tipo("CONSULTA_COBERTURA")
            .dataHora(LocalDateTime.now().minusHours(2))
            .duracao(Duration.ofMinutes(15))
            .resolvido(true)
            .build();
        interacaoRepository.save(interacao);
    }

    @Test
    @DisplayName("Should complete promoter NPS journey")
    void shouldCompletePromoterJourney() throws Exception {
        // Given: Post-interaction NPS survey trigger
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN654321");
        variables.put("interacaoId", interacao.getId());
        variables.put("canal", "WHATSAPP");
        variables.put("tipoInteracao", "CONSULTA_COBERTURA");

        // When: Trigger NPS survey
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", variables);

        // Then: Survey sent
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                PesquisaNps pesquisa = pesquisaNpsRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(pesquisa.getStatus()).isEqualTo(StatusPesquisa.ENVIADA);
                assertThat(pesquisa.getCanal()).isEqualTo("WHATSAPP");
            });

        // Verify survey sent event
        JsonNode enviadaEvent = consumeKafkaMessage("nps.pesquisa_enviada", Duration.ofSeconds(5));
        assertThat(enviadaEvent.get("beneficiarioId").asText()).isEqualTo("BEN654321");

        // Simulate promoter response (score 9)
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task surveyTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("coletar-resposta-nps")
                    .singleResult();
                assertThat(surveyTask).isNotNull();
            });

        Task surveyTask = taskService.createTaskQuery()
            .processInstanceId(process.getId())
            .taskDefinitionKey("coletar-resposta-nps")
            .singleResult();

        Map<String, Object> response = new HashMap<>();
        response.put("nota", 9);
        response.put("comentario", "Excelente atendimento, muito rápido e eficiente!");
        response.put("recomendaria", true);

        taskService.complete(surveyTask.getId(), response);

        // Then: Response classified as PROMOTOR
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                PesquisaNps pesquisa = pesquisaNpsRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(pesquisa.getNota()).isEqualTo(9);
                assertThat(pesquisa.getClassificacao()).isEqualTo(ClassificacaoNps.PROMOTOR);
                assertThat(pesquisa.getStatus()).isEqualTo(StatusPesquisa.RESPONDIDA);
            });

        // Verify NPS response event
        JsonNode respostaEvent = consumeKafkaMessage("nps.resposta_recebida", Duration.ofSeconds(5));
        assertThat(respostaEvent.get("nota").asInt()).isEqualTo(9);
        assertThat(respostaEvent.get("classificacao").asText()).isEqualTo("PROMOTOR");

        // Verify no recovery action needed
        List<Recuperacao> recuperacoes = recuperacaoRepository.findByBeneficiarioId("BEN654321");
        assertThat(recuperacoes).isEmpty();
    }

    @Test
    @DisplayName("Should handle detractor with recovery workflow")
    void shouldHandleDetractorWithRecovery() throws Exception {
        // Given: Post-interaction NPS survey
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN654321");
        variables.put("interacaoId", interacao.getId());
        variables.put("canal", "EMAIL");

        // When: Trigger survey
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", variables);

        // Simulate detractor response (score 3)
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task surveyTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("coletar-resposta-nps")
                    .singleResult();
                assertThat(surveyTask).isNotNull();
            });

        Task surveyTask = taskService.createTaskQuery()
            .processInstanceId(process.getId())
            .taskDefinitionKey("coletar-resposta-nps")
            .singleResult();

        Map<String, Object> detractorResponse = new HashMap<>();
        detractorResponse.put("nota", 3);
        detractorResponse.put("comentario", "Muito tempo de espera, informações confusas");
        detractorResponse.put("recomendaria", false);
        detractorResponse.put("problemasPrincipais", List.of("TEMPO_ESPERA", "INFORMACAO_CONFUSA"));

        taskService.complete(surveyTask.getId(), detractorResponse);

        // Then: Detractor identified and recovery triggered
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                PesquisaNps pesquisa = pesquisaNpsRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(pesquisa.getClassificacao()).isEqualTo(ClassificacaoNps.DETRATOR);
            });

        // Verify detractor event
        JsonNode detratadorEvent = consumeKafkaMessage("nps.detrator_identificado", Duration.ofSeconds(5));
        assertThat(detratadorEvent.get("nota").asInt()).isEqualTo(3);

        // Verify recovery action created
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Recuperacao> recuperacoes = recuperacaoRepository.findByBeneficiarioId("BEN654321");
                assertThat(recuperacoes).isNotEmpty();
                assertThat(recuperacoes.get(0).getStatus()).isEqualTo(StatusRecuperacao.INICIADA);
                assertThat(recuperacoes.get(0).getPrioridade()).isEqualTo(PrioridadeRecuperacao.ALTA);
            });

        // Verify recovery event
        JsonNode recuperacaoEvent = consumeKafkaMessage("nps.recuperacao_iniciada", Duration.ofSeconds(5));
        assertThat(recuperacaoEvent.get("beneficiarioId").asText()).isEqualTo("BEN654321");

        // Verify recovery task assigned
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task recoveryTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("executar-recuperacao-detrator")
                    .singleResult();
                assertThat(recoveryTask).isNotNull();
                assertThat(recoveryTask.getPriority()).isGreaterThan(50);
            });
    }

    @Test
    @DisplayName("Should calculate NPS score correctly")
    void shouldCalculateNPSScore() throws Exception {
        // Given: Multiple NPS responses over time
        List<Map<String, Object>> responses = List.of(
            Map.of("beneficiarioId", "BEN001", "nota", 9),  // Promotor
            Map.of("beneficiarioId", "BEN002", "nota", 10), // Promotor
            Map.of("beneficiarioId", "BEN003", "nota", 8),  // Passivo
            Map.of("beneficiarioId", "BEN004", "nota", 7),  // Passivo
            Map.of("beneficiarioId", "BEN005", "nota", 4),  // Detrator
            Map.of("beneficiarioId", "BEN006", "nota", 9),  // Promotor
            Map.of("beneficiarioId", "BEN007", "nota", 6),  // Detrator
            Map.of("beneficiarioId", "BEN008", "nota", 10), // Promotor
            Map.of("beneficiarioId", "BEN009", "nota", 8),  // Passivo
            Map.of("beneficiarioId", "BEN010", "nota", 9)   // Promotor
        );

        // When: Calculate NPS
        Map<String, Object> calculoVariables = new HashMap<>();
        calculoVariables.put("periodo", "MENSAL");
        calculoVariables.put("respostas", responses);
        calculoVariables.put("totalRespostas", 10);

        ProcessInstance calculoProcess = runtimeService
            .startProcessInstanceByKey("DMN-008-Calculo-NPS", calculoVariables);

        // Then: NPS calculated correctly
        // Promoters: 5 (50%), Passivos: 3 (30%), Detratores: 2 (20%)
        // NPS = 50% - 20% = 30
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Object npsScore = runtimeService.getVariable(calculoProcess.getId(), "npsScore");
                assertThat(npsScore).isEqualTo(30);

                Object percentualPromotores = runtimeService.getVariable(calculoProcess.getId(), "percentualPromotores");
                assertThat(percentualPromotores).isEqualTo(50.0);

                Object percentualDetratores = runtimeService.getVariable(calculoProcess.getId(), "percentualDetratores");
                assertThat(percentualDetratores).isEqualTo(20.0);
            });

        // Verify score calculated event
        JsonNode scoreEvent = consumeKafkaMessage("nps.score_calculado", Duration.ofSeconds(5));
        assertThat(scoreEvent.get("score").asInt()).isEqualTo(30);
        assertThat(scoreEvent.get("periodo").asText()).isEqualTo("MENSAL");
    }

    @Test
    @DisplayName("Should track NPS across multiple touchpoints")
    void shouldTrackMultipleTouchpoints() throws Exception {
        // Given: Beneficiary with multiple interactions
        Interacao consulta = TestDataBuilder.buildInteracao()
            .beneficiarioId("BEN654321")
            .tipo("CONSULTA_MEDICA")
            .canal("PRESENCIAL")
            .build();
        interacaoRepository.save(consulta);

        Interacao autorizacao = TestDataBuilder.buildInteracao()
            .beneficiarioId("BEN654321")
            .tipo("AUTORIZACAO")
            .canal("APP")
            .build();
        interacaoRepository.save(autorizacao);

        // When: Collect NPS for each touchpoint
        Map<String, Object> consultaVariables = Map.of(
            "beneficiarioId", "BEN654321",
            "interacaoId", consulta.getId(),
            "touchpoint", "POS_CONSULTA"
        );

        Map<String, Object> autorizacaoVariables = Map.of(
            "beneficiarioId", "BEN654321",
            "interacaoId", autorizacao.getId(),
            "touchpoint", "POS_AUTORIZACAO"
        );

        ProcessInstance consultaProcess = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", consultaVariables);

        ProcessInstance autorizacaoProcess = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", autorizacaoVariables);

        // Then: Both touchpoints tracked independently
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<PesquisaNps> pesquisas = pesquisaNpsRepository.findByBeneficiarioId("BEN654321");
                assertThat(pesquisas).hasSizeGreaterThanOrEqualTo(2);
                assertThat(pesquisas)
                    .extracting(PesquisaNps::getTouchpoint)
                    .contains("POS_CONSULTA", "POS_AUTORIZACAO");
            });
    }

    @Test
    @DisplayName("Should segment NPS by demographic and behavior")
    void shouldSegmentNPS() throws Exception {
        // Given: NPS analysis request with segmentation
        Map<String, Object> analysisVariables = new HashMap<>();
        analysisVariables.put("periodo", "TRIMESTRAL");
        analysisVariables.put("segmentos", List.of("IDADE", "PLANO", "REGIAO", "FREQUENCIA_USO"));

        // When: Execute segmented analysis
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("NPS-Analise-Segmentada", analysisVariables);

        // Then: Segmented scores calculated
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                Object segmentacoes = runtimeService.getVariable(process.getId(), "npsPorSegmento");
                assertThat(segmentacoes).isNotNull();

                Object tendencias = runtimeService.getVariable(process.getId(), "tendencias");
                assertThat(tendencias).isNotNull();
            });
    }

    @Test
    @DisplayName("Should identify improvement opportunities from NPS feedback")
    void shouldIdentifyImprovementOpportunities() throws Exception {
        // Given: Multiple detractor responses with similar issues
        List<Map<String, Object>> feedbacks = List.of(
            Map.of("nota", 4, "problemas", List.of("TEMPO_ESPERA"), "comentario", "Muita espera no telefone"),
            Map.of("nota", 3, "problemas", List.of("TEMPO_ESPERA"), "comentario", "Demorou 40 minutos para ser atendido"),
            Map.of("nota", 5, "problemas", List.of("TEMPO_ESPERA", "ATENDIMENTO"), "comentario", "Longa espera e atendente sem informação")
        );

        Map<String, Object> analysisVariables = new HashMap<>();
        analysisVariables.put("feedbacks", feedbacks);
        analysisVariables.put("periodo", "SEMANAL");

        // When: Analyze patterns
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("NPS-Analise-Padroes", analysisVariables);

        // Then: Improvement opportunities identified
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Object oportunidades = runtimeService.getVariable(process.getId(), "oportunidadesMelhoria");
                assertThat(oportunidades).isNotNull();

                Object problemaRecorrente = runtimeService.getVariable(process.getId(), "problemaTopPrioridade");
                assertThat(problemaRecorrente).isEqualTo("TEMPO_ESPERA");
            });
    }

    @Test
    @DisplayName("Should track NPS recovery success rate")
    void shouldTrackRecoverySuccessRate() throws Exception {
        // Given: Detractor with completed recovery
        PesquisaNps pesquisaInicial = TestDataBuilder.buildPesquisaNps()
            .beneficiarioId("BEN654321")
            .nota(4)
            .classificacao(ClassificacaoNps.DETRATOR)
            .build();
        pesquisaNpsRepository.save(pesquisaInicial);

        Recuperacao recuperacao = TestDataBuilder.buildRecuperacao()
            .beneficiarioId("BEN654321")
            .pesquisaNpsId(pesquisaInicial.getId())
            .status(StatusRecuperacao.CONCLUIDA)
            .dataResolucao(LocalDateTime.now().minusDays(3))
            .build();
        recuperacaoRepository.save(recuperacao);

        // When: Send follow-up NPS after recovery
        Map<String, Object> followUpVariables = new HashMap<>();
        followUpVariables.put("beneficiarioId", "BEN654321");
        followUpVariables.put("recuperacaoId", recuperacao.getId());
        followUpVariables.put("tipoSurvey", "FOLLOWUP_RECUPERACAO");

        ProcessInstance followUpProcess = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", followUpVariables);

        // Simulate improved score after recovery
        Task followUpTask = taskService.createTaskQuery()
            .processInstanceId(followUpProcess.getId())
            .taskDefinitionKey("coletar-resposta-nps")
            .singleResult();

        Map<String, Object> improvedResponse = new HashMap<>();
        improvedResponse.put("nota", 8);
        improvedResponse.put("comentario", "Problema resolvido satisfatoriamente");
        improvedResponse.put("recomendaria", true);

        taskService.complete(followUpTask.getId(), improvedResponse);

        // Then: Recovery success recorded
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Recuperacao recuperacaoAtualizada = recuperacaoRepository.findById(recuperacao.getId())
                    .orElseThrow();
                assertThat(recuperacaoAtualizada.getSucessoRecuperacao()).isTrue();
                assertThat(recuperacaoAtualizada.getNotaAposRecuperacao()).isEqualTo(8);
                assertThat(recuperacaoAtualizada.getMelhoria()).isEqualTo(4); // 8 - 4 = 4 points improvement
            });
    }
}
