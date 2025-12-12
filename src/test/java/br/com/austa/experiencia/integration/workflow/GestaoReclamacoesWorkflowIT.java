package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.ReclamacaoDTO;
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
 * Integration tests for SUB-009: Gestão de Reclamações workflow.
 *
 * Tests cover:
 * - Complaint registration and categorization
 * - Root cause analysis
 * - Solution proposal and implementation
 * - Escalation to regulatory bodies
 *
 * @see PROMPT_TECNICO_3.MD
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"reclamacao.registrada", "reclamacao.resolvida"})
@DisplayName("SUB-009: Gestão de Reclamações - Workflow Integration Tests")
class GestaoReclamacoesWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-009_Gestao_Reclamacoes";

    @Test
    @DisplayName("Deve registrar reclamação e categorizar automaticamente")
    void deveRegistrarReclamacaoCategorizarAutomaticamente() {
        // Given
        ReclamacaoDTO reclamacao = TestDataBuilder.createReclamacao()
                .withBeneficiarioId("BEN-001")
                .withDescricao("Negativa de autorização sem justificativa adequada")
                .withCanal("PORTAL")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("reclamacao", reclamacao);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        String categoriaReclamacao = (String) runtimeService.getVariable(instance.getId(), "categoriaReclamacao");
        String numeroProtocolo = (String) runtimeService.getVariable(instance.getId(), "numeroProtocolo");

        assertThat(categoriaReclamacao).isEqualTo("AUTORIZACAO");
        assertThat(numeroProtocolo).startsWith("REC-");
    }

    @Test
    @DisplayName("Deve realizar análise de causa raiz para reclamações recorrentes")
    void deveRealizarAnaliseCausaRaizRecorrentes() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiarioId", "BEN-001");
        variables.put("reclamacoesAnteriores", 3);
        variables.put("categoriaReclamacao", "AUTORIZACAO");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        Boolean analiseRealizada = (Boolean) runtimeService.getVariable(instance.getId(), "analiseRealizada");
        String causaRaiz = (String) runtimeService.getVariable(instance.getId(), "causaRaiz");

        assertThat(analiseRealizada).isTrue();
        assertThat(causaRaiz).isNotNull();
    }

    @Test
    @DisplayName("Deve propor solução e implementar ações corretivas")
    void deveProporSolucaoImplementarAcoes() {
        // Given
        ReclamacaoDTO reclamacao = TestDataBuilder.createReclamacao()
                .withCategoria("ATENDIMENTO")
                .withSeveridade("MEDIA")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("reclamacao", reclamacao);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String solucaoProposta = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("solucaoProposta")
                .singleResult().getValue();

        Boolean acoesImplementadas = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("acoesImplementadas")
                .singleResult().getValue();

        assertThat(solucaoProposta).isNotNull();
        assertThat(acoesImplementadas).isTrue();
    }

    @Test
    @DisplayName("Deve escalar para ANS quando SLA é excedido")
    void deveEscalarParaAnsQuandoSlaExcedido() {
        // Given
        ReclamacaoDTO reclamacao = TestDataBuilder.createReclamacao()
                .withSeveridade("ALTA")
                .withDiasAberta(30)  // SLA excedido
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("reclamacao", reclamacao);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isWaitingAt("UserTask_EscalacaoANS");

        Boolean escalonado = (Boolean) runtimeService.getVariable(instance.getId(), "escalonado");
        assertThat(escalonado).isTrue();
    }

    @Test
    @DisplayName("Deve comunicar resolução ao beneficiário e solicitar feedback")
    void deveComunicarResolucaoSolicitarFeedback() {
        // Given
        Map<String, Object> variables = TestDataBuilder.createReclamacaoVariables()
                .withReclamacaoResolvida()
                .build();

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        Boolean comunicacaoEnviada = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("comunicacaoEnviada")
                .singleResult().getValue();

        Boolean feedbackSolicitado = (Boolean) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("feedbackSolicitado")
                .singleResult().getValue();

        assertThat(comunicacaoEnviada).isTrue();
        assertThat(feedbackSolicitado).isTrue();
    }
}
