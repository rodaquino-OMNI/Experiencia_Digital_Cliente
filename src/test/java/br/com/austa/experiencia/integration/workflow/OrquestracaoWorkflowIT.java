package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Integration tests for PROC-ORC-001: Orquestração Central workflow.
 *
 * Tests cover:
 * - Message routing to appropriate subprocesses
 * - Subprocess orchestration and coordination
 * - State management across workflows
 * - Complete lifecycle from reception to resolution
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"orquestracao.iniciada", "orquestracao.concluida"})
@DisplayName("PROC-ORC-001: Orquestração Central - Workflow Integration Tests")
class OrquestracaoWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "PROC-ORC-001_Orquestracao_Central";

    @Test
    @DisplayName("Deve rotear mensagem para subprocess apropriado com base no tipo")
    void deveRotearMensagemParaSubprocessApropriado() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("tipoSolicitacao", "AUTORIZACAO");
        variables.put("beneficiarioId", "BEN-001");
        variables.put("canal", "WHATSAPP");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        String subprocessRoteado = (String) runtimeService.getVariable(instance.getId(), "subprocessRoteado");
        assertThat(subprocessRoteado).isEqualTo("SUB-006_Autorizacao_Inteligente");
    }

    @Test
    @DisplayName("Deve orquestrar múltiplos subprocessos em sequência")
    void deveOrquestrarMultiplosSubprocessosSequencia() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("fluxoCompleto", Arrays.asList(
                "SUB-001_Onboarding",
                "SUB-002_Motor_Proativo",
                "SUB-010_Feedback"
        ));

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        List<String> subprocessosExecutados = (List<String>) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("subprocessosExecutados")
                .singleResult().getValue();

        assertThat(subprocessosExecutados).hasSize(3);
    }

    @Test
    @DisplayName("Deve gerenciar estado compartilhado entre subprocessos")
    void deveGerenciarEstadoCompartilhadoEntreSubprocessos() {
        // Given
        Map<String, Object> estadoInicial = new HashMap<>();
        estadoInicial.put("beneficiarioId", "BEN-001");
        estadoInicial.put("contextoAtendimento", "AUTORIZACAO");

        Map<String, Object> variables = new HashMap<>();
        variables.put("estadoCompartilhado", estadoInicial);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Map<String, Object> estadoFinal = (Map<String, Object>)
                runtimeService.getVariable(instance.getId(), "estadoCompartilhado");

        assertThat(estadoFinal).containsKeys("beneficiarioId", "contextoAtendimento");
    }

    @Test
    @DisplayName("Deve completar ciclo de vida completo de solicitação")
    void deveCompletarCicloVidaCompletoSolicitacao() {
        // Given
        Map<String, Object> variables = TestDataBuilder.createOrquestracaoVariables()
                .withBeneficiarioId("BEN-001")
                .withTipoSolicitacao("AUTORIZACAO")
                .withCanal("PORTAL")
                .build();

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String statusFinal = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("statusFinal")
                .singleResult().getValue();

        assertThat(statusFinal).isIn("CONCLUIDA", "RESOLVIDA");
    }

    @Test
    @DisplayName("Deve lidar com erros em subprocessos e aplicar compensação")
    void deveLidarComErrosSubprocessosAplicarCompensacao() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-ERRO");
        variables.put("simularErro", true);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Boolean compensacaoAplicada = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("compensacaoAplicada")
                .singleResult().getValue();

        assertThat(compensacaoAplicada).isTrue();
    }
}
