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
 * E2E Integration Test for Chronic Disease Management Journey
 *
 * Tests the complete chronic disease management flow:
 * 1. Program Enrollment - Patient onboarding and assessment
 * 2. Goal Setting - Personalized health goals
 * 3. Monitoring - Continuous health tracking
 * 4. Interventions - Medication adherence, lifestyle coaching
 * 5. Adjustments - Plan modifications based on progress
 *
 * Scenarios:
 * - Diabetes Type 2 management
 * - Hypertension monitoring
 * - Medication adherence tracking
 * - Health marker collection (glucose, blood pressure)
 * - Progress assessment and plan adjustments
 *
 * @see PROMPT_TECNICO_3.MD Lines 1024-1078 (SUB-008)
 */
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    "cronico.inscrito",
    "cronico.meta_definida",
    "cronico.medicao_registrada",
    "cronico.alerta_saude",
    "cronico.progresso_avaliado"
})
@DisplayName("E2E: Chronic Disease Management Journey")
public class CronicoE2EIT extends BaseIntegrationTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProgramaCronicoRepository programaCronicoRepository;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private MedicaoSaudeRepository medicaoSaudeRepository;

    @Autowired
    private MetaSaudeRepository metaSaudeRepository;

    @Autowired
    private AlertaSaudeRepository alertaSaudeRepository;

    @Autowired
    private EventoRepository eventoRepository;

    private Beneficiario beneficiario;

    @BeforeEach
    void setupTestData() {
        beneficiario = TestDataBuilder.buildBeneficiario()
            .cpf("12345678901")
            .nome("Maria Santos")
            .codigoExterno("BEN123456")
            .dataNascimento(LocalDateTime.of(1970, 5, 15, 0, 0))
            .build();
        beneficiarioRepository.save(beneficiario);
    }

    @Test
    @DisplayName("Should complete diabetes Type 2 enrollment and monitoring")
    void shouldCompleteDiabetesEnrollmentAndMonitoring() throws Exception {
        // ==================== PHASE 1: ENROLLMENT ====================

        // Given: Beneficiary diagnosed with Diabetes Type 2
        Map<String, Object> enrollmentVariables = new HashMap<>();
        enrollmentVariables.put("beneficiarioId", "BEN123456");
        enrollmentVariables.put("condicao", "DIABETES_TIPO2");
        enrollmentVariables.put("cid10", "E11");
        enrollmentVariables.put("medicoResponsavel", "CRM12345");
        enrollmentVariables.put("datadiagnostico", LocalDateTime.now().minusMonths(3));

        // Mock clinical data from Tasy
        stubFor(get(urlMatching("/api/tasy/beneficiario/BEN123456/historico-clinico"))
            .willReturn(aOk()
                .withBody("{\"hba1c\": 8.5, \"glicemiaJejum\": 145, \"peso\": 82, \"altura\": 1.68}")));

        // When: Enroll in chronic program
        ProcessInstance enrollmentProcess = runtimeService
            .startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", enrollmentVariables);

        // Then: Program enrollment created
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ProgramaCronico programa = programaCronicoRepository.findByBeneficiarioIdAndCondicao(
                    "BEN123456", "DIABETES_TIPO2"
                ).orElseThrow();
                assertThat(programa.getStatus()).isEqualTo(StatusPrograma.ATIVO);
                assertThat(programa.getDataInscricao()).isNotNull();
            });

        // Verify enrollment event
        JsonNode inscritoEvent = consumeKafkaMessage("cronico.inscrito", Duration.ofSeconds(5));
        assertThat(inscritoEvent.get("condicao").asText()).isEqualTo("DIABETES_TIPO2");

        // ==================== PHASE 2: GOAL SETTING ====================

        // Wait for initial assessment task
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task assessmentTask = taskService.createTaskQuery()
                    .processInstanceId(enrollmentProcess.getId())
                    .taskDefinitionKey("definir-metas")
                    .singleResult();
                assertThat(assessmentTask).isNotNull();
            });

        // Complete goal setting
        Task goalSettingTask = taskService.createTaskQuery()
            .processInstanceId(enrollmentProcess.getId())
            .taskDefinitionKey("definir-metas")
            .singleResult();

        Map<String, Object> goals = new HashMap<>();
        goals.put("metas", List.of(
            Map.of("tipo", "HBA1C", "valorAlvo", 7.0, "prazo", LocalDateTime.now().plusMonths(3)),
            Map.of("tipo", "GLICEMIA_JEJUM", "valorAlvo", 110.0, "prazo", LocalDateTime.now().plusMonths(3)),
            Map.of("tipo", "PESO", "valorAlvo", 75.0, "prazo", LocalDateTime.now().plusMonths(6))
        ));
        goals.put("frequenciaMonitoramento", "SEMANAL");

        taskService.complete(goalSettingTask.getId(), goals);

        // Verify goals created
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<MetaSaude> metas = metaSaudeRepository.findByBeneficiarioId("BEN123456");
                assertThat(metas).hasSize(3);
                assertThat(metas)
                    .extracting(MetaSaude::getTipo)
                    .contains("HBA1C", "GLICEMIA_JEJUM", "PESO");
            });

        // Verify goal event
        JsonNode metaEvent = consumeKafkaMessage("cronico.meta_definida", Duration.ofSeconds(5));
        assertThat(metaEvent.get("quantidade").asInt()).isEqualTo(3);

        // ==================== PHASE 3: MONITORING ====================

        // Simulate patient submitting glucose measurements
        Map<String, Object> medicaoVariables = new HashMap<>();
        medicaoVariables.put("beneficiarioId", "BEN123456");
        medicaoVariables.put("tipo", "GLICEMIA_JEJUM");
        medicaoVariables.put("valor", 135.0);
        medicaoVariables.put("dataHora", LocalDateTime.now());
        medicaoVariables.put("origem", "APP_MOBILE");

        ProcessInstance medicaoProcess = runtimeService
            .startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", medicaoVariables);

        // Verify measurement recorded
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<MedicaoSaude> medicoes = medicaoSaudeRepository.findByBeneficiarioIdAndTipo(
                    "BEN123456", "GLICEMIA_JEJUM"
                );
                assertThat(medicoes).isNotEmpty();
                assertThat(medicoes.get(0).getValor()).isEqualTo(135.0);
            });

        // Verify measurement event
        JsonNode medicaoEvent = consumeKafkaMessage("cronico.medicao_registrada", Duration.ofSeconds(5));
        assertThat(medicaoEvent.get("tipo").asText()).isEqualTo("GLICEMIA_JEJUM");
        assertThat(medicaoEvent.get("valor").asDouble()).isEqualTo(135.0);

        // ==================== PHASE 4: ALERT GENERATION ====================

        // Simulate high glucose reading
        Map<String, Object> highGlucoseVariables = new HashMap<>();
        highGlucoseVariables.put("beneficiarioId", "BEN123456");
        highGlucoseVariables.put("tipo", "GLICEMIA_JEJUM");
        highGlucoseVariables.put("valor", 210.0); // High value
        highGlucoseVariables.put("dataHora", LocalDateTime.now());

        runtimeService.startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", highGlucoseVariables);

        // Mock notification service
        stubFor(post(urlEqualTo("/api/sms/send"))
            .willReturn(aOk().withBody("{\"messageId\": \"SMS123\", \"status\": \"sent\"}")));

        // Verify health alert generated
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<AlertaSaude> alertas = alertaSaudeRepository.findByBeneficiarioId("BEN123456");
                assertThat(alertas).isNotEmpty();
                assertThat(alertas.get(0).getTipo()).isEqualTo(TipoAlerta.GLICEMIA_ALTA);
                assertThat(alertas.get(0).getGravidade()).isEqualTo(GravidadeAlerta.MEDIA);
            });

        // Verify alert event
        JsonNode alertaEvent = consumeKafkaMessage("cronico.alerta_saude", Duration.ofSeconds(5));
        assertThat(alertaEvent.get("tipo").asText()).isEqualTo("GLICEMIA_ALTA");

        // ==================== PHASE 5: PROGRESS ASSESSMENT ====================

        // Simulate monthly progress review
        Map<String, Object> assessmentVariables = new HashMap<>();
        assessmentVariables.put("beneficiarioId", "BEN123456");
        assessmentVariables.put("programaId", programaCronicoRepository
            .findByBeneficiarioIdAndCondicao("BEN123456", "DIABETES_TIPO2")
            .get().getId());

        ProcessInstance assessmentProcess = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", assessmentVariables);

        // Complete assessment task
        Task reviewTask = taskService.createTaskQuery()
            .processInstanceId(assessmentProcess.getId())
            .taskDefinitionKey("avaliar-progresso")
            .singleResult();

        Map<String, Object> progressData = new HashMap<>();
        progressData.put("hba1cAtual", 7.8); // Improved from 8.5
        progressData.put("pesoAtual", 79.0); // Improved from 82
        progressData.put("aderenciaMedicacao", 85.0);
        progressData.put("metasAtingidas", 1); // Weight goal on track
        progressData.put("ajustarPlano", false);

        taskService.complete(reviewTask.getId(), progressData);

        // Verify progress recorded
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ProgramaCronico programa = programaCronicoRepository.findByBeneficiarioIdAndCondicao(
                    "BEN123456", "DIABETES_TIPO2"
                ).orElseThrow();
                assertThat(programa.getAderencia()).isEqualTo(85.0);
                assertThat(programa.getUltimaAvaliacao()).isNotNull();
            });

        // Verify progress event
        JsonNode progressoEvent = consumeKafkaMessage("cronico.progresso_avaliado", Duration.ofSeconds(5));
        assertThat(progressoEvent.get("hba1cAtual").asDouble()).isEqualTo(7.8);
        assertThat(progressoEvent.get("aderenciaMedicacao").asDouble()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("Should handle medication adherence monitoring")
    void shouldHandleMedicationAdherence() throws Exception {
        // Given: Enrolled patient with medication plan
        ProgramaCronico programa = TestDataBuilder.buildProgramaCronico()
            .beneficiarioId("BEN123456")
            .condicao("DIABETES_TIPO2")
            .status(StatusPrograma.ATIVO)
            .build();
        programaCronicoRepository.save(programa);

        // When: Track medication adherence
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("programaId", programa.getId());
        variables.put("medicacoes", List.of(
            Map.of("nome", "Metformina", "tomado", true, "horario", "08:00"),
            Map.of("nome", "Glibenclamida", "tomado", false, "horario", "14:00")
        ));
        variables.put("data", LocalDateTime.now());

        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", variables);

        // Then: Adherence tracked and alert generated for missed dose
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<AlertaSaude> alertas = alertaSaudeRepository.findByBeneficiarioId("BEN123456");
                assertThat(alertas)
                    .anyMatch(a -> a.getTipo() == TipoAlerta.MEDICACAO_NAO_TOMADA);
            });

        // Verify adherence percentage calculated
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                ProgramaCronico updatedPrograma = programaCronicoRepository.findById(programa.getId())
                    .orElseThrow();
                assertThat(updatedPrograma.getAderenciaMedicacao()).isEqualTo(50.0); // 1 of 2
            });
    }

    @Test
    @DisplayName("Should trigger intervention for poor adherence")
    void shouldTriggerInterventionForPoorAdherence() throws Exception {
        // Given: Patient with poor medication adherence
        ProgramaCronico programa = TestDataBuilder.buildProgramaCronico()
            .beneficiarioId("BEN123456")
            .condicao("DIABETES_TIPO2")
            .aderenciaMedicacao(35.0) // Low adherence
            .build();
        programaCronicoRepository.save(programa);

        // When: Weekly adherence check runs
        Map<String, Object> variables = new HashMap<>();
        variables.put("programaId", programa.getId());
        variables.put("aderenciaSemanal", 35.0);

        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", variables);

        // Then: Should trigger nurse intervention
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Task interventionTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("intervencao-enfermagem")
                    .singleResult();
                assertThat(interventionTask).isNotNull();
                assertThat(interventionTask.getAssignee()).contains("enfermeiro");
            });
    }

    @Test
    @DisplayName("Should adjust plan based on poor progress")
    void shouldAdjustPlanBasedOnProgress() throws Exception {
        // Given: Patient not meeting goals after 3 months
        ProgramaCronico programa = TestDataBuilder.buildProgramaCronico()
            .beneficiarioId("BEN123456")
            .condicao("DIABETES_TIPO2")
            .dataInscricao(LocalDateTime.now().minusMonths(3))
            .build();
        programaCronicoRepository.save(programa);

        // When: Assessment shows no progress
        Map<String, Object> variables = new HashMap<>();
        variables.put("programaId", programa.getId());
        variables.put("hba1cInicial", 8.5);
        variables.put("hba1cAtual", 8.7); // Worse
        variables.put("metasAtingidas", 0);

        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-010-FollowUp-Pesquisa", variables);

        // Complete assessment
        Task assessmentTask = taskService.createTaskQuery()
            .processInstanceId(process.getId())
            .taskDefinitionKey("avaliar-progresso")
            .singleResult();

        Map<String, Object> assessment = new HashMap<>();
        assessment.put("progressoInsuficiente", true);
        assessment.put("ajustarPlano", true);
        assessment.put("novasIntervencoes", List.of(
            "Consulta com nutricionista",
            "Ajuste medicação - encaminhar endocrinologista"
        ));

        taskService.complete(assessmentTask.getId(), assessment);

        // Then: Plan should be updated
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ProgramaCronico updatedPrograma = programaCronicoRepository.findById(programa.getId())
                    .orElseThrow();
                assertThat(updatedPrograma.getIntervencoesAtivas())
                    .contains("nutricionista", "endocrinologista");
            });
    }

    @Test
    @DisplayName("Should integrate wearable device data")
    void shouldIntegrateWearableData() throws Exception {
        // Given: Patient with connected wearable device
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("dispositivoId", "FITBIT123");

        // Mock wearable API
        stubFor(get(urlMatching("/api/wearable/FITBIT123/dados"))
            .willReturn(aOk()
                .withBody("{\"passos\": 8500, \"frequenciaCardiaca\": 78, \"sono\": 7.5}")));

        // When: Sync wearable data
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", variables);

        // Then: Data should be imported and analyzed
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<MedicaoSaude> medicoes = medicaoSaudeRepository.findByBeneficiarioIdAndOrigem(
                    "BEN123456", "WEARABLE"
                );
                assertThat(medicoes).isNotEmpty();
                assertThat(medicoes)
                    .extracting(MedicaoSaude::getTipo)
                    .contains("PASSOS", "FREQUENCIA_CARDIACA", "SONO");
            });
    }

    @Test
    @DisplayName("Should handle hypertension monitoring with multiple markers")
    void shouldHandleHypertensionMonitoring() throws Exception {
        // Given: Hypertension program enrollment
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("condicao", "HIPERTENSAO");
        variables.put("cid10", "I10");

        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", variables);

        // When: Patient submits blood pressure readings
        Map<String, Object> bpVariables = new HashMap<>();
        bpVariables.put("beneficiarioId", "BEN123456");
        bpVariables.put("tipo", "PRESSAO_ARTERIAL");
        bpVariables.put("sistolica", 145);
        bpVariables.put("diastolica", 92);
        bpVariables.put("dataHora", LocalDateTime.now());

        runtimeService.startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", bpVariables);

        // Then: Reading analyzed and categorized
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<MedicaoSaude> medicoes = medicaoSaudeRepository.findByBeneficiarioIdAndTipo(
                    "BEN123456", "PRESSAO_ARTERIAL"
                );
                assertThat(medicoes).isNotEmpty();
                assertThat(medicoes.get(0).getClassificacao()).isEqualTo("HIPERTENSAO_ESTAGIO_1");
            });

        // Verify alert for elevated reading
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<AlertaSaude> alertas = alertaSaudeRepository.findByBeneficiarioId("BEN123456");
                assertThat(alertas)
                    .anyMatch(a -> a.getTipo() == TipoAlerta.PRESSAO_ELEVADA);
            });
    }

    @Test
    @DisplayName("Should coordinate care team for complex cases")
    void shouldCoordinateCareTeam() throws Exception {
        // Given: Complex patient with multiple conditions
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN123456");
        variables.put("condicoes", List.of("DIABETES_TIPO2", "HIPERTENSAO", "OBESIDADE"));
        variables.put("complexidade", "ALTA");

        // When: Enroll in coordinated care program
        ProcessInstance process = runtimeService
            .startProcessInstanceByKey("SUB-008-Cronicos-Gerenciamento", variables);

        // Then: Care team assembled
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ProgramaCronico programa = programaCronicoRepository.findByBeneficiarioIdAndCondicao(
                    "BEN123456", "DIABETES_TIPO2"
                ).orElseThrow();
                assertThat(programa.getEquipeResponsavel())
                    .contains("medico", "enfermeiro", "nutricionista");
            });

        // Verify coordination task created
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Task coordTask = taskService.createTaskQuery()
                    .processInstanceId(process.getId())
                    .taskDefinitionKey("coordenar-cuidado")
                    .singleResult();
                assertThat(coordTask).isNotNull();
            });
    }
}
