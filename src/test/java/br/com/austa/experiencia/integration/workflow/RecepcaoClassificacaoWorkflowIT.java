package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.SolicitacaoDTO;
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
 * Integration tests for SUB-003: Recepção e Classificação Inteligente workflow.
 *
 * Tests cover:
 * - Multi-channel reception (WhatsApp, Telegram, Portal, Email)
 * - NLP classification and intent detection
 * - Urgency-based routing
 * - Profile loading and context enrichment
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"solicitacao.recebida", "solicitacao.classificada"})
@DisplayName("SUB-003: Recepção e Classificação Inteligente - Workflow Integration Tests")
class RecepcaoClassificacaoWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-003_Recepcao_Classificacao";

    @Test
    @DisplayName("Deve receber e classificar solicitação via WhatsApp")
    void deveReceberClassificarSolicitacaoWhatsApp() {
        // Given
        SolicitacaoDTO solicitacao = TestDataBuilder.createSolicitacao()
                .withCanal("WHATSAPP")
                .withBeneficiarioId("BEN-001")
                .withMensagem("Gostaria de saber sobre autorização de consulta")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitacao", solicitacao);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String classificacao = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("classificacao")
                .singleResult().getValue();

        assertThat(classificacao).isIn("AUTORIZACAO", "CONSULTA_STATUS");
    }

    @Test
    @DisplayName("Deve classificar urgência e rotear para fila apropriada")
    void deveClassificarUrgenciaRotearFila() {
        // Given
        SolicitacaoDTO solicitacaoUrgente = TestDataBuilder.createSolicitacao()
                .withCanal("PORTAL")
                .withMensagem("Preciso urgente de autorização para internação de emergência")
                .withBeneficiarioId("BEN-002")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitacao", solicitacaoUrgente);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        String urgencia = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("urgencia")
                .singleResult().getValue();

        String filaRoteamento = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("filaRoteamento")
                .singleResult().getValue();

        assertThat(urgencia).isEqualTo("ALTA");
        assertThat(filaRoteamento).isEqualTo("EMERGENCIA");
    }

    @Test
    @DisplayName("Deve carregar perfil completo do beneficiário")
    void deveCarregarPerfilCompletoBeneficiario() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("canal", "TELEGRAM");
        variables.put("mensagem", "Consulta sobre plano");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Map<String, Object> perfilBeneficiario = (Map<String, Object>)
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(instance.getId())
                        .variableName("perfilBeneficiario")
                        .singleResult().getValue();

        assertThat(perfilBeneficiario).isNotNull();
        assertThat(perfilBeneficiario).containsKeys("nome", "plano", "historico");
    }

    @Test
    @DisplayName("Deve usar NLP para detectar intenção da mensagem")
    void deveUsarNlpDetectarIntencao() {
        // Given
        SolicitacaoDTO solicitacao = TestDataBuilder.createSolicitacao()
                .withMensagem("Quero solicitar reembolso de consulta particular")
                .withCanal("EMAIL")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitacao", solicitacao);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        String intencaoDetectada = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("intencaoDetectada")
                .singleResult().getValue();

        assertThat(intencaoDetectada).isEqualTo("REEMBOLSO");
    }

    @Test
    @DisplayName("Deve tratar mensagem ambígua e solicitar esclarecimento")
    void deveTratarMensagemAmbiguaSolicitarEsclarecimento() {
        // Given
        SolicitacaoDTO solicitacaoAmbigua = TestDataBuilder.createSolicitacao()
                .withMensagem("Oi")
                .withCanal("WHATSAPP")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitacao", solicitacaoAmbigua);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Boolean necessitaEsclarecimento = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("necessitaEsclarecimento")
                .singleResult().getValue();

        assertThat(necessitaEsclarecimento).isTrue();
    }
}
