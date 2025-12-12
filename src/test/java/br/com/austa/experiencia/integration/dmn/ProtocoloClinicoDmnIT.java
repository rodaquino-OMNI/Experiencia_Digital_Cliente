package br.com.austa.experiencia.integration.dmn;

import br.com.austa.experiencia.support.BaseIntegrationTest;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DMN_ProtocoloClinico decision table.
 *
 * Tests clinical protocol validation including:
 * - Orthopedic surgeries (conservative treatment requirements)
 * - Oncology procedures (diagnosis-based approval)
 * - Cardiovascular procedures (severity indicators)
 * - Bariatric surgery (time requirements)
 * - High-complexity imaging (prerequisite exams)
 * - Physiotherapy (medical indication)
 * - Emergency procedures (immediate approval)
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Protocolo Clínico")
class ProtocoloClinicoDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY = "DMN_ProtocoloClinico";

    @Test
    @DisplayName("Should approve orthopedic surgery with proper conservative treatment")
    void deveAprovarCirurgiaOrtopedicaComTratamentoConservador() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CIRURGIA_ORTOPEDICA")
            .putValue("cid10", "M17")
            .putValue("tratamentosPrevios", true)
            .putValue("tempoTratamento", 8)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 50);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(true);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("PROTOCOLO_ATENDIDO");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("APROVAR_AUTOMATICAMENTE");
    }

    @Test
    @DisplayName("Should deny orthopedic surgery with insufficient conservative treatment time")
    void deveNegarCirurgiaOrtopedicaPorTempoInsuficiente() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "ARTROPLASTIA")
            .putValue("cid10", "M16")
            .putValue("tratamentosPrevios", true)
            .putValue("tempoTratamento", 3)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 45);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(false);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("TEMPO_INSUFICIENTE_TRATAMENTO_CONSERVADOR");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("AUDITORIA_MEDICA_OBRIGATORIA");
    }

    @Test
    @DisplayName("Should deny orthopedic surgery without complementary exams")
    void deveNegarCirurgiaOrtopedicaSemExamesComplementares() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "ARTROSCOPIA")
            .putValue("cid10", "M23")
            .putValue("tratamentosPrevios", true)
            .putValue("tempoTratamento", 6)
            .putValue("examesComplementares", false)
            .putValue("indicadorGravidade", 40);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(false);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("EXAMES_COMPLEMENTARES_OBRIGATORIOS_AUSENTES");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("SOLICITAR_EXAMES_COMPLEMENTARES");
    }

    @Test
    @DisplayName("Should approve oncology procedure with cancer diagnosis")
    void deveAprovarProcedimentoOncologicoComDiagnosticoCancer() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "QUIMIOTERAPIA")
            .putValue("cid10", "C50")
            .putValue("tratamentosPrevios", false)
            .putValue("tempoTratamento", 0)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 80);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(true);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("PROTOCOLO_ONCOLOGICO_ATENDIDO");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("APROVAR_PRIORITARIAMENTE");
    }

    @Test
    @DisplayName("Should approve cardiac catheterization with high severity")
    void deveAprovarCateterismoComAltaGravidade() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CATETERISMO")
            .putValue("cid10", "I21")
            .putValue("tratamentosPrevios", false)
            .putValue("tempoTratamento", 0)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 85);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(true);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("PROTOCOLO_CARDIOLOGICO_ATENDIDO");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("APROVAR_URGENTEMENTE");
    }

    @Test
    @DisplayName("Should deny cardiac catheterization with low severity")
    void deveNegarCateterismoPorBaixaGravidade() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "ANGIOPLASTIA")
            .putValue("cid10", "I25")
            .putValue("tratamentosPrevios", false)
            .putValue("tempoTratamento", 0)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 55);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(false);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("INDICADOR_GRAVIDADE_INSUFICIENTE");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("AUDITORIA_CARDIOLOGISTA");
    }

    @Test
    @DisplayName("Should approve bariatric surgery with proper clinical treatment time")
    void deveAprovarCirurgiaBariatricaComTempoAdequado() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CIRURGIA_BARIATRICA")
            .putValue("cid10", "E66")
            .putValue("tratamentosPrevios", true)
            .putValue("tempoTratamento", 30)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 70);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(true);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("PROTOCOLO_BARIATRICA_ATENDIDO");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("APROVAR_COM_AVALIACAO_MULTIDISCIPLINAR");
    }

    @Test
    @DisplayName("Should deny bariatric surgery with insufficient clinical treatment time")
    void deveNegarCirurgiaBariatricaPorTempoInsuficiente() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "GASTROPLASTIA")
            .putValue("cid10", "E66")
            .putValue("tratamentosPrevios", true)
            .putValue("tempoTratamento", 18)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 65);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(false);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("TEMPO_MINIMO_TRATAMENTO_CLINICO_NAO_CUMPRIDO");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("SOLICITAR_CONTINUIDADE_TRATAMENTO_CLINICO");
    }

    @Test
    @DisplayName("Should approve high-complexity imaging with prior conservative treatment")
    void deveAprovarImagemAltaComplexidadeComTratamentoPrevio() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "RESSONANCIA")
            .putValue("cid10", "M51")
            .putValue("tratamentosPrevios", true)
            .putValue("tempoTratamento", 3)
            .putValue("examesComplementares", false)
            .putValue("indicadorGravidade", 50);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(true);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("EXAMES_PREVIOS_REALIZADOS");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("APROVAR_AUTOMATICAMENTE");
    }

    @Test
    @DisplayName("Should deny high-complexity imaging without basic exams first")
    void deveNegarImagemAltaComplexidadeSemExamesBasicos() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "TOMOGRAFIA")
            .putValue("cid10", "M54")
            .putValue("tratamentosPrevios", false)
            .putValue("tempoTratamento", 0)
            .putValue("examesComplementares", false)
            .putValue("indicadorGravidade", 40);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(false);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("EXAMES_BASICOS_NAO_REALIZADOS");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("SOLICITAR_EXAMES_BASICOS_PRIMEIRO");
    }

    @Test
    @DisplayName("Should approve physiotherapy with complementary exams")
    void deveAprovarFisioterapiaComExamesComplementares() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "FISIOTERAPIA")
            .putValue("cid10", "M79")
            .putValue("tratamentosPrevios", false)
            .putValue("tempoTratamento", 0)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 30);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(true);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("INDICACAO_MEDICA_ADEQUADA");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("APROVAR_SESSOES_PROTOCOLO");
    }

    @Test
    @DisplayName("Should immediately approve emergency procedures")
    void deveAprovarImediatamenteProcedimentosUrgenciaEmergencia() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "URGENCIA")
            .putValue("cid10", "S06")
            .putValue("tratamentosPrevios", false)
            .putValue("tempoTratamento", 0)
            .putValue("examesComplementares", false)
            .putValue("indicadorGravidade", 95);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(true);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("URGENCIA_EMERGENCIA_APROVACAO_AUTOMATICA");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("APROVAR_IMEDIATAMENTE");
    }

    @Test
    @DisplayName("Should require medical audit for unknown procedures - default rule")
    void deveExigirAuditoriaMedicaParaProcedimentosDesconhecidos() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "PROCEDIMENTO_NOVO")
            .putValue("cid10", "Z00")
            .putValue("tratamentosPrevios", false)
            .putValue("tempoTratamento", 0)
            .putValue("examesComplementares", false)
            .putValue("indicadorGravidade", 50);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(false);
        assertThat(result.getSingleResult().get("justificativaNaoAderencia"))
            .isEqualTo("REQUER_AVALIACAO_MEDICA_DETALHADA");
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("ENCAMINHAR_AUDITORIA_MEDICA");
    }

    @Test
    @DisplayName("Should handle boundary case - exactly 6 months conservative treatment")
    void deveAprovarComExatamente6MesesTratamento() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CIRURGIA_ORTOPEDICA")
            .putValue("cid10", "M17")
            .putValue("tratamentosPrevios", true)
            .putValue("tempoTratamento", 6)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 50);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - >= 6 months should approve
        assertThat(result.getSingleResult().get("atendeProtocolo"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should use FIRST hit policy - emergency before orthopedic rules")
    void deveUsarPoliticaFIRSTCorretamente() {
        // Arrange - Emergency procedure that could also match orthopedic rule
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "EMERGENCIA")
            .putValue("cid10", "M17")
            .putValue("tratamentosPrevios", true)
            .putValue("tempoTratamento", 6)
            .putValue("examesComplementares", true)
            .putValue("indicadorGravidade", 50);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - Emergency rule should fire first
        assertThat(result.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("APROVAR_IMEDIATAMENTE");
    }
}
