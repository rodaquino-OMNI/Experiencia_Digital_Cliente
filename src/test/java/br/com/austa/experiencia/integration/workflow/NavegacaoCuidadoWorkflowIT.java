package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.JornadaCuidadoDTO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Integration tests for SUB-007: Navegação de Cuidado workflow.
 *
 * Tests cover:
 * - Navigator assignment based on complexity
 * - Care journey creation and planning
 * - Status communication and updates
 * - Journey closure and outcomes
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"navegacao.iniciada", "navegacao.concluida"})
@DisplayName("SUB-007: Navegação de Cuidado - Workflow Integration Tests")
class NavegacaoCuidadoWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-007_Navegacao_Cuidado";

    @Test
    @DisplayName("Deve atribuir navegador de cuidado com base na complexidade do caso")
    void deveAtribuirNavegadorComBaseComplexidade() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("complexidadeCaso", "ALTA");
        variables.put("especialidadeRequerida", "ONCOLOGIA");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        String navegadorAtribuido = (String) runtimeService.getVariable(instance.getId(), "navegadorId");
        String especialidadeNavegador = (String) runtimeService.getVariable(instance.getId(), "especialidadeNavegador");

        assertThat(navegadorAtribuido).isNotNull();
        assertThat(especialidadeNavegador).isEqualTo("ONCOLOGIA");
    }

    @Test
    @DisplayName("Deve criar jornada de cuidado personalizada")
    void deveCriarJornadaCuidadoPersonalizada() {
        // Given
        Map<String, Object> variables = TestDataBuilder.createNavegacaoVariables()
                .withBeneficiarioId("BEN-001")
                .withDiagnostico("Diabetes Tipo 2")
                .withObjetivoCuidado("Controle glicêmico")
                .build();

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        JornadaCuidadoDTO jornada = (JornadaCuidadoDTO) runtimeService.getVariable(instance.getId(), "jornada");

        assertThat(jornada).isNotNull();
        assertThat(jornada.getEtapas()).isNotEmpty();
        assertThat(jornada.getObjetivos()).contains("Controle glicêmico");
    }

    @Test
    @DisplayName("Deve comunicar status da jornada ao beneficiário periodicamente")
    void deveComunicarStatusJornadaPeriodicamente() {
        // Given
        JornadaCuidadoDTO jornada = TestDataBuilder.createJornadaCuidado()
                .withBeneficiarioId("BEN-001")
                .withEtapasCompletas(3)
                .withEtapasTotais(10)
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("jornada", jornada);
        variables.put("canalComunicacao", "WHATSAPP");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Boolean comunicacaoEnviada = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("comunicacaoEnviada")
                .singleResult().getValue();

        assertThat(comunicacaoEnviada).isTrue();
    }

    @Test
    @DisplayName("Deve encerrar jornada ao atingir objetivos de cuidado")
    void deveEncerrarJornadaAoAtingirObjetivos() {
        // Given
        JornadaCuidadoDTO jornada = TestDataBuilder.createJornadaCuidado()
                .withBeneficiarioId("BEN-001")
                .withTodosObjetivosAtingidos()
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("jornada", jornada);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String statusJornada = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("statusJornada")
                .singleResult().getValue();

        assertThat(statusJornada).isEqualTo("CONCLUIDA");
    }

    @Test
    @DisplayName("Deve ajustar jornada quando objetivos não são atingidos")
    void deveAjustarJornadaQuandoObjetivosNaoAtingidos() {
        // Given
        JornadaCuidadoDTO jornada = TestDataBuilder.createJornadaCuidado()
                .withBeneficiarioId("BEN-001")
                .withObjetivosNaoAtingidos()
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("jornada", jornada);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isWaitingAt("UserTask_ReplanejamentoJornada");

        Boolean necessitaAjuste = (Boolean) runtimeService.getVariable(instance.getId(), "necessitaAjuste");
        assertThat(necessitaAjuste).isTrue();
    }
}
