package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.ConversaDTO;
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
 * Integration tests for SUB-005: Agentes IA workflow.
 *
 * Tests cover:
 * - AI-powered triage and intent detection
 * - Protocol consultation and tracking
 * - Escalation logic to human agents
 * - Context handoff and session management
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"agente.ia.resposta", "agente.ia.escalacao"})
@DisplayName("SUB-005: Agentes IA - Workflow Integration Tests")
class AgentesIaWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-005_Agentes_IA";

    @Test
    @DisplayName("Deve realizar triage automático com IA e responder consulta simples")
    void deveRealizarTriageAutomaticoResponderConsulta() {
        // Given
        ConversaDTO conversa = TestDataBuilder.createConversa()
                .withBeneficiarioId("BEN-001")
                .withMensagem("Qual é o telefone da central de atendimento?")
                .withCanal("WHATSAPP")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("conversa", conversa);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String respostaIA = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("respostaIA")
                .singleResult().getValue();

        assertThat(respostaIA).contains("0800");

        Boolean escalonado = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("escalonado")
                .singleResult().getValue();

        assertThat(escalonado).isFalse();
    }

    @Test
    @DisplayName("Deve consultar protocolo existente e retornar status")
    void deveConsultarProtocoloRetornarStatus() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("numeroProtocolo", "PROT-12345");
        variables.put("tipoConsulta", "STATUS_PROTOCOLO");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        Map<String, Object> statusProtocolo = (Map<String, Object>)
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(instance.getId())
                        .variableName("statusProtocolo")
                        .singleResult().getValue();

        assertThat(statusProtocolo).containsKeys("numero", "status", "dataAbertura", "descricao");
    }

    @Test
    @DisplayName("Deve escalar para atendente humano quando IA não consegue resolver")
    void deveEscalarParaAtendenteHumano() {
        // Given
        ConversaDTO conversaComplexa = TestDataBuilder.createConversa()
                .withBeneficiarioId("BEN-001")
                .withMensagem("Preciso contestar uma negativa de autorização cirúrgica")
                .withCanal("PORTAL")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("conversa", conversaComplexa);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isWaitingAt("UserTask_AtendimentoHumano");

        Boolean escalonado = (Boolean) runtimeService.getVariable(instance.getId(), "escalonado");
        String motivoEscalacao = (String) runtimeService.getVariable(instance.getId(), "motivoEscalacao");

        assertThat(escalonado).isTrue();
        assertThat(motivoEscalacao).contains("complexidade");
    }

    @Test
    @DisplayName("Deve transferir contexto completo ao escalar para humano")
    void deveTransferirContextoCompletoAoEscalar() {
        // Given
        Map<String, Object> variables = TestDataBuilder.createConversaVariables()
                .withHistoricoConversa()
                .withPerfilBeneficiario()
                .withNecessitaEscalacao(true)
                .build();

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Map<String, Object> contextoTransferido = (Map<String, Object>)
                runtimeService.getVariable(instance.getId(), "contextoTransferido");

        assertThat(contextoTransferido).containsKeys(
                "historicoConversa",
                "perfilBeneficiario",
                "tentativasResolucaoIA",
                "motivoEscalacao"
        );
    }

    @Test
    @DisplayName("Deve aprender com interações e melhorar respostas")
    void deveAprenderComInteracoesMelhorarRespostas() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("pergunta", "Como faço para agendar consulta?");
        variables.put("feedbackPositivo", true);
        variables.put("resolvidoPelaIA", true);

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
}
