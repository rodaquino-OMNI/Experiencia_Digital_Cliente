package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.FeedbackDTO;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Integration tests for SUB-010: Follow-up e Feedback workflow.
 *
 * Tests cover:
 * - NPS survey sending and scheduling
 * - Response processing and scoring
 * - Detractor recovery workflows
 * - ML model updates based on feedback
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"feedback.recebido", "nps.calculado"})
@DisplayName("SUB-010: Follow-up e Feedback - Workflow Integration Tests")
class FollowUpFeedbackWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-010_FollowUp_Feedback";

    @Test
    @DisplayName("Deve enviar pesquisa NPS após conclusão de atendimento")
    void deveEnviarPesquisaNpsAposConclusao() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("protocoloAtendimento", "PROT-12345");
        variables.put("canalPreferencial", "WHATSAPP");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Boolean pesquisaEnviada = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("pesquisaEnviada")
                .singleResult().getValue();

        assertThat(pesquisaEnviada).isTrue();
    }

    @Test
    @DisplayName("Deve processar resposta NPS e calcular score")
    void deveProcessarRespostaNpsCalcularScore() {
        // Given
        FeedbackDTO feedback = TestDataBuilder.createFeedback()
                .withBeneficiarioId("BEN-001")
                .withNotaNps(9)
                .withComentario("Excelente atendimento")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("feedback", feedback);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        Integer scoreNps = (Integer) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("scoreNps")
                .singleResult().getValue();

        String categoriaCliente = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("categoriaCliente")
                .singleResult().getValue();

        assertThat(scoreNps).isEqualTo(9);
        assertThat(categoriaCliente).isEqualTo("PROMOTOR");
    }

    @Test
    @DisplayName("Deve iniciar workflow de recuperação para detratores")
    void deveIniciarWorkflowRecuperacaoDetratores() {
        // Given
        FeedbackDTO feedbackDetrator = TestDataBuilder.createFeedback()
                .withBeneficiarioId("BEN-002")
                .withNotaNps(3)
                .withComentario("Atendimento péssimo, muito demorado")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("feedback", feedbackDetrator);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isWaitingAt("UserTask_RecuperacaoDetrator");

        String categoriaCliente = (String) runtimeService.getVariable(instance.getId(), "categoriaCliente");
        Boolean necessitaRecuperacao = (Boolean) runtimeService.getVariable(instance.getId(), "necessitaRecuperacao");

        assertThat(categoriaCliente).isEqualTo("DETRATOR");
        assertThat(necessitaRecuperacao).isTrue();
    }

    @Test
    @DisplayName("Deve atualizar modelos ML com base no feedback recebido")
    void deveAtualizarModelosMlComFeedback() {
        // Given
        Map<String, Object> variables = TestDataBuilder.createFeedbackVariables()
                .withMultiplosResponses(50)
                .build();

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        Boolean modeloAtualizado = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("modeloAtualizado")
                .singleResult().getValue();

        assertThat(modeloAtualizado).isTrue();
    }

    @Test
    @DisplayName("Deve agendar follow-up para casos de baixa satisfação")
    void deveAgendarFollowUpBaixaSatisfacao() {
        // Given
        FeedbackDTO feedbackNeutro = TestDataBuilder.createFeedback()
                .withNotaNps(7)
                .withComentario("Razoável")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("feedback", feedbackNeutro);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Boolean followUpAgendado = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("followUpAgendado")
                .singleResult().getValue();

        assertThat(followUpAgendado).isTrue();
    }
}
