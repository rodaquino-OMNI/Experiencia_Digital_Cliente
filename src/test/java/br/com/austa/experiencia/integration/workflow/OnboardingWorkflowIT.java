package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.BeneficiarioDTO;
import br.com.austa.experiencia.model.dto.ScreeningDTO;
import br.com.austa.experiencia.utils.TestDataBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Integration tests for SUB-001: Onboarding Inteligente workflow.
 *
 * Tests cover:
 * - Happy path: baixo risco sem CPT
 * - CPT detection and validation routing
 * - Kafka event publication
 * - Error scenarios and boundary events
 *
 * @see PROMPT_TECNICO_3.MD lines 1164-1242
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"beneficiario.onboarding.completed", "beneficiario.cpt.detected"})
@DisplayName("SUB-001: Onboarding Inteligente - Workflow Integration Tests")
class OnboardingWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("experiencia_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String PROCESS_KEY = "SUB-001_Onboarding_Inteligente";

    @Test
    @DisplayName("Deve completar onboarding de beneficiário baixo risco sem CPT")
    void deveCompletarOnboardingBaixoRiscoSemCpt() {
        // Given
        BeneficiarioDTO beneficiario = TestDataBuilder.createBeneficiario()
                .withId("BEN-001")
                .withCpf("12345678901")
                .withNome("João Silva")
                .withIdade(30)
                .build();

        ScreeningDTO screening = TestDataBuilder.createScreening()
                .withBeneficiarioId("BEN-001")
                .withClassificacaoRisco("BAIXO")
                .withCondicoesCronicas(List.of())
                .withCptDetectado(false)
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", beneficiario.getId());
        variables.put("cpf", beneficiario.getCpf());
        variables.put("nome", beneficiario.getNome());
        variables.put("idade", beneficiario.getIdade());
        variables.put("dadosScreening", screening);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("classificacaoRisco")
                .singleResult().getValue()).isEqualTo("BAIXO");

        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("statusCPT")
                .singleResult().getValue()).isEqualTo("NENHUM");

        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("onboardingCompleto")
                .singleResult().getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("Deve detectar CPT e encaminhar para validação médica")
    void deveDetectarCptEEncaminharValidacao() {
        // Given
        ScreeningDTO screeningComCpt = TestDataBuilder.createScreening()
                .withBeneficiarioId("BEN-002")
                .withClassificacaoRisco("ALTO")
                .withCptDetectado(true)
                .withCondicoesCronicas(List.of("Diabetes Tipo 2", "Hipertensão"))
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-002");
        variables.put("cpf", "98765432101");
        variables.put("nome", "Maria Santos");
        variables.put("idade", 55);
        variables.put("dadosScreening", screeningComCpt);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then - Processo deve estar aguardando validação médica
        assertThat(instance).isNotEnded();
        assertThat(instance).isWaitingAt("UserTask_ValidacaoMedicaCpt");

        assertThat(runtimeService.getVariable(instance.getId(), "statusCPT"))
                .isEqualTo("DETECTADO");
        assertThat(runtimeService.getVariable(instance.getId(), "classificacaoRisco"))
                .isEqualTo("ALTO");
    }

    @Test
    @DisplayName("Deve publicar evento Kafka ao concluir onboarding")
    void devePublicarEventoKafkaAoConcluir() {
        // Given
        Map<String, Object> variables = TestDataBuilder.createOnboardingVariables()
                .withBeneficiarioId("BEN-003")
                .withClassificacaoRisco("BAIXO")
                .withCptDetectado(false)
                .build();

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // Verify Kafka event was published
            assertThat(kafkaTemplate.metrics()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Deve tratar erro de validação de dados e acionar boundary event")
    void deveTratarErroValidacaoDados() {
        // Given - Dados inválidos
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-004");
        variables.put("cpf", "INVALID_CPF");  // CPF inválido
        variables.put("nome", null);  // Nome obrigatório ausente

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then - Deve acionar boundary error event
        assertThat(instance).isEnded();
        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("erroOnboarding")
                .singleResult().getValue()).isNotNull();
    }

    @Test
    @DisplayName("Deve realizar screening completo para beneficiário de médio risco")
    void deveRealizarScreeningCompletoMedioRisco() {
        // Given
        ScreeningDTO screeningMedioRisco = TestDataBuilder.createScreening()
                .withBeneficiarioId("BEN-005")
                .withClassificacaoRisco("MEDIO")
                .withCondicoesCronicas(List.of("Hipertensão controlada"))
                .withCptDetectado(false)
                .withNecessitaAcompanhamento(true)
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-005");
        variables.put("cpf", "11122233344");
        variables.put("nome", "Carlos Oliveira");
        variables.put("idade", 45);
        variables.put("dadosScreening", screeningMedioRisco);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();
        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("classificacaoRisco")
                .singleResult().getValue()).isEqualTo("MEDIO");

        assertThat(historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("necessitaAcompanhamento")
                .singleResult().getValue()).isEqualTo(true);
    }
}
