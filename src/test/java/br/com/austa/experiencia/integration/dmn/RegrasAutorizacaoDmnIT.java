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
 * Integration tests for DMN_RegrasAutorizacao decision table.
 *
 * Tests authorization rules including:
 * - Simple consultations (auto-approved)
 * - Routine exams with protocol compliance
 * - Carency period restrictions
 * - High-cost procedures with/without clinical protocol
 * - Out-of-network provider restrictions
 * - Technical analysis requirements
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Regras de Autorização")
class RegrasAutorizacaoDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY = "DMN_RegrasAutorizacao";

    @Test
    @DisplayName("Should approve simple consultation automatically")
    void deveAprovarConsultaSimples() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CONSULTA_CLINICO_GERAL")
            .putValue("diasDesdeAdesao", 30)
            .putValue("valorProcedimento", 200.0)
            .putValue("atendeProtocolo", true)
            .putValue("prestadorRede", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("APROVADO");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .isEqualTo("Procedimento de rotina coberto pelo plano");
        assertThat(result.getSingleResult().get("requerAnaliseTecnica"))
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Should approve routine exam with protocol compliance")
    void deveAprovarExameRotinaComProtocolo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "EXAME_LABORATORIAL")
            .putValue("diasDesdeAdesao", 45)
            .putValue("valorProcedimento", 500.0)
            .putValue("atendeProtocolo", true)
            .putValue("prestadorRede", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("APROVADO");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .isEqualTo("Exame atende protocolo clínico");
        assertThat(result.getSingleResult().get("requerAnaliseTecnica"))
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Should deny elective surgery in carency period")
    void deveNegarCirurgiaEletivaEmCarencia() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CIRURGIA_ELETIVA")
            .putValue("diasDesdeAdesao", 90)
            .putValue("valorProcedimento", 5000.0)
            .putValue("atendeProtocolo", true)
            .putValue("prestadorRede", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("NEGADO");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .isEqualTo("Procedimento em período de carência (180 dias)");
        assertThat(result.getSingleResult().get("requerAnaliseTecnica"))
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Should approve high-cost procedure with protocol")
    void deveAprovarAltoCustoComProtocolo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "PROCEDIMENTO_COMPLEXO")
            .putValue("diasDesdeAdesao", 200)
            .putValue("valorProcedimento", 15000.0)
            .putValue("atendeProtocolo", true)
            .putValue("prestadorRede", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("APROVADO");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .isEqualTo("Alto custo aprovado por protocolo clínico");
        assertThat(result.getSingleResult().get("requerAnaliseTecnica"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should pend high-cost procedure without protocol")
    void devePenderAltoCustoSemProtocolo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "PROCEDIMENTO_COMPLEXO")
            .putValue("diasDesdeAdesao", 200)
            .putValue("valorProcedimento", 20000.0)
            .putValue("atendeProtocolo", false)
            .putValue("prestadorRede", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("PENDENTE");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .isEqualTo("Requer análise técnica - alto custo sem protocolo estabelecido");
        assertThat(result.getSingleResult().get("requerAnaliseTecnica"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should deny out-of-network provider")
    void deveNegarPrestadorForaRede() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CONSULTA_CLINICO_GERAL")
            .putValue("diasDesdeAdesao", 365)
            .putValue("valorProcedimento", 300.0)
            .putValue("atendeProtocolo", true)
            .putValue("prestadorRede", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("NEGADO");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .isEqualTo("Prestador não pertence à rede credenciada");
        assertThat(result.getSingleResult().get("requerAnaliseTecnica"))
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Should approve elective surgery after carency with protocol")
    void deveAprovarCirurgiaEletivaAposCarenciaComProtocolo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CIRURGIA_ELETIVA")
            .putValue("diasDesdeAdesao", 200)
            .putValue("valorProcedimento", 8000.0)
            .putValue("atendeProtocolo", true)
            .putValue("prestadorRede", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("APROVADO");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .isEqualTo("Cirurgia atende protocolo e fora de carência");
        assertThat(result.getSingleResult().get("requerAnaliseTecnica"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should pend unknown procedure - default rule")
    void devePenderProcedimentoDesconhecido() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "PROCEDIMENTO_NOVO")
            .putValue("diasDesdeAdesao", 365)
            .putValue("valorProcedimento", 3000.0)
            .putValue("atendeProtocolo", false)
            .putValue("prestadorRede", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("PENDENTE");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .isEqualTo("Requer análise técnica especializada");
        assertThat(result.getSingleResult().get("requerAnaliseTecnica"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should handle boundary - exactly 180 days")
    void deveAprovarComExatamente180Dias() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CIRURGIA_ELETIVA")
            .putValue("diasDesdeAdesao", 180)
            .putValue("valorProcedimento", 5000.0)
            .putValue("atendeProtocolo", true)
            .putValue("prestadorRede", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - >= 180 should approve
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("APROVADO");
    }

    @Test
    @DisplayName("Should use FIRST hit policy - network provider check after carency")
    void deveUsarPoliticaFIRSTCorretamente() {
        // Arrange - Procedure after carency but out-of-network
        VariableMap variables = Variables.createVariables()
            .putValue("tipoProcedimento", "CIRURGIA_ELETIVA")
            .putValue("diasDesdeAdesao", 200)
            .putValue("valorProcedimento", 5000.0)
            .putValue("atendeProtocolo", true)
            .putValue("prestadorRede", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - Out-of-network rule should fire before approval
        assertThat(result.getSingleResult().get("decisaoAutorizacao"))
            .isEqualTo("NEGADO");
        assertThat(result.getSingleResult().get("motivoDecisao"))
            .contains("rede credenciada");
    }
}
