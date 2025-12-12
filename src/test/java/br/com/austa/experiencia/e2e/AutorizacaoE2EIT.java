package br.com.austa.experiencia.e2e;

import br.com.austa.experiencia.BaseIntegrationTest;
import br.com.austa.experiencia.builder.TestDataBuilder;
import br.com.austa.experiencia.model.*;
import br.com.austa.experiencia.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * E2E Integration Test for Authorization Journey
 *
 * Tests the complete authorization flow:
 * 1. Reception - Request receipt and validation
 * 2. Classification - Auto-approval vs manual review routing
 * 3. Authorization - Decision making (auto/audit)
 * 4. Notification - Provider and beneficiary alerts
 * 5. Integration - Tasy ERP synchronization
 *
 * Scenarios:
 * - Auto-approval for low-value procedures
 * - Audit routing for high-value/complex cases
 * - Provider and beneficiary notifications
 * - ERP integration and guide generation
 * - Denial and appeal workflows
 *
 * @see PROMPT_TECNICO_3.MD Lines 891-1023 (SUB-006)
 */
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    "autorizacao.solicitada",
    "autorizacao.aprovada",
    "autorizacao.negada",
    "autorizacao.auditoria",
    "notificacao.prestador",
    "notificacao.beneficiario"
})
@DisplayName("E2E: Authorization Journey")
public class AutorizacaoE2EIT extends BaseIntegrationTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AutorizacaoRepository autorizacaoRepository;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private PrestadorRepository prestadorRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Beneficiario beneficiario;
    private Prestador prestador;

    @BeforeEach
    void setupTestData() {
        // Create test beneficiary
        beneficiario = TestDataBuilder.buildBeneficiario()
            .cpf("12345678901")
            .nome("João Silva")
            .codigoExterno("BEN123456")
            .plano("GOLD")
            .status(StatusBeneficiario.ATIVO)
            .build();
        beneficiarioRepository.save(beneficiario);

        // Create test provider
        prestador = TestDataBuilder.buildPrestador()
            .codigo("PREST001")
            .nome("Hospital São Lucas")
            .cnpj("12345678000190")
            .especialidades(List.of("CARDIOLOGIA", "ORTOPEDIA"))
            .build();
        prestadorRepository.save(prestador);
    }

    @Test
    @DisplayName("Should auto-approve low-value consultation authorization")
    void shouldAutoApproveLowValueConsultation() throws Exception {
        // Given: Low-value consultation request
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("prestadorId", "PREST001");
        variables.put("tipoSolicitacao", "CONSULTA");
        variables.put("especialidade", "CARDIOLOGIA");
        variables.put("valorEstimado", 250.00);
        variables.put("urgencia", "NORMAL");
        variables.put("observacoes", "Consulta de rotina - check-up anual");

        // Mock Tasy ERP validation
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/validar"))
            .willReturn(aOk()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"aprovada\": true, \"numeroGuia\": \"GUIA123456\", \"validadeAte\": \"2025-12-31\"}")));

        // Mock notification services
        stubFor(post(urlEqualTo("/api/sms/send"))
            .willReturn(aOk().withBody("{\"messageId\": \"SMS123\", \"status\": \"sent\"}")));

        stubFor(post(urlEqualTo("/api/email/send"))
            .willReturn(aOk().withBody("{\"messageId\": \"EMAIL123\", \"status\": \"sent\"}")));

        // When: Start authorization process
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", variables);

        // Then: Should auto-approve (value < 500)
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacao.APROVADA);
                assertThat(autorizacao.getNumeroGuia()).isEqualTo("GUIA123456");
                assertThat(autorizacao.getDataAprovacao()).isNotNull();
                assertThat(autorizacao.getTipoAprovacao()).isEqualTo(TipoAprovacao.AUTOMATICA);
            });

        // Verify Kafka events
        JsonNode solicitadaEvent = consumeKafkaMessage("autorizacao.solicitada", Duration.ofSeconds(5));
        assertThat(solicitadaEvent.get("tipoSolicitacao").asText()).isEqualTo("CONSULTA");
        assertThat(solicitadaEvent.get("valorEstimado").asDouble()).isEqualTo(250.00);

        JsonNode aprovadaEvent = consumeKafkaMessage("autorizacao.aprovada", Duration.ofSeconds(5));
        assertThat(aprovadaEvent.get("numeroGuia").asText()).isEqualTo("GUIA123456");
        assertThat(aprovadaEvent.get("tipoAprovacao").asText()).isEqualTo("AUTOMATICA");

        // Verify notifications sent
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Notificacao> notificacoes = notificacaoRepository.findByAutorizacaoId(
                    autorizacaoRepository.findByProcessInstanceId(process.getId()).get().getId()
                );
                assertThat(notificacoes).hasSizeGreaterThanOrEqualTo(2);
                assertThat(notificacoes)
                    .extracting(Notificacao::getDestinatarioTipo)
                    .contains(TipoDestinatario.BENEFICIARIO, TipoDestinatario.PRESTADOR);
            });

        // Verify Tasy ERP called
        verify(1, postRequestedFor(urlEqualTo("/api/tasy/autorizacao/validar")));

        // Verify process completed
        assertThat(runtimeService.createProcessInstanceQuery()
            .processInstanceId(process.getId())
            .singleResult()).isNull();
    }

    @Test
    @DisplayName("Should route high-value surgery to audit team")
    void shouldRouteHighValueSurgeryToAudit() throws Exception {
        // Given: High-value surgery request
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("prestadorId", "PREST001");
        variables.put("tipoSolicitacao", "CIRURGIA");
        variables.put("procedimento", "ANGIOPLASTIA_CORONARIANA");
        variables.put("valorEstimado", 45000.00);
        variables.put("urgencia", "ALTA");
        variables.put("cid10", "I25.1");
        variables.put("justificativa", "Oclusão arterial 90% - risco de infarto iminente");

        // Mock Tasy clinical data
        stubFor(get(urlMatching("/api/tasy/beneficiario/BEN123456/historico"))
            .willReturn(aOk()
                .withBody("{\"historicoCardiovascular\": true, \"internacoesAnteriores\": 2}")));

        // When: Start authorization process
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", variables);

        // Then: Should route to audit (value > 10000)
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacao.EM_ANALISE);
            });

        // Verify audit task created
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task auditTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("analisar-auditoria")
                    .singleResult();
                assertThat(auditTask).isNotNull();
                assertThat(auditTask.getAssignee()).isIn("auditor", "auditor-senior");
            });

        // Verify audit event
        JsonNode auditoriaEvent = consumeKafkaMessage("autorizacao.auditoria", Duration.ofSeconds(5));
        assertThat(auditoriaEvent.get("valorEstimado").asDouble()).isEqualTo(45000.00);
        assertThat(auditoriaEvent.get("urgencia").asText()).isEqualTo("ALTA");

        // Simulate auditor approval
        Task auditTask = taskService.createTaskQuery()
            .processInstanceId(process.getId())
            .taskDefinitionKey("analisar-auditoria")
            .singleResult();

        Map<String, Object> auditDecision = new HashMap<>();
        auditDecision.put("decisao", "APROVAR");
        auditDecision.put("parecerTecnico", "Procedimento necessário conforme quadro clínico");
        auditDecision.put("auditorId", "AUD001");

        // Mock Tasy guide generation for approved surgery
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/gerar-guia"))
            .willReturn(aOk()
                .withBody("{\"numeroGuia\": \"GUIA999888\", \"validadeAte\": \"2025-12-15\"}")));

        taskService.complete(auditTask.getId(), auditDecision);

        // Verify approval after audit
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacao.APROVADA);
                assertThat(autorizacao.getTipoAprovacao()).isEqualTo(TipoAprovacao.MANUAL);
                assertThat(autorizacao.getNumeroGuia()).isEqualTo("GUIA999888");
                assertThat(autorizacao.getParecerTecnico()).contains("necessário conforme quadro");
            });
    }

    @Test
    @DisplayName("Should deny authorization with missing clinical justification")
    void shouldDenyAuthorizationWithMissingJustification() throws Exception {
        // Given: Request without proper justification
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("prestadorId", "PREST001");
        variables.put("tipoSolicitacao", "EXAME");
        variables.put("procedimento", "RESSONANCIA_MAGNETICA");
        variables.put("valorEstimado", 1500.00);
        variables.put("justificativa", ""); // Empty justification

        // When: Start authorization process
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", variables);

        // Then: Should deny due to missing information
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacao.NEGADA);
                assertThat(autorizacao.getMotivoNegacao()).contains("justificativa clínica");
            });

        // Verify denial event
        JsonNode negadaEvent = consumeKafkaMessage("autorizacao.negada", Duration.ofSeconds(5));
        assertThat(negadaEvent.get("motivo").asText()).containsIgnoringCase("justificativa");

        // Verify notifications sent
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Notificacao> notificacoes = notificacaoRepository.findByAutorizacaoId(
                    autorizacaoRepository.findByProcessInstanceId(process.getId()).get().getId()
                );
                assertThat(notificacoes).isNotEmpty();
                assertThat(notificacoes.get(0).getMensagem()).contains("negada");
            });
    }

    @Test
    @DisplayName("Should handle urgent authorization with expedited workflow")
    void shouldHandleUrgentAuthorizationExpedited() throws Exception {
        // Given: Urgent authorization request
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("prestadorId", "PREST001");
        variables.put("tipoSolicitacao", "INTERNACAO");
        variables.put("valorEstimado", 8000.00);
        variables.put("urgencia", "EMERGENCIAL");
        variables.put("justificativa", "Quadro de dor torácica - suspeita IAM");

        // Mock Tasy emergency validation
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/emergencia"))
            .willReturn(aOk()
                .withBody("{\"aprovada\": true, \"numeroGuia\": \"EMERG123\", \"validadeAte\": \"2025-12-11\"}")));

        // When: Start authorization process
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", variables);

        // Then: Should approve immediately via emergency path
        await().atMost(Duration.ofSeconds(8))
            .untilAsserted(() -> {
                Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacao.APROVADA);
                assertThat(autorizacao.getNumeroGuia()).isEqualTo("EMERG123");
                assertThat(autorizacao.getTipoAprovacao()).isEqualTo(TipoAprovacao.EMERGENCIAL);
            });

        // Verify expedited notifications
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<Notificacao> notificacoes = notificacaoRepository.findByAutorizacaoId(
                    autorizacaoRepository.findByProcessInstanceId(process.getId()).get().getId()
                );
                assertThat(notificacoes).isNotEmpty();
                assertThat(notificacoes)
                    .allMatch(n -> n.getCanalPrioridade().equals("SMS") ||
                                   n.getCanalPrioridade().equals("PUSH"));
            });
    }

    @Test
    @DisplayName("Should handle authorization with provider network validation")
    void shouldValidateProviderNetwork() throws Exception {
        // Given: Request with out-of-network provider
        Prestador outOfNetworkProvider = TestDataBuilder.buildPrestador()
            .codigo("PREST999")
            .nome("Clínica Particular")
            .redeCredenciada(false)
            .build();
        prestadorRepository.save(outOfNetworkProvider);

        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("prestadorId", "PREST999");
        variables.put("tipoSolicitacao", "CONSULTA");
        variables.put("valorEstimado", 500.00);

        // When: Start authorization process
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", variables);

        // Then: Should require additional approval for out-of-network
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Task approvalTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("aprovar-fora-rede")
                    .singleResult();
                assertThat(approvalTask).isNotNull();
            });

        // Verify warning event
        List<Evento> eventos = eventoRepository.findByProcessInstanceId(process.getId());
        assertThat(eventos)
            .anyMatch(e -> e.getTipo() == TipoEvento.ALERTA_PRESTADOR_FORA_REDE);
    }

    @Test
    @DisplayName("Should handle authorization with beneficiary carency validation")
    void shouldValidateBeneficiaryCarency() throws Exception {
        // Given: Recent beneficiary with carency period
        Beneficiario recentBeneficiario = TestDataBuilder.buildBeneficiario()
            .cpf("99988877766")
            .codigoExterno("BEN777888")
            .dataAdesao(LocalDateTime.now().minusDays(30)) // 30 days ago
            .build();
        beneficiarioRepository.save(recentBeneficiario);

        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN777888");
        variables.put("prestadorId", "PREST001");
        variables.put("tipoSolicitacao", "CIRURGIA");
        variables.put("procedimento", "HERNIORRAFIA");
        variables.put("valorEstimado", 5000.00);

        // Mock Tasy carency validation
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/validar-carencia"))
            .willReturn(aOk()
                .withBody("{\"emCarencia\": true, \"diasRestantes\": 150, \"procedimento\": \"HERNIORRAFIA\"}")));

        // When: Start authorization process
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", variables);

        // Then: Should deny due to carency
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(process.getId())
                    .orElseThrow();
                assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacao.NEGADA);
                assertThat(autorizacao.getMotivoNegacao()).containsIgnoringCase("carência");
            });
    }

    @Test
    @DisplayName("Should synchronize authorization with Tasy ERP")
    void shouldSynchronizeWithTasyERP() throws Exception {
        // Given: Authorization request
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("prestadorId", "PREST001");
        variables.put("tipoSolicitacao", "EXAME");
        variables.put("procedimento", "ULTRASSOM_ABDOMINAL");
        variables.put("valorEstimado", 300.00);

        // Mock Tasy integration endpoints
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/validar"))
            .willReturn(aOk()
                .withBody("{\"aprovada\": true, \"numeroGuia\": \"TASY987654\", \"validadeAte\": \"2025-12-20\"}")));

        stubFor(post(urlEqualTo("/api/tasy/autorizacao/sincronizar"))
            .willReturn(aOk()
                .withBody("{\"sincronizado\": true, \"timestamp\": \"2025-12-11T10:30:00\"}")));

        // When: Start and complete authorization
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", variables);

        // Then: Should sync with Tasy
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                verify(1, postRequestedFor(urlEqualTo("/api/tasy/autorizacao/validar")));
                verify(1, postRequestedFor(urlEqualTo("/api/tasy/autorizacao/sincronizar")));
            });

        // Verify authorization has Tasy reference
        Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(process.getId()).orElseThrow();
        assertThat(autorizacao.getNumeroGuia()).isEqualTo("TASY987654");
        assertThat(autorizacao.getSistemaOrigem()).isEqualTo("TASY");
    }
}
