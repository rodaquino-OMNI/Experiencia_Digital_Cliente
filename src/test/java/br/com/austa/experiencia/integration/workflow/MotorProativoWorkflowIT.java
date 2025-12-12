package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.BeneficiarioDTO;
import br.com.austa.experiencia.model.dto.TriggerProativoDTO;
import br.com.austa.experiencia.utils.TestDataBuilder;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Integration tests for SUB-002: Motor Proativo workflow.
 *
 * Tests cover:
 * - Batch processing of beneficiaries
 * - Trigger identification and scoring
 * - Proactive action execution
 * - Dashboard updates and notifications
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"motor.proativo.trigger", "motor.proativo.acao"})
@DisplayName("SUB-002: Motor Proativo - Workflow Integration Tests")
class MotorProativoWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("experiencia_test");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-002_Motor_Proativo";

    @Test
    @DisplayName("Deve processar lote de beneficiários e identificar triggers")
    void deveProcessarLoteBeneficiariosIdentificarTriggers() {
        // Given
        List<BeneficiarioDTO> loteBeneficiarios = Arrays.asList(
                TestDataBuilder.createBeneficiario().withId("BEN-001").build(),
                TestDataBuilder.createBeneficiario().withId("BEN-002").build(),
                TestDataBuilder.createBeneficiario().withId("BEN-003").build()
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("loteBeneficiarios", loteBeneficiarios);
        variables.put("dataProcessamento", LocalDate.now());

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        List<TriggerProativoDTO> triggersIdentificados = (List<TriggerProativoDTO>)
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(instance.getId())
                        .variableName("triggersIdentificados")
                        .singleResult().getValue();

        assertThat(triggersIdentificados).isNotEmpty();
    }

    @Test
    @DisplayName("Deve executar ação proativa para exames preventivos atrasados")
    void deveExecutarAcaoProativaExamesPreventivos() {
        // Given
        TriggerProativoDTO trigger = TestDataBuilder.createTriggerProativo()
                .withTipo("EXAMES_PREVENTIVOS_ATRASADOS")
                .withBeneficiarioId("BEN-001")
                .withScore(85)
                .withUrgencia("ALTA")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("trigger", trigger);
        variables.put("beneficiarioId", "BEN-001");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String acaoExecutada = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("acaoExecutada")
                .singleResult().getValue();

        assertThat(acaoExecutada).isEqualTo("NOTIFICACAO_WHATSAPP_ENVIADA");
    }

    @Test
    @DisplayName("Deve priorizar triggers por score e urgência")
    void devePriorizarTriggersPorScoreUrgencia() {
        // Given
        List<TriggerProativoDTO> triggers = Arrays.asList(
                TestDataBuilder.createTriggerProativo()
                        .withScore(95).withUrgencia("CRITICA").build(),
                TestDataBuilder.createTriggerProativo()
                        .withScore(70).withUrgencia("MEDIA").build(),
                TestDataBuilder.createTriggerProativo()
                        .withScore(50).withUrgencia("BAIXA").build()
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("triggersIdentificados", triggers);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        List<TriggerProativoDTO> triggersPriorizados = (List<TriggerProativoDTO>)
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(instance.getId())
                        .variableName("triggersPriorizados")
                        .singleResult().getValue();

        assertThat(triggersPriorizados.get(0).getScore()).isEqualTo(95);
        assertThat(triggersPriorizados.get(0).getUrgencia()).isEqualTo("CRITICA");
    }

    @Test
    @DisplayName("Deve atualizar dashboard com resultados das ações proativas")
    void deveAtualizarDashboardComResultados() {
        // Given
        Map<String, Object> variables = TestDataBuilder.createMotorProativoVariables()
                .withAcoesExecutadas(5)
                .withSuccessRate(0.8)
                .build();

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        Boolean dashboardAtualizado = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("dashboardAtualizado")
                .singleResult().getValue();

        assertThat(dashboardAtualizado).isTrue();
    }

    @Test
    @DisplayName("Deve lidar com falha na execução de ação proativa")
    void deveLidarComFalhaExecucaoAcao() {
        // Given
        TriggerProativoDTO trigger = TestDataBuilder.createTriggerProativo()
                .withTipo("NOTIFICACAO_INVALIDA")
                .withBeneficiarioId("BEN-INVALIDO")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("trigger", trigger);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String statusExecucao = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("statusExecucao")
                .singleResult().getValue();

        assertThat(statusExecucao).isEqualTo("FALHA");
    }
}
