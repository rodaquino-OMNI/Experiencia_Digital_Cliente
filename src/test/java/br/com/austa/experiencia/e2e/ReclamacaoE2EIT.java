package br.com.austa.experiencia.e2e;

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
 * E2E Integration Test for Complaint Resolution Journey
 *
 * Tests the complete complaint handling flow:
 * 1. Registration - Complaint intake and categorization
 * 2. Root Cause Analysis - Investigation and diagnosis
 * 3. Solution - Action plan and implementation
 * 4. Resolution - Closure and satisfaction measurement
 * 5. ANS Escalation - Regulatory compliance scenarios
 *
 * Scenarios:
 * - Simple complaint resolution
 * - Complex multi-department investigation
 * - ANS escalation and regulatory reporting
 * - Compensation workflows
 * - Satisfaction tracking
 *
 * @see PROMPT_TECNICO_3.MD Lines 1079-1169 (SUB-009)
 */
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    "reclamacao.registrada",
    "reclamacao.em_analise",
    "reclamacao.resolvida",
    "reclamacao.ans_escalada",
    "compensacao.aplicada"
})
@DisplayName("E2E: Complaint Resolution Journey")
public class ReclamacaoE2EIT extends BaseIntegrationTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ReclamacaoRepository reclamacaoRepository;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private CompensacaoRepository compensacaoRepository;

    @Autowired
    private SatisfacaoRepository satisfacaoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    private Beneficiario beneficiario;

    @BeforeEach
    void setupTestData() {
        beneficiario = TestDataBuilder.buildBeneficiario()
            .cpf("12345678901")
            .nome("João Silva")
            .codigoExterno("BEN123456")
            .build();
        beneficiarioRepository.save(beneficiario);
    }

    @Test
    @DisplayName("Should complete simple complaint resolution workflow")
    void shouldCompleteSimpleComplaintResolution() throws Exception {
        // Given: Simple service complaint
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("canal", "WHATSAPP");
        variables.put("categoria", "ATENDIMENTO");
        variables.put("descricao", "Demora no atendimento telefônico - aguardei 45 minutos");
        variables.put("gravidade", "MEDIA");

        // Mock NLP classification
        stubFor(post(urlEqualTo("/api/nlp/classify-complaint"))
            .willReturn(aOk()
                .withBody("{\"categoria\": \"ATENDIMENTO\", \"sentimento\": \"NEGATIVO\", \"urgencia\": \"MEDIA\"}")));

        // When: Register complaint
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-009-Reclamacoes-Gestao", variables);

        // Then: Complaint registered and classified
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Reclamacao reclamacao = reclamacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(reclamacao.getStatus()).isEqualTo(StatusReclamacao.REGISTRADA);
                assertThat(reclamacao.getCategoria()).isEqualTo("ATENDIMENTO");
                assertThat(reclamacao.getGravidade()).isEqualTo(GravidadeReclamacao.MEDIA);
            });

        // Verify registration event
        JsonNode registradaEvent = consumeKafkaMessage("reclamacao.registrada", Duration.ofSeconds(5));
        assertThat(registradaEvent.get("categoria").asText()).isEqualTo("ATENDIMENTO");

        // Simulate root cause analysis
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task analysisTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("analisar-causa-raiz")
                    .singleResult();
                assertThat(analysisTask).isNotNull();
            });

        Task analysisTask = taskService.createTaskQuery()
            .processInstanceId(process.getId())
            .taskDefinitionKey("analisar-causa-raiz")
            .singleResult();

        Map<String, Object> analysisResult = new HashMap<>();
        analysisResult.put("causaRaiz", "FALTA_AGENTES");
        analysisResult.put("departamentoResponsavel", "ATENDIMENTO");
        analysisResult.put("analise", "Pico de atendimento sem escala adequada");

        taskService.complete(analysisTask.getId(), analysisResult);

        // Verify analysis status
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Reclamacao reclamacao = reclamacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(reclamacao.getStatus()).isEqualTo(StatusReclamacao.EM_ANALISE);
                assertThat(reclamacao.getCausaRaiz()).isEqualTo("FALTA_AGENTES");
            });

        // Simulate solution implementation
        Task solutionTask = taskService.createTaskQuery()
            .processInstanceId(process.getId())
            .taskDefinitionKey("implementar-solucao")
            .singleResult();

        Map<String, Object> solution = new HashMap<>();
        solution.put("acoes", List.of(
            "Ajustar escala de atendimento em horários de pico",
            "Implementar callback automático após 10 minutos de espera"
        ));
        solution.put("responsavelId", "GEST001");
        solution.put("prazoImplementacao", LocalDateTime.now().plusDays(7));

        taskService.complete(solutionTask.getId(), solution);

        // Verify resolution
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Reclamacao reclamacao = reclamacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(reclamacao.getStatus()).isEqualTo(StatusReclamacao.RESOLVIDA);
                assertThat(reclamacao.getDataResolucao()).isNotNull();
            });

        // Verify resolution event
        JsonNode resolvidaEvent = consumeKafkaMessage("reclamacao.resolvida", Duration.ofSeconds(5));
        assertThat(resolvidaEvent.get("causaRaiz").asText()).isEqualTo("FALTA_AGENTES");

        // Verify satisfaction survey triggered
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<Satisfacao> surveys = satisfacaoRepository.findByBeneficiarioIdAndTipo(
                    "BEN123456", TipoSatisfacao.POS_RECLAMACAO
                );
                assertThat(surveys).isNotEmpty();
                assertThat(surveys.get(0).getStatus()).isEqualTo(StatusSatisfacao.PENDENTE);
            });
    }

    @Test
    @DisplayName("Should escalate high-severity complaint to ANS")
    void shouldEscalateToANS() throws Exception {
        // Given: High-severity complaint about denied authorization
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("canal", "PRESENCIAL");
        variables.put("categoria", "NEGATIVA_AUTORIZACAO");
        variables.put("descricao", "Autorização negada indevidamente para cirurgia urgente");
        variables.put("gravidade", "ALTA");
        variables.put("tempoSemResolucao", 8); // 8 days unresolved

        // Mock ANS API
        stubFor(post(urlEqualTo("/api/ans/reclamacao/registrar"))
            .willReturn(aOk()
                .withBody("{\"protocoloANS\": \"ANS123456789\", \"prazoResposta\": \"2025-12-25\"}")));

        // When: Register high-severity complaint
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-009-Reclamacoes-Gestao", variables);

        // Then: Should auto-escalate to ANS
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                Reclamacao reclamacao = reclamacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(reclamacao.getStatus()).isEqualTo(StatusReclamacao.ESCALADA_ANS);
                assertThat(reclamacao.getProtocoloANS()).isEqualTo("ANS123456789");
            });

        // Verify ANS escalation event
        JsonNode ansEvent = consumeKafkaMessage("reclamacao.ans_escalada", Duration.ofSeconds(5));
        assertThat(ansEvent.get("protocoloANS").asText()).isEqualTo("ANS123456789");
        assertThat(ansEvent.get("categoria").asText()).isEqualTo("NEGATIVA_AUTORIZACAO");

        // Verify ANS API called
        verify(1, postRequestedFor(urlEqualTo("/api/ans/reclamacao/registrar")));

        // Verify urgency task created
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task urgentTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("tratar-reclamacao-ans")
                    .singleResult();
                assertThat(urgentTask).isNotNull();
                assertThat(urgentTask.getPriority()).isGreaterThan(50);
            });
    }

    @Test
    @DisplayName("Should apply compensation for service failure")
    void shouldApplyCompensation() throws Exception {
        // Given: Complaint with compensation criteria
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("categoria", "FALHA_SERVICO");
        variables.put("descricao", "Consulta agendada cancelada 3 vezes pelo prestador");
        variables.put("gravidade", "ALTA");
        variables.put("impactoFinanceiro", 450.00); // Transport costs

        // When: Process complaint with compensation analysis
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-009-Reclamacoes-Gestao", variables);

        // Complete analysis indicating compensation needed
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task analysisTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("analisar-causa-raiz")
                    .singleResult();
                assertThat(analysisTask).isNotNull();
            });

        Task analysisTask = taskService.createTaskQuery()
            .processInstanceId(process.getId())
            .taskDefinitionKey("analisar-causa-raiz")
            .singleResult();

        Map<String, Object> analysisWithCompensation = new HashMap<>();
        analysisWithCompensation.put("causaRaiz", "FALHA_PRESTADOR");
        analysisWithCompensation.put("requererCompensacao", true);
        analysisWithCompensation.put("tipoCompensacao", "REEMBOLSO");
        analysisWithCompensation.put("valorCompensacao", 450.00);

        taskService.complete(analysisTask.getId(), analysisWithCompensation);

        // Then: Compensation should be created and approved
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Compensacao> compensacoes = compensacaoRepository.findByBeneficiarioId("BEN123456");
                assertThat(compensacoes).isNotEmpty();
                assertThat(compensacoes.get(0).getTipo()).isEqualTo(TipoCompensacao.REEMBOLSO);
                assertThat(compensacoes.get(0).getValor()).isEqualTo(450.00);
                assertThat(compensacoes.get(0).getStatus()).isEqualTo(StatusCompensacao.APROVADA);
            });

        // Verify compensation event
        JsonNode compensacaoEvent = consumeKafkaMessage("compensacao.aplicada", Duration.ofSeconds(5));
        assertThat(compensacaoEvent.get("valor").asDouble()).isEqualTo(450.00);
        assertThat(compensacaoEvent.get("tipo").asText()).isEqualTo("REEMBOLSO");
    }

    @Test
    @DisplayName("Should handle complex multi-department investigation")
    void shouldHandleComplexInvestigation() throws Exception {
        // Given: Complex complaint involving multiple departments
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("categoria", "COBRANCA_INDEVIDA");
        variables.put("descricao", "Cobrança de procedimento já coberto pelo plano");
        variables.put("gravidade", "ALTA");
        variables.put("valorContestado", 2500.00);

        // When: Start investigation
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-009-Reclamacoes-Gestao", variables);

        // Then: Should involve financial and medical audit departments
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Task> investigationTasks = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .list();
                assertThat(investigationTasks).hasSizeGreaterThanOrEqualTo(2);
                assertThat(investigationTasks)
                    .extracting(Task::getTaskDefinitionKey)
                    .contains("analisar-causa-raiz", "validar-cobertura");
            });

        // Complete financial analysis
        Task financialTask = taskService.createTaskQuery()
            .processInstanceId(process.getId())
            .taskDefinitionKey("analisar-causa-raiz")
            .singleResult();

        Map<String, Object> financialAnalysis = new HashMap<>();
        financialAnalysis.put("causaRaiz", "ERRO_SISTEMA");
        financialAnalysis.put("departamentoResponsavel", "TI");
        financialAnalysis.put("requererEstorno", true);

        taskService.complete(financialTask.getId(), financialAnalysis);

        // Verify coordination between departments
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Reclamacao reclamacao = reclamacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(reclamacao.getDepartamentosEnvolvidos())
                    .contains("TI", "FINANCEIRO");
            });
    }

    @Test
    @DisplayName("Should track satisfaction after complaint resolution")
    void shouldTrackSatisfactionAfterResolution() throws Exception {
        // Given: Resolved complaint
        Reclamacao reclamacao = TestDataBuilder.buildReclamacao()
            .beneficiarioId("BEN123456")
            .status(StatusReclamacao.RESOLVIDA)
            .dataResolucao(LocalDateTime.now().minusDays(2))
            .build();
        reclamacaoRepository.save(reclamacao);

        // When: Trigger satisfaction survey
        Map<String, Object> surveyVariables = new HashMap<>();
        surveyVariables.put("reclamacaoId", reclamacao.getId());
        surveyVariables.put("beneficiarioId", "BEN123456");

        ProcessInstance surveyProcess = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", surveyVariables);

        // Simulate beneficiary response
        Task surveyTask = taskService.createTaskQuery()
            .processInstanceId(surveyProcess.getId())
            .taskDefinitionKey("coletar-satisfacao")
            .singleResult();

        Map<String, Object> surveyResponse = new HashMap<>();
        surveyResponse.put("satisfacao", 4); // 4/5
        surveyResponse.put("problemaResolvido", true);
        surveyResponse.put("comentarios", "Problema resolvido satisfatoriamente");

        taskService.complete(surveyTask.getId(), surveyResponse);

        // Then: Satisfaction recorded
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Satisfacao> satisfacoes = satisfacaoRepository.findByReclamacaoId(reclamacao.getId());
                assertThat(satisfacoes).isNotEmpty();
                assertThat(satisfacoes.get(0).getNota()).isEqualTo(4);
                assertThat(satisfacoes.get(0).getProblemaResolvido()).isTrue();
            });
    }

    @Test
    @DisplayName("Should handle complaint reopening for unresolved issues")
    void shouldHandleComplaintReopening() throws Exception {
        // Given: Previously "resolved" complaint
        Reclamacao reclamacao = TestDataBuilder.buildReclamacao()
            .beneficiarioId("BEN123456")
            .categoria("ATENDIMENTO")
            .status(StatusReclamacao.RESOLVIDA)
            .dataResolucao(LocalDateTime.now().minusDays(5))
            .build();
        reclamacaoRepository.save(reclamacao);

        // When: Beneficiary reports issue persists
        Map<String, Object> variables = new HashMap<>();
        variables.put("reclamacaoOriginalId", reclamacao.getId());
        variables.put("beneficiarioId", "BEN123456");
        variables.put("motivo", "Problema continua ocorrendo - atendimento ainda demorado");

        ProcessInstance reopenProcess = runtimeService
            .startProcessInstanceByKey("SUB-009-Reclamacoes-Gestao", variables);

        // Then: Should reopen original complaint
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Reclamacao reopenedReclamacao = reclamacaoRepository.findById(reclamacao.getId())
                    .orElseThrow();
                assertThat(reopenedReclamacao.getStatus()).isEqualTo(StatusReclamacao.REABERTA);
                assertThat(reopenedReclamacao.getQuantidadeReaberturas()).isEqualTo(1);
            });

        // Verify escalation due to reopening
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task escalationTask = taskService.createTaskQuery()
                    .processInstanceId(reopenProcess.getId())
                    .taskDefinitionKey("escalar-gestao")
                    .singleResult();
                assertThat(escalationTask).isNotNull();
            });
    }

    @Test
    @DisplayName("Should comply with ANS resolution timeframes")
    void shouldComplyWithANSTimeframes() throws Exception {
        // Given: Complaint with ANS deadline
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("categoria", "NEGATIVA_AUTORIZACAO");
        variables.put("gravidade", "ALTA");
        variables.put("prazoANS", LocalDateTime.now().plusDays(5)); // 5-day ANS deadline

        // When: Process complaint near deadline
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-009-Reclamacoes-Gestao", variables);

        // Then: Should trigger deadline warnings
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Evento> eventos = eventoRepository.findByProcessInstanceId(process.getId());
                assertThat(eventos)
                    .anyMatch(e -> e.getTipo() == TipoEvento.ALERTA_PRAZO_ANS);
            });

        // Verify priority escalation
        Reclamacao reclamacao = reclamacaoRepository.findByProcessInstanceId(process.getId())
            .orElseThrow();
        assertThat(reclamacao.getPrioridade()).isGreaterThanOrEqualTo(8);
    }
}
