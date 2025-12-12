package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.ProgramaCronicoDTO;
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
 * Integration tests for SUB-008: Gestão de Crônicos workflow.
 *
 * Tests cover:
 * - Program enrollment and eligibility
 * - Therapeutic goal setting
 * - Progress monitoring and tracking
 * - Care plan adjustments
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"cronico.inscrito", "cronico.progresso"})
@DisplayName("SUB-008: Gestão de Crônicos - Workflow Integration Tests")
class GestaoCronicosWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-008_Gestao_Cronicos";

    @Test
    @DisplayName("Deve inscrever beneficiário em programa de gestão de crônicos")
    void deveInscreverBeneficiarioProgramaCronicos() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("condicaoCronica", "DIABETES_TIPO2");
        variables.put("elegivel", true);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Boolean inscritoPrograma = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("inscritoPrograma")
                .singleResult().getValue();

        assertThat(inscritoPrograma).isTrue();
    }

    @Test
    @DisplayName("Deve estabelecer metas terapêuticas personalizadas")
    void deveEstabelecerMetasTerapeuticasPersonalizadas() {
        // Given
        ProgramaCronicoDTO programa = TestDataBuilder.createProgramaCronico()
                .withBeneficiarioId("BEN-001")
                .withCondicao("HIPERTENSAO")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("programa", programa);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        List<String> metasTerapeuticas = (List<String>) runtimeService.getVariable(
                instance.getId(), "metasTerapeuticas");

        assertThat(metasTerapeuticas).isNotEmpty();
        assertThat(metasTerapeuticas).contains("Pressão arterial < 140/90 mmHg");
    }

    @Test
    @DisplayName("Deve monitorar progresso e coletar indicadores de saúde")
    void deveMonitorarProgressoColetarIndicadores() {
        // Given
        Map<String, Object> variables = TestDataBuilder.createGestaoCronicosVariables()
                .withBeneficiarioId("BEN-001")
                .withIndicadores("glicemia", 120.0)
                .withIndicadores("hba1c", 6.5)
                .build();

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Map<String, Object> progressoMonitorado = (Map<String, Object>)
                runtimeService.getVariable(instance.getId(), "progressoMonitorado");

        assertThat(progressoMonitorado).containsKeys("glicemia", "hba1c");
    }

    @Test
    @DisplayName("Deve ajustar plano de cuidado quando metas não são atingidas")
    void deveAjustarPlanoQuandoMetasNaoAtingidas() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("metasAtingidas", false);
        variables.put("indicadorForaAlvo", true);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isWaitingAt("UserTask_AjustePlano");

        Boolean necessitaAjuste = (Boolean) runtimeService.getVariable(instance.getId(), "necessitaAjuste");
        assertThat(necessitaAjuste).isTrue();
    }

    @Test
    @DisplayName("Deve enviar alertas quando indicadores críticos são detectados")
    void deveEnviarAlertasIndicadoresCriticos() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("glicemia", 300.0);  // Valor crítico
        variables.put("pressaoArterial", "180/110");  // Hipertensão severa

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Boolean alertaEnviado = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("alertaEnviado")
                .singleResult().getValue();

        String nivelAlerta = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("nivelAlerta")
                .singleResult().getValue();

        assertThat(alertaEnviado).isTrue();
        assertThat(nivelAlerta).isEqualTo("CRITICO");
    }
}
