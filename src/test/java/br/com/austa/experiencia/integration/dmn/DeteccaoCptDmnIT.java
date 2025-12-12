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
 * Integration tests for DMN_DeteccaoCPT decision table.
 *
 * Tests pre-existing condition (CPT) detection including:
 * - Chronic disease detection (diabetes, hypertension, etc.)
 * - Confidence level calculation (COLLECT MAX)
 * - Status classification (CPT_PROVAVEL, SUSPEITA_ALTA, SUSPEITA_BAIXA, SEM_CPT)
 * - Required actions based on detection level
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Detecção de CPT")
class DeteccaoCptDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY_DETECCAO = "DMN_DeteccaoCPT";
    private static final String DECISION_KEY_STATUS = "DMN_StatusCPT";

    @Test
    @DisplayName("Should detect diabetes with high confidence - CPT_PROVAVEL")
    void deveDetectarDiabetesComAltaConfianca() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicaoDeclarada", "diabetes")
            .putValue("tempoCondicao", 12)
            .putValue("usaMedicacao", true)
            .putValue("historicoInternacao", false)
            .putValue("bmi", 28.0);

        // Act - Detection
        DmnDecisionTableResult detectionResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_DETECCAO)
            .singleResult()
            .evaluate(variables);

        Integer confianca = (Integer) detectionResult.getSingleResult().get("nivelConfiancaCPT");
        variables.putValue("nivelConfiancaCPT", confianca);

        // Act - Status
        DmnDecisionTableResult statusResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_STATUS)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(confianca).isEqualTo(90);
        assertThat(statusResult.getSingleResult().get("statusCPT"))
            .isEqualTo("CPT_PROVAVEL");
        assertThat(statusResult.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("VALIDACAO_MEDICA_OBRIGATORIA");
    }

    @Test
    @DisplayName("Should detect hypertension with medication use")
    void deveDetectarHipertensaoComMedicacao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicaoDeclarada", "pressão alta")
            .putValue("tempoCondicao", 24)
            .putValue("usaMedicacao", true)
            .putValue("historicoInternacao", false)
            .putValue("bmi", 26.0);

        // Act
        DmnDecisionTableResult detectionResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_DETECCAO)
            .singleResult()
            .evaluate(variables);

        Integer confianca = (Integer) detectionResult.getSingleResult().get("nivelConfiancaCPT");
        variables.putValue("nivelConfiancaCPT", confianca);

        DmnDecisionTableResult statusResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_STATUS)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(confianca).isEqualTo(90);
        assertThat(statusResult.getSingleResult().get("statusCPT"))
            .isEqualTo("CPT_PROVAVEL");
    }

    @Test
    @DisplayName("Should detect heart disease with hospitalization - highest confidence")
    void deveDetectarCardiopatiaComInternacao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicaoDeclarada", "infarto prévio")
            .putValue("tempoCondicao", 6)
            .putValue("usaMedicacao", true)
            .putValue("historicoInternacao", true)
            .putValue("bmi", 30.0);

        // Act
        DmnDecisionTableResult detectionResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_DETECCAO)
            .singleResult()
            .evaluate(variables);

        Integer confianca = (Integer) detectionResult.getSingleResult().get("nivelConfiancaCPT");
        variables.putValue("nivelConfiancaCPT", confianca);

        DmnDecisionTableResult statusResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_STATUS)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(confianca).isEqualTo(95);
        assertThat(statusResult.getSingleResult().get("statusCPT"))
            .isEqualTo("CPT_PROVAVEL");
    }

    @Test
    @DisplayName("Should detect cancer with maximum confidence")
    void deveDetectarCancerComConfiancaMaxima() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicaoDeclarada", "câncer")
            .putValue("tempoCondicao", 0)
            .putValue("usaMedicacao", false)
            .putValue("historicoInternacao", false)
            .putValue("bmi", 22.0);

        // Act
        DmnDecisionTableResult detectionResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_DETECCAO)
            .singleResult()
            .evaluate(variables);

        Integer confianca = (Integer) detectionResult.getSingleResult().get("nivelConfiancaCPT");

        // Assert
        assertThat(confianca).isEqualTo(100);
    }

    @Test
    @DisplayName("Should detect obesity from BMI - moderate confidence")
    void deveDetectarObesidadePorBMI() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicaoDeclarada", "nenhuma")
            .putValue("tempoCondicao", 0)
            .putValue("usaMedicacao", false)
            .putValue("historicoInternacao", false)
            .putValue("bmi", 36.0);

        // Act
        DmnDecisionTableResult detectionResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_DETECCAO)
            .singleResult()
            .evaluate(variables);

        Integer confianca = (Integer) detectionResult.getSingleResult().get("nivelConfiancaCPT");
        variables.putValue("nivelConfiancaCPT", confianca);

        DmnDecisionTableResult statusResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_STATUS)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(confianca).isEqualTo(70);
        assertThat(statusResult.getSingleResult().get("statusCPT"))
            .isEqualTo("SUSPEITA_ALTA");
        assertThat(statusResult.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("INVESTIGACAO_DETALHADA");
    }

    @Test
    @DisplayName("Should detect asthma with chronic medication")
    void deveDetectarAsmaComMedicacaoContinua() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("condicaoDeclarada", "asma")
            .putValue("tempoCondicao", 36)
            .putValue("usaMedicacao", true)
            .putValue("historicoInternacao", false)
            .putValue("bmi", 24.0);

        // Act
        DmnDecisionTableResult detectionResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_DETECCAO)
            .singleResult()
            .evaluate(variables);

        Integer confianca = (Integer) detectionResult.getSingleResult().get("nivelConfiancaCPT");
        variables.putValue("nivelConfiancaCPT", confianca);

        DmnDecisionTableResult statusResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_STATUS)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(confianca).isEqualTo(85);
        assertThat(statusResult.getSingleResult().get("statusCPT"))
            .isEqualTo("CPT_PROVAVEL");
    }

    @Test
    @DisplayName("Should classify as low suspicion - SUSPEITA_BAIXA")
    void deveClassificarComoSuspeitaBaixa() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("nivelConfiancaCPT", 45);

        // Act
        DmnDecisionTableResult statusResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_STATUS)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(statusResult.getSingleResult().get("statusCPT"))
            .isEqualTo("SUSPEITA_BAIXA");
        assertThat(statusResult.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("MONITORAMENTO_PERIODICO");
    }

    @Test
    @DisplayName("Should classify as no CPT - SEM_CPT")
    void deveClassificarComoSemCPT() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("nivelConfiancaCPT", 15);

        // Act
        DmnDecisionTableResult statusResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_STATUS)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(statusResult.getSingleResult().get("statusCPT"))
            .isEqualTo("SEM_CPT");
        assertThat(statusResult.getSingleResult().get("acaoNecessaria"))
            .isEqualTo("NENHUMA");
    }

    @Test
    @DisplayName("Should use COLLECT MAX policy correctly")
    void deveUsarPoliticaCOLLECTMAXCorretamente() {
        // Arrange - Patient declaring multiple conditions
        VariableMap variables = Variables.createVariables()
            .putValue("condicaoDeclarada", "diabetes e pressão alta")
            .putValue("tempoCondicao", 12)
            .putValue("usaMedicacao", true)
            .putValue("historicoInternacao", false)
            .putValue("bmi", 36.0); // Obesity

        // Act
        DmnDecisionTableResult detectionResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_DETECCAO)
            .singleResult()
            .evaluate(variables);

        // Should match diabetes (90), hypertension (90), and obesity (70)
        // COLLECT MAX should return 90
        Integer confianca = (Integer) detectionResult.getSingleResult().get("nivelConfiancaCPT");

        // Assert
        assertThat(confianca).isEqualTo(90);
    }
}
