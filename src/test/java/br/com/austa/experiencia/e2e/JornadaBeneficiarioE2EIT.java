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
 * E2E Integration Test for Complete Beneficiary Journey
 *
 * Tests the complete beneficiary journey from adhesion to follow-up:
 * 1. Adesão (SUB-001) - Beneficiary onboarding
 * 2. Onboarding (SUB-001) - Initial configuration
 * 3. Interação (SUB-003) - Channel interactions
 * 4. Autorização (SUB-006) - Authorization requests
 * 5. Follow-up (SUB-010) - Post-authorization monitoring
 *
 * Validates:
 * - Multi-subprocess orchestration
 * - State persistence across processes
 * - Kafka event chain
 * - External system integrations
 * - End-to-end data consistency
 *
 * @see PROMPT_TECNICO_3.MD Lines 1321-1381
 */
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    "beneficiario.adesao",
    "beneficiario.onboarding",
    "interacao.received",
    "autorizacao.solicitada",
    "autorizacao.aprovada",
    "followup.agendado",
    "saga.beneficiario.completed"
})
@DisplayName("E2E: Complete Beneficiary Journey")
public class JornadaBeneficiarioE2EIT extends BaseIntegrationTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private InteracaoRepository interacaoRepository;

    @Autowired
    private AutorizacaoRepository autorizacaoRepository;

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should complete full beneficiary journey from adhesion to follow-up")
    void shouldCompleteFullBeneficiaryJourney() throws Exception {
        // ==================== PHASE 1: ADESÃO ====================

        // Given: New beneficiary data
        Map<String, Object> adesaoVariables = new HashMap<>();
        adesaoVariables.put("nome", "João Silva");
        adesaoVariables.put("cpf", "12345678901");
        adesaoVariables.put("email", "joao.silva@email.com");
        adesaoVariables.put("telefone", "+5511999999999");
        adesaoVariables.put("dataNascimento", "1985-06-15");
        adesaoVariables.put("plano", "GOLD");
        adesaoVariables.put("empresaId", "EMP001");

        // Mock ERP API for beneficiary validation
        stubFor(post(urlEqualTo("/api/tasy/beneficiario/validar"))
            .willReturn(aOk()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"valido\": true, \"codigo\": \"BEN123456\"}")));

        // When: Start adhesion process
        ProcessInstance adesaoProcess = runtimeService
            .startProcessInstanceByKey("SUB-001-Adesao-Onboarding", adesaoVariables);

        // Then: Adhesion creates beneficiary record
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Beneficiario beneficiario = beneficiarioRepository.findByCpf("12345678901")
                    .orElseThrow();
                assertThat(beneficiario.getNome()).isEqualTo("João Silva");
                assertThat(beneficiario.getStatus()).isEqualTo(StatusBeneficiario.ATIVO);
                assertThat(beneficiario.getCodigoExterno()).isEqualTo("BEN123456");
            });

        // Verify Kafka event published
        JsonNode adesaoEvent = consumeKafkaMessage("beneficiario.adesao", Duration.ofSeconds(5));
        assertThat(adesaoEvent.get("cpf").asText()).isEqualTo("12345678901");
        assertThat(adesaoEvent.get("evento").asText()).isEqualTo("ADESAO_COMPLETA");

        // ==================== PHASE 2: ONBOARDING ====================

        // Mock SMS service for notification
        stubFor(post(urlEqualTo("/api/sms/send"))
            .willReturn(aOk()
                .withBody("{\"messageId\": \"MSG123\", \"status\": \"sent\"}")));

        // Mock email service
        stubFor(post(urlEqualTo("/api/email/send"))
            .willReturn(aOk()
                .withBody("{\"messageId\": \"EMAIL123\", \"status\": \"sent\"}")));

        // Onboarding subprocess should auto-trigger
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Task> onboardingTasks = taskService.createTaskQuery()
                    .processDefinitionKey("SUB-001-Adesao-Onboarding")
                    .taskDefinitionKey("configurar-preferencias")
                    .list();
                assertThat(onboardingTasks).isNotEmpty();
            });

        // Complete onboarding preferences
        Task preferencesTask = taskService.createTaskQuery()
            .taskDefinitionKey("configurar-preferencias")
            .singleResult();

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("canalPreferencial", "WHATSAPP");
        preferences.put("notificacoesSMS", true);
        preferences.put("notificacoesEmail", true);
        preferences.put("preferenciaHorario", "MANHA");

        taskService.complete(preferencesTask.getId(), preferences);

        // Verify onboarding completion
        JsonNode onboardingEvent = consumeKafkaMessage("beneficiario.onboarding", Duration.ofSeconds(5));
        assertThat(onboardingEvent.get("canalPreferencial").asText()).isEqualTo("WHATSAPP");

        // ==================== PHASE 3: INTERAÇÃO ====================

        // Simulate beneficiary interaction via WhatsApp
        Map<String, Object> interacaoVariables = new HashMap<>();
        interacaoVariables.put("beneficiarioId", "BEN123456");
        interacaoVariables.put("canal", "WHATSAPP");
        interacaoVariables.put("mensagem", "Preciso solicitar uma autorização para consulta");
        interacaoVariables.put("urgencia", "NORMAL");

        ProcessInstance interacaoProcess = runtimeService
            .startProcessInstanceByKey("SUB-003-Recepcao-Triagem", interacaoVariables);

        // Wait for NLP classification
        stubFor(post(urlEqualTo("/api/nlp/classify"))
            .willReturn(aOk()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"intent\": \"SOLICITAR_AUTORIZACAO\", \"confidence\": 0.95}")));

        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Interacao interacao = interacaoRepository.findByBeneficiarioIdAndProcessInstanceId(
                    "BEN123456", interacaoProcess.getId()
                ).orElseThrow();
                assertThat(interacao.getIntencao()).isEqualTo("SOLICITAR_AUTORIZACAO");
                assertThat(interacao.getCanal()).isEqualTo(CanalAtendimento.WHATSAPP);
            });

        // Verify interaction event
        JsonNode interacaoEvent = consumeKafkaMessage("interacao.received", Duration.ofSeconds(5));
        assertThat(interacaoEvent.get("intent").asText()).isEqualTo("SOLICITAR_AUTORIZACAO");

        // ==================== PHASE 4: AUTORIZAÇÃO ====================

        // Interaction should trigger authorization subprocess
        Map<String, Object> autorizacaoVariables = new HashMap<>();
        autorizacaoVariables.put("beneficiarioId", "BEN123456");
        autorizacaoVariables.put("tipoSolicitacao", "CONSULTA");
        autorizacaoVariables.put("especialidade", "CARDIOLOGIA");
        autorizacaoVariables.put("prestadorId", "PREST001");
        autorizacaoVariables.put("valorEstimado", 250.00);

        // Mock Tasy ERP for authorization validation
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/validar"))
            .willReturn(aOk()
                .withBody("{\"aprovada\": true, \"numeroGuia\": \"GUIA123456\", \"validadeAte\": \"2025-12-31\"}")));

        ProcessInstance autorizacaoProcess = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", autorizacaoVariables);

        // Wait for auto-approval (value < threshold)
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(
                    autorizacaoProcess.getId()
                ).orElseThrow();
                assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacao.APROVADA);
                assertThat(autorizacao.getNumeroGuia()).isEqualTo("GUIA123456");
            });

        // Verify authorization events
        JsonNode autorizacaoSolicitadaEvent = consumeKafkaMessage("autorizacao.solicitada", Duration.ofSeconds(5));
        assertThat(autorizacaoSolicitadaEvent.get("tipoSolicitacao").asText()).isEqualTo("CONSULTA");

        JsonNode autorizacaoAprovadaEvent = consumeKafkaMessage("autorizacao.aprovada", Duration.ofSeconds(5));
        assertThat(autorizacaoAprovadaEvent.get("numeroGuia").asText()).isEqualTo("GUIA123456");

        // ==================== PHASE 5: FOLLOW-UP ====================

        // Authorization approval should trigger follow-up
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<FollowUp> followUps = followUpRepository.findByBeneficiarioIdAndTipo(
                    "BEN123456", TipoFollowUp.POS_AUTORIZACAO
                );
                assertThat(followUps).isNotEmpty();
                assertThat(followUps.get(0).getStatus()).isEqualTo(StatusFollowUp.AGENDADO);
            });

        // Verify follow-up event
        JsonNode followUpEvent = consumeKafkaMessage("followup.agendado", Duration.ofSeconds(5));
        assertThat(followUpEvent.get("tipo").asText()).isEqualTo("POS_AUTORIZACAO");

        // Simulate follow-up call after 48h
        FollowUp followUp = followUpRepository.findByBeneficiarioIdAndTipo(
            "BEN123456", TipoFollowUp.POS_AUTORIZACAO
        ).get(0);

        Map<String, Object> followUpVariables = new HashMap<>();
        followUpVariables.put("followUpId", followUp.getId());
        followUpVariables.put("realizouConsulta", true);
        followUpVariables.put("satisfacao", 5);
        followUpVariables.put("observacoes", "Atendimento excelente, médico muito atencioso");

        ProcessInstance followUpProcess = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", followUpVariables);

        taskService.complete(
            taskService.createTaskQuery()
                .processInstanceId(followUpProcess.getId())
                .taskDefinitionKey("registrar-feedback")
                .singleResult()
                .getId(),
            followUpVariables
        );

        // Verify follow-up completion
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                FollowUp completedFollowUp = followUpRepository.findById(followUp.getId()).orElseThrow();
                assertThat(completedFollowUp.getStatus()).isEqualTo(StatusFollowUp.CONCLUIDO);
                assertThat(completedFollowUp.getSatisfacao()).isEqualTo(5);
            });

        // ==================== FINAL VALIDATION ====================

        // Verify complete event chain
        List<Evento> eventos = eventoRepository.findByBeneficiarioIdOrderByDataHoraAsc("BEN123456");
        assertThat(eventos).hasSizeGreaterThanOrEqualTo(5);
        assertThat(eventos)
            .extracting(Evento::getTipo)
            .containsSequence(
                TipoEvento.ADESAO_COMPLETA,
                TipoEvento.ONBOARDING_COMPLETO,
                TipoEvento.INTERACAO_RECEBIDA,
                TipoEvento.AUTORIZACAO_APROVADA,
                TipoEvento.FOLLOWUP_CONCLUIDO
            );

        // Verify all process instances completed successfully
        assertThat(runtimeService.createProcessInstanceQuery()
            .processInstanceId(adesaoProcess.getId())
            .singleResult())
            .isNull(); // Completed

        // Verify beneficiary state
        Beneficiario finalBeneficiario = beneficiarioRepository.findByCpf("12345678901").orElseThrow();
        assertThat(finalBeneficiario.getStatus()).isEqualTo(StatusBeneficiario.ATIVO);
        assertThat(finalBeneficiario.getCanalPreferencial()).isEqualTo(CanalAtendimento.WHATSAPP);

        // Verify saga completion event
        JsonNode sagaEvent = consumeKafkaMessage("saga.beneficiario.completed", Duration.ofSeconds(5));
        assertThat(sagaEvent.get("beneficiarioId").asText()).isEqualTo("BEN123456");
        assertThat(sagaEvent.get("etapas").size()).isGreaterThanOrEqualTo(5);

        // Verify all external API calls made
        verify(1, postRequestedFor(urlEqualTo("/api/tasy/beneficiario/validar")));
        verify(atLeastOnce(), postRequestedFor(urlEqualTo("/api/sms/send")));
        verify(atLeastOnce(), postRequestedFor(urlEqualTo("/api/email/send")));
        verify(1, postRequestedFor(urlEqualTo("/api/tasy/autorizacao/validar")));
    }

    @Test
    @DisplayName("Should handle journey with chronic disease enrollment")
    void shouldHandleJourneyWithChronicDisease() throws Exception {
        // Given: Beneficiary with chronic condition
        Map<String, Object> variables = new HashMap<>();
        variables.put("nome", "Maria Santos");
        variables.put("cpf", "98765432100");
        variables.put("email", "maria.santos@email.com");
        variables.put("telefone", "+5511888888888");
        variables.put("dataNascimento", "1970-03-20");
        variables.put("plano", "PLATINUM");
        variables.put("condicaoCronica", "DIABETES_TIPO2");

        // Mock ERP validation
        stubFor(post(urlEqualTo("/api/tasy/beneficiario/validar"))
            .willReturn(aOk()
                .withBody("{\"valido\": true, \"codigo\": \"BEN789012\"}")));

        // When: Start adhesion with chronic flag
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-001-Adesao-Onboarding", variables);

        // Then: Should trigger chronic disease subprocess
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                List<ProcessInstance> chronicProcesses = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey("SUB-008-Cronicos-Gerenciamento")
                    .list();
                assertThat(chronicProcesses).isNotEmpty();
            });

        // Verify beneficiary enrolled in chronic program
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Beneficiario beneficiario = beneficiarioRepository.findByCpf("98765432100").orElseThrow();
                assertThat(beneficiario.getProgramaCronico()).isEqualTo("DIABETES_TIPO2");
                assertThat(beneficiario.getStatus()).isEqualTo(StatusBeneficiario.ATIVO);
            });
    }

    @Test
    @DisplayName("Should handle journey interruption and compensation")
    void shouldHandleJourneyInterruptionAndCompensation() throws Exception {
        // Given: Adhesion process started
        Map<String, Object> variables = new HashMap<>();
        variables.put("nome", "Carlos Oliveira");
        variables.put("cpf", "45678912300");
        variables.put("email", "carlos@email.com");

        // Mock ERP validation failure
        stubFor(post(urlEqualTo("/api/tasy/beneficiario/validar"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\": \"ERP unavailable\"}")));

        // When: Start process with failing external service
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-001-Adesao-Onboarding", variables);

        // Then: Should trigger compensation/retry logic
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Task> errorTasks = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("tratar-erro-validacao")
                    .list();
                assertThat(errorTasks).isNotEmpty();
            });

        // Verify error event captured
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<Evento> eventos = eventoRepository.findByTipo(TipoEvento.ERRO_INTEGRACAO);
                assertThat(eventos).isNotEmpty();
            });
    }

    @Test
    @DisplayName("Should maintain state consistency across subprocess failures")
    void shouldMaintainStateConsistencyAcrossSubprocessFailures() throws Exception {
        // Given: Complete adhesion
        Beneficiario beneficiario = TestDataBuilder.buildBeneficiario()
            .cpf("11122233344")
            .nome("Ana Paula")
            .codigoExterno("BEN999999")
            .build();
        beneficiarioRepository.save(beneficiario);

        // When: Authorization fails mid-process
        Map<String, Object> autorizacaoVariables = new HashMap<>();
        autorizacaoVariables.put("beneficiarioId", "BEN999999");
        autorizacaoVariables.put("tipoSolicitacao", "CIRURGIA");
        autorizacaoVariables.put("valorEstimado", 50000.00);

        // Mock Tasy timeout
        stubFor(post(urlEqualTo("/api/tasy/autorizacao/validar"))
            .willReturn(aResponse()
                .withStatus(504)
                .withFixedDelay(30000)));

        ProcessInstance autorizacaoProcess = runtimeService
            .startProcessInstanceByKey("SUB-006-Autorizacao-Gestao", autorizacaoVariables);

        // Then: Should handle timeout gracefully
        await().atMost(Duration.ofSeconds(35))
            .untilAsserted(() -> {
                Autorizacao autorizacao = autorizacaoRepository.findByProcessInstanceId(
                    autorizacaoProcess.getId()
                ).orElseThrow();
                assertThat(autorizacao.getStatus())
                    .isIn(StatusAutorizacao.PENDENTE, StatusAutorizacao.EM_ANALISE);
            });

        // Verify beneficiary state unchanged
        Beneficiario updatedBeneficiario = beneficiarioRepository.findByCpf("11122233344").orElseThrow();
        assertThat(updatedBeneficiario.getStatus()).isEqualTo(StatusBeneficiario.ATIVO);
    }
}
