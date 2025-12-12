package br.com.austa.experiencia.integration.dmn;

import br.com.austa.experiencia.support.BaseIntegrationTest;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DMN_ElegibilidadePrograma decision table.
 *
 * Tests eligibility for chronic disease management programs:
 * - PROG_DIABETES: Diabetes program
 * - PROG_HIPERTENSAO: Hypertension program
 * - PROG_DPOC: COPD program
 * - PROG_ICC: Heart failure program
 * - PROG_ASMA: Asthma program
 * - PROG_OBESIDADE: Obesity program
 * - PROG_RENAL: Kidney disease program
 * - PROG_GESTANTE: Maternity program
 * - PROG_IDOSO: Elderly care program
 * - PROG_ALTO_RISCO: High-risk program
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Elegibilidade para Programa")
class ElegibilidadeProgramaDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY = "DMN_ElegibilidadePrograma";

    @Test
    @DisplayName("Should be eligible for PROG_DIABETES with diabetes condition")
    void deveSerElegivelParaProgramaDiabetes() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList("DIABETES"))
            .putValue("idade", 55)
            .putValue("scoreRisco", 45)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_DIABETES");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("MODERADO");
    }

    @Test
    @DisplayName("Should be eligible for PROG_HIPERTENSAO with hypertension condition")
    void deveSerElegivelParaProgramaHipertensao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList("HIPERTENSAO"))
            .putValue("idade", 60)
            .putValue("scoreRisco", 35)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_HIPERTENSAO");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("LEVE");
    }

    @Test
    @DisplayName("Should be eligible for PROG_DPOC with intensive care level")
    void deveSerElegivelParaProgramaDPOCComIntensidadeIntensiva() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList("DPOC"))
            .putValue("idade", 68)
            .putValue("scoreRisco", 65)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_DPOC");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("INTENSIVO");
    }

    @Test
    @DisplayName("Should be eligible for PROG_ICC with intensive care level")
    void deveSerElegivelParaProgramaICCComIntensidadeIntensiva() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList("ICC"))
            .putValue("idade", 72)
            .putValue("scoreRisco", 70)
            .putValue("gestante", false)
            .putValue("internacaoRecente", true)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_ICC");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("INTENSIVO");
    }

    @Test
    @DisplayName("Should be eligible for PROG_ASMA with moderate intensity")
    void deveSerElegivelParaProgramaAsma() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList("ASMA"))
            .putValue("idade", 35)
            .putValue("scoreRisco", 30)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_ASMA");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("MODERADO");
    }

    @Test
    @DisplayName("Should be eligible for PROG_OBESIDADE")
    void deveSerElegivelParaProgramaObesidade() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList("OBESIDADE"))
            .putValue("idade", 40)
            .putValue("scoreRisco", 35)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_OBESIDADE");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("LEVE");
    }

    @Test
    @DisplayName("Should be eligible for PROG_RENAL with intensive care")
    void deveSerElegivelParaProgramaRenal() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList("DRC"))
            .putValue("idade", 65)
            .putValue("scoreRisco", 75)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_RENAL");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("INTENSIVO");
    }

    @Test
    @DisplayName("Should be eligible for PROG_GESTANTE when pregnant")
    void deveSerElegivelParaProgramaGestante() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList())
            .putValue("idade", 28)
            .putValue("scoreRisco", 20)
            .putValue("gestante", true)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_GESTANTE");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("MODERADO");
    }

    @Test
    @DisplayName("Should be eligible for PROG_IDOSO - elderly with high risk")
    void deveSerElegivelParaProgramaIdoso() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList())
            .putValue("idade", 70)
            .putValue("scoreRisco", 55)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_IDOSO");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("MODERADO");
    }

    @Test
    @DisplayName("Should be eligible for PROG_ALTO_RISCO with high risk score")
    void deveSerElegivelParaProgramaAltoRisco() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList())
            .putValue("idade", 55)
            .putValue("scoreRisco", 80)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("programaPrioritario"))
            .isEqualTo("PROG_ALTO_RISCO");
        assertThat(result.getSingleResult().get("nivelIntensidade"))
            .isEqualTo("INTENSIVO");
    }

    @Test
    @DisplayName("Should be eligible for multiple programs using COLLECT policy")
    void deveSerElegivelParaMultiplosProgramas() {
        // Arrange - Patient with multiple conditions
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList("DIABETES", "HIPERTENSAO", "OBESIDADE"))
            .putValue("idade", 68)
            .putValue("scoreRisco", 75)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - COLLECT policy should return multiple programs
        // Eligible for: DIABETES, HIPERTENSAO, OBESIDADE, IDOSO, ALTO_RISCO
        assertThat(result.getResultList()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(result.collectEntries("programaPrioritario"))
            .contains("PROG_DIABETES", "PROG_HIPERTENSAO", "PROG_OBESIDADE");
    }

    @Test
    @DisplayName("Should handle boundary case - exactly 65 years old")
    void deveSerElegivelParaProgramaIdosoComExatamente65Anos() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList())
            .putValue("idade", 65)
            .putValue("scoreRisco", 50)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - >= 65 should trigger elderly program
        assertThat(result.collectEntries("programaPrioritario"))
            .contains("PROG_IDOSO");
    }

    @Test
    @DisplayName("Should handle boundary case - exactly score 70 for high risk")
    void deveSerElegivelParaAltoRiscoComScore70() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList())
            .putValue("idade", 50)
            .putValue("scoreRisco", 70)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - >= 70 should trigger high-risk program
        assertThat(result.collectEntries("programaPrioritario"))
            .contains("PROG_ALTO_RISCO");
    }

    @Test
    @DisplayName("Should not be eligible for any program with no risk factors")
    void naoDeveSerElegivelParaNenhumPrograma() {
        // Arrange - Healthy young individual
        VariableMap variables = Variables.createVariables()
            .putValue("condicoesCronicas", Arrays.asList())
            .putValue("idade", 30)
            .putValue("scoreRisco", 15)
            .putValue("gestante", false)
            .putValue("internacaoRecente", false)
            .putValue("temNavegadorAtribuido", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - No programs should match
        assertThat(result.getResultList()).isEmpty();
    }
}
