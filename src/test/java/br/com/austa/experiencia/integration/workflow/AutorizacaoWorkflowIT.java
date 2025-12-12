package br.com.austa.experiencia.integration.workflow;

import br.com.austa.experiencia.integration.BaseIntegrationTest;
import br.com.austa.experiencia.model.dto.GuiaTissDTO;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Integration tests for SUB-006: Autorização Inteligente workflow (CRITICAL).
 *
 * Tests cover:
 * - Auto-approval scenarios for low-risk procedures
 * - Medical audit routing for complex cases
 * - CPT blocking and validation
 * - Denial scenarios with proper justification
 *
 * @see PROMPT_TECNICO_3.MD lines 1244-1311
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"autorizacao.aprovada", "autorizacao.negada", "autorizacao.auditoria"})
@DisplayName("SUB-006: Autorização Inteligente - Workflow Integration Tests (CRITICAL)")
class AutorizacaoWorkflowIT extends BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    private static final String PROCESS_KEY = "SUB-006_Autorizacao_Inteligente";

    @Test
    @DisplayName("Deve autorizar consulta eletiva automaticamente")
    void deveAutorizarConsultaEletivaAutomaticamente() {
        // Given
        GuiaTissDTO guiaTiss = TestDataBuilder.createGuiaTiss()
                .withTipo("CONSULTA_ELETIVA")
                .withProcedimento("10101012", "Consulta médica")
                .withValor(350.00)
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("guiaTISS", guiaTiss);
        variables.put("beneficiarioId", "BEN-001");
        variables.put("prestadorId", "PREST-001");
        variables.put("valorSolicitado", 350.00);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String statusAutorizacao = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("statusAutorizacao")
                .singleResult().getValue();

        String tipoAutorizacao = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("tipoAutorizacao")
                .singleResult().getValue();

        String numeroAutorizacao = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("numeroAutorizacao")
                .singleResult().getValue();

        assertThat(statusAutorizacao).isEqualTo("APROVADA");
        assertThat(tipoAutorizacao).isEqualTo("AUTOMATICA");
        assertThat(numeroAutorizacao).isNotNull();
    }

    @Test
    @DisplayName("Deve escalar para auditoria médica em cirurgia eletiva de alto valor")
    void deveEscalarParaAuditoriaMedicaEmCirurgia() {
        // Given
        GuiaTissDTO guiaTissCirurgia = TestDataBuilder.createGuiaTiss()
                .withTipo("CIRURGIA_ELETIVA")
                .withProcedimento("31201012", "Cirurgia cardiovascular")
                .withValor(15000.00)
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("guiaTISS", guiaTissCirurgia);
        variables.put("beneficiarioId", "BEN-001");
        variables.put("prestadorId", "PREST-002");
        variables.put("valorSolicitado", 15000.00);

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isNotEnded();
        assertThat(instance).isWaitingAt("UserTask_AuditoriaMedica");

        String statusAutorizacao = (String) runtimeService.getVariable(instance.getId(), "statusAutorizacao");
        assertThat(statusAutorizacao).isEqualTo("EM_AUDITORIA");
    }

    @Test
    @DisplayName("Deve negar autorização por carência não cumprida")
    void deveNegarPorCarenciaNaoCumprida() {
        // Given
        GuiaTissDTO guiaTissInternacao = TestDataBuilder.createGuiaTiss()
                .withTipo("INTERNACAO")
                .withProcedimento("30101010", "Internação hospitalar")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("guiaTISS", guiaTissInternacao);
        variables.put("beneficiarioId", "BEN-NOVO");
        variables.put("dataAdesao", LocalDate.now().minusDays(30));  // Carência de 180 dias não cumprida

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String statusAutorizacao = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("statusAutorizacao")
                .singleResult().getValue();

        String motivoNegativa = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("motivoNegativa")
                .singleResult().getValue();

        assertThat(statusAutorizacao).isEqualTo("NEGADA");
        assertThat(motivoNegativa).contains("CARENCIA");
    }

    @Test
    @DisplayName("Deve bloquear procedimento CPT e exigir validação médica")
    void deveBloquearprocedimentoCptExigirValidacao() {
        // Given
        GuiaTissDTO guiaTissCpt = TestDataBuilder.createGuiaTiss()
                .withTipo("CIRURGIA_ELETIVA")
                .withProcedimento("40101010", "Procedimento CPT")
                .withCptDetectado(true)
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("guiaTISS", guiaTissCpt);
        variables.put("beneficiarioId", "BEN-001");

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isNotEnded();
        assertThat(instance).isWaitingAt("UserTask_ValidacaoCPT");

        Boolean cptDetectado = (Boolean) runtimeService.getVariable(instance.getId(), "cptDetectado");
        assertThat(cptDetectado).isTrue();
    }

    @Test
    @DisplayName("Deve aprovar urgência/emergência automaticamente independente de carência")
    void deveAprovarUrgenciaEmergenciaAutomaticamente() {
        // Given
        GuiaTissDTO guiaTissEmergencia = TestDataBuilder.createGuiaTiss()
                .withTipo("URGENCIA_EMERGENCIA")
                .withProcedimento("30301010", "Atendimento emergência")
                .withCaraterAtendimento("EMERGENCIA")
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("guiaTISS", guiaTissEmergencia);
        variables.put("beneficiarioId", "BEN-NOVO");
        variables.put("dataAdesao", LocalDate.now().minusDays(1));  // Sem carência cumprida

        // When
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // Then
        assertThat(instance).isEnded();

        String statusAutorizacao = (String) historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getId())
                .variableName("statusAutorizacao")
                .singleResult().getValue();

        assertThat(statusAutorizacao).isEqualTo("APROVADA");
    }
}
