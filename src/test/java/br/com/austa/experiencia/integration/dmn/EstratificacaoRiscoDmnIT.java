package br.com.austa.experiencia.integration.dmn;

import br.com.austa.experiencia.support.BaseIntegrationTest;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DMN_EstratificacaoRisco decision table.
 *
 * Tests health risk stratification logic including:
 * - Age-based risk scoring
 * - BMI-based risk scoring
 * - Comorbidities scoring
 * - Lifestyle factors (smoking, sedentary, family history)
 * - Score aggregation with COLLECT SUM policy
 * - Final risk classification (BAIXO, MODERADO, ALTO, COMPLEXO)
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Estratificação de Risco")
class EstratificacaoRiscoDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY_ESTRATIFICACAO = "DMN_EstratificacaoRisco";
    private static final String DECISION_KEY_CLASSIFICACAO = "DMN_ClassificacaoRisco";

    @Test
    @DisplayName("Should classify as BAIXO risk - young, healthy profile")
    void deveClassificarComoBaixoRisco() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("idade", 25)
            .putValue("bmi", 22.0)
            .putValue("qtdComorbidades", 0)
            .putValue("historicoFamiliarPositivo", false)
            .putValue("tabagista", false)
            .putValue("sedentario", false);

        // Act - Evaluate stratification (COLLECT SUM)
        DmnDecisionTableResult stratificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ESTRATIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Sum all partial scores
        Integer scoreTotal = stratificationResult.collectEntries("scoreRiscoParcial")
            .stream()
            .mapToInt(o -> (Integer) o)
            .sum();

        variables.putValue("scoreRisco", scoreTotal);

        // Act - Evaluate classification (FIRST)
        DmnDecisionTableResult classificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(scoreTotal).isEqualTo(0); // Age: 0, BMI: 0, Comorbidities: 0
        assertThat(classificationResult.getSingleResult().get("classificacaoRisco"))
            .isEqualTo("BAIXO");
    }

    @Test
    @DisplayName("Should classify as MODERADO risk - middle age with some risk factors")
    void deveClassificarComoModeradoRisco() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("idade", 45)
            .putValue("bmi", 27.0)
            .putValue("qtdComorbidades", 1)
            .putValue("historicoFamiliarPositivo", true)
            .putValue("tabagista", false)
            .putValue("sedentario", true);

        // Act - Stratification
        DmnDecisionTableResult stratificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ESTRATIFICACAO)
            .singleResult()
            .evaluate(variables);

        Integer scoreTotal = stratificationResult.collectEntries("scoreRiscoParcial")
            .stream()
            .mapToInt(o -> (Integer) o)
            .sum();

        variables.putValue("scoreRisco", scoreTotal);

        // Act - Classification
        DmnDecisionTableResult classificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        // Age [30..50]: 10, BMI [25..30]: 10, Comorbidities [1..2]: 15,
        // Family history: 10, Sedentary: 5 = 50
        assertThat(scoreTotal).isEqualTo(50);
        assertThat(classificationResult.getSingleResult().get("classificacaoRisco"))
            .isEqualTo("MODERADO");
    }

    @Test
    @DisplayName("Should classify as ALTO risk - senior with multiple comorbidities")
    void deveClassificarComoAltoRisco() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("idade", 62)
            .putValue("bmi", 31.5)
            .putValue("qtdComorbidades", 2)
            .putValue("historicoFamiliarPositivo", true)
            .putValue("tabagista", false)
            .putValue("sedentario", false);

        // Act
        DmnDecisionTableResult stratificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ESTRATIFICACAO)
            .singleResult()
            .evaluate(variables);

        Integer scoreTotal = stratificationResult.collectEntries("scoreRiscoParcial")
            .stream()
            .mapToInt(o -> (Integer) o)
            .sum();

        variables.putValue("scoreRisco", scoreTotal);

        DmnDecisionTableResult classificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        // Age [51..65]: 20, BMI >30: 20, Comorbidities [1..2]: 15, Family: 10 = 65
        assertThat(scoreTotal).isEqualTo(65);
        assertThat(classificationResult.getSingleResult().get("classificacaoRisco"))
            .isEqualTo("ALTO");
    }

    @Test
    @DisplayName("Should classify as COMPLEXO risk - elderly with high risk profile")
    void deveClassificarComoComplexoRisco() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("idade", 70)
            .putValue("bmi", 32.0)
            .putValue("qtdComorbidades", 3)
            .putValue("historicoFamiliarPositivo", true)
            .putValue("tabagista", true)
            .putValue("sedentario", true);

        // Act
        DmnDecisionTableResult stratificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ESTRATIFICACAO)
            .singleResult()
            .evaluate(variables);

        Integer scoreTotal = stratificationResult.collectEntries("scoreRiscoParcial")
            .stream()
            .mapToInt(o -> (Integer) o)
            .sum();

        variables.putValue("scoreRisco", scoreTotal);

        DmnDecisionTableResult classificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        // Age >65: 30, BMI >30: 20, Comorbidities >=3: 30,
        // Family: 10, Smoker: 15, Sedentary: 5 = 110
        assertThat(scoreTotal).isEqualTo(110);
        assertThat(classificationResult.getSingleResult().get("classificacaoRisco"))
            .isEqualTo("COMPLEXO");
    }

    @Test
    @DisplayName("Should handle boundary case - age exactly 30")
    void deveCalcularScoreCorretamenteIdadeExata30() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("idade", 30)
            .putValue("bmi", 22.0)
            .putValue("qtdComorbidades", 0)
            .putValue("historicoFamiliarPositivo", false)
            .putValue("tabagista", false)
            .putValue("sedentario", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ESTRATIFICACAO)
            .singleResult()
            .evaluate(variables);

        Integer scoreTotal = result.collectEntries("scoreRiscoParcial")
            .stream()
            .mapToInt(o -> (Integer) o)
            .sum();

        // Assert - Age [30..50] should give 10 points
        assertThat(scoreTotal).isEqualTo(10);
    }

    @Test
    @DisplayName("Should correctly aggregate COLLECT SUM policy")
    void deveAgregarScoresCorretamenteComPoliticaCOLLECT() {
        // Arrange - Scenario with multiple risk factors
        VariableMap variables = Variables.createVariables()
            .putValue("idade", 55)
            .putValue("bmi", 28.0)
            .putValue("qtdComorbidades", 2)
            .putValue("historicoFamiliarPositivo", true)
            .putValue("tabagista", true)
            .putValue("sedentario", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ESTRATIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Get all partial scores
        List<Object> partialScores = result.collectEntries("scoreRiscoParcial");
        Integer scoreTotal = partialScores.stream()
            .mapToInt(o -> (Integer) o)
            .sum();

        // Assert
        // Age [51..65]: 20, BMI [25..30]: 10, Comorbidities [1..2]: 15,
        // Family: 10, Smoker: 15, Sedentary: 5
        assertThat(partialScores).hasSize(6);
        assertThat(scoreTotal).isEqualTo(75);
    }

    @Test
    @DisplayName("Should handle edge case - BMI exactly 25")
    void deveCalcularScoreCorretamenteBMIExato25() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("idade", 40)
            .putValue("bmi", 25.0)
            .putValue("qtdComorbidades", 0)
            .putValue("historicoFamiliarPositivo", false)
            .putValue("tabagista", false)
            .putValue("sedentario", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ESTRATIFICACAO)
            .singleResult()
            .evaluate(variables);

        Integer scoreTotal = result.collectEntries("scoreRiscoParcial")
            .stream()
            .mapToInt(o -> (Integer) o)
            .sum();

        // Assert - BMI [25..30] should give 10 points, Age [30..50]: 10 = 20
        assertThat(scoreTotal).isEqualTo(20);
    }

    @Test
    @DisplayName("Should handle maximum risk scenario")
    void deveCalcularScoreMaximo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("idade", 75)
            .putValue("bmi", 35.0)
            .putValue("qtdComorbidades", 5)
            .putValue("historicoFamiliarPositivo", true)
            .putValue("tabagista", true)
            .putValue("sedentario", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ESTRATIFICACAO)
            .singleResult()
            .evaluate(variables);

        Integer scoreTotal = result.collectEntries("scoreRiscoParcial")
            .stream()
            .mapToInt(o -> (Integer) o)
            .sum();

        variables.putValue("scoreRisco", scoreTotal);

        DmnDecisionTableResult classificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert - Maximum possible score
        // Age >65: 30, BMI >30: 20, Comorbidities >=3: 30,
        // Family: 10, Smoker: 15, Sedentary: 5 = 110
        assertThat(scoreTotal).isEqualTo(110);
        assertThat(classificationResult.getSingleResult().get("classificacaoRisco"))
            .isEqualTo("COMPLEXO");
    }
}
