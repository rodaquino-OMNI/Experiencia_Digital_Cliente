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
 * Integration tests for DMN_ClassificarUrgencia decision table.
 *
 * Tests urgency classification logic including:
 * - Critical urgency (medical emergencies)
 * - High urgency (complaints, high-risk beneficiaries)
 * - Medium urgency (consultations, exams)
 * - Low urgency (general inquiries)
 * - SLA assignment based on urgency level
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Classificação de Urgência")
class ClassificacaoUrgenciaDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY = "DMN_ClassificarUrgencia";

    @Test
    @DisplayName("Should classify as CRITICA - medical emergency keywords")
    void deveClassificarComoCriticaPorPalavraChaveEmergencia() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "infarto")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tentativasAnteriores", 0);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(5);
    }

    @Test
    @DisplayName("Should classify as CRITICA - high risk with negative sentiment and retries")
    void deveClassificarComoCriticaPorAltoRiscoComMultiplasTentativas() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "consulta")
            .putValue("sentimentoNegativo", true)
            .putValue("classificacaoRisco", "COMPLEXO")
            .putValue("tentativasAnteriores", 3);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(10);
    }

    @Test
    @DisplayName("Should classify as ALTA - moderate urgency keywords")
    void deveClassificarComoAltaPorPalavrasChaveDor() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "dor")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tentativasAnteriores", 0);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(15);
    }

    @Test
    @DisplayName("Should classify as ALTA - high risk beneficiary with negative sentiment")
    void deveClassificarComoAltaPorAltoRiscoComSentimentoNegativo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "informação")
            .putValue("sentimentoNegativo", true)
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tentativasAnteriores", 0);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(30);
    }

    @Test
    @DisplayName("Should classify as ALTA - complaint keywords")
    void deveClassificarComoAltaPorReclamacao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "ouvidoria")
            .putValue("sentimentoNegativo", true)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tentativasAnteriores", 1);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(20);
    }

    @Test
    @DisplayName("Should classify as MEDIA - consultation or exam request")
    void deveClassificarComoMediaPorConsulta() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "consulta")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tentativasAnteriores", 0);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("MEDIA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(60);
    }

    @Test
    @DisplayName("Should classify as MEDIA - moderate risk beneficiary")
    void deveClassificarComoMediaPorRiscoModerado() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "informação")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "MODERADO")
            .putValue("tentativasAnteriores", 0);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("MEDIA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(120);
    }

    @Test
    @DisplayName("Should classify as BAIXA - general inquiries (default rule)")
    void deveClassificarComoBaixaPorDuvidaGeral() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "horário")
            .putValue("sentimentoNegativo", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tentativasAnteriores", 0);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("BAIXA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(240);
    }

    @Test
    @DisplayName("Should use FIRST hit policy - emergency takes precedence")
    void deveUsarPoliticaFIRSTPrecedenciaEmergencia() {
        // Arrange - Multiple conditions that could match different rules
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "dor no peito")
            .putValue("sentimentoNegativo", true)
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tentativasAnteriores", 2);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - Should match first rule (emergency keywords) not later rules
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("slaResposta"))
            .isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle edge case - exactly 3 retries")
    void deveClassificarCorretamenteComExatamente3Tentativas() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("palavrasChaveUrgencia", "plano")
            .putValue("sentimentoNegativo", true)
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tentativasAnteriores", 3);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - Should trigger critical rule (>=3 retries)
        assertThat(result.getSingleResult().get("nivelUrgencia"))
            .isEqualTo("CRITICA");
    }
}
