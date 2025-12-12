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
 * Integration tests for DMN_DefinirRoteamento decision table.
 *
 * Tests routing logic to appropriate service layer:
 * - NAVEGACAO (SUB-007): Navigation support for complex/critical cases
 * - AUTORIZACAO (SUB-006): Authorization processing
 * - AGENTE_IA (SUB-005): AI agent for medium complexity
 * - SELF_SERVICE (SUB-004): Self-service for simple requests
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Roteamento de Demanda")
class RoteamentoDemandaDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY = "DMN_DefinirRoteamento";

    @Test
    @DisplayName("Should route to NAVEGACAO - critical urgency")
    void deveRotearParaNavegacaoPorUrgenciaCritica() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "dor")
            .putValue("complexidadeInteracao", "MEDIA")
            .putValue("nivelUrgencia", "CRITICA")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "MODERADO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("NAVEGACAO");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-007");
    }

    @Test
    @DisplayName("Should route to NAVEGACAO - complex beneficiary with high urgency")
    void deveRotearParaNavegacaoPorBeneficiarioComplexo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "consulta")
            .putValue("complexidadeInteracao", "MEDIA")
            .putValue("nivelUrgencia", "ALTA")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "COMPLEXO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("NAVEGACAO");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-007");
    }

    @Test
    @DisplayName("Should route to NAVEGACAO - high complexity with negative sentiment")
    void deveRotearParaNavegacaoPorComplexidadeAltaComSentimentoNegativo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "reclamacao")
            .putValue("complexidadeInteracao", "ALTA")
            .putValue("nivelUrgencia", "MEDIA")
            .putValue("sentimentoNegativo", true)
            .putValue("classificacaoRisco", "BAIXO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("NAVEGACAO");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-007");
    }

    @Test
    @DisplayName("Should route to AUTORIZACAO - authorization intent")
    void deveRotearParaAutorizacaoPorIntencao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "solicitar_autorizacao")
            .putValue("complexidadeInteracao", "MEDIA")
            .putValue("nivelUrgencia", "MEDIA")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("AUTORIZACAO");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-006");
    }

    @Test
    @DisplayName("Should route to AGENTE_IA - medium complexity inquiry")
    void deveRotearParaAgenteIAPorComplexidadeMedia() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "duvida_cobertura")
            .putValue("complexidadeInteracao", "MEDIA")
            .putValue("nivelUrgencia", "MEDIA")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("AGENTE_IA");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-005");
    }

    @Test
    @DisplayName("Should route to AGENTE_IA - high urgency but low complexity")
    void deveRotearParaAgenteIAPorAltaUrgenciaComBaixaComplexidade() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "informacao")
            .putValue("complexidadeInteracao", "BAIXA")
            .putValue("nivelUrgencia", "ALTA")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("AGENTE_IA");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-005");
    }

    @Test
    @DisplayName("Should route to SELF_SERVICE - simple intent")
    void deveRotearParaSelfServicePorIntencaoSimples() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "consultar_saldo")
            .putValue("complexidadeInteracao", "BAIXA")
            .putValue("nivelUrgencia", "BAIXA")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("SELF_SERVICE");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-004");
    }

    @Test
    @DisplayName("Should route to SELF_SERVICE - low complexity and low urgency")
    void deveRotearParaSelfServicePorBaixaComplexidadeEUrgencia() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "informacao")
            .putValue("complexidadeInteracao", "BAIXA")
            .putValue("nivelUrgencia", "BAIXA")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "MODERADO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("SELF_SERVICE");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-004");
    }

    @Test
    @DisplayName("Should route to AGENTE_IA - fallback rule")
    void deveRotearParaAgenteIAComoFallback() {
        // Arrange - Scenario that doesn't match specific rules
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "desconhecida")
            .putValue("complexidadeInteracao", "MEDIA")
            .putValue("nivelUrgencia", "MEDIA")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - Should use fallback rule
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("AGENTE_IA");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-005");
    }

    @Test
    @DisplayName("Should use FIRST hit policy - navigation takes precedence")
    void deveUsarPoliticaFIRSTComPrecedenciaNavegacao() {
        // Arrange - Could match multiple rules
        VariableMap variables = Variables.createVariables()
            .putValue("intencaoDetectada", "solicitar_autorizacao")
            .putValue("complexidadeInteracao", "ALTA")
            .putValue("nivelUrgencia", "CRITICA")
            .putValue("sentimentoNegativo", true)
            .putValue("classificacaoRisco", "ALTO");

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - Critical urgency rule should fire first
        assertThat(result.getSingleResult().get("camadaDestino"))
            .isEqualTo("NAVEGACAO");
        assertThat(result.getSingleResult().get("tipoSubprocesso"))
            .isEqualTo("SUB-007");
    }
}
