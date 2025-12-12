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
 * Integration tests for DMN_ClassificacaoNPS decision table.
 *
 * Tests NPS classification and routing including:
 * - DETRATOR (0-6): Detractors requiring immediate action
 * - NEUTRO (7-8): Neutrals requiring standard follow-up
 * - PROMOTOR (9-10): Promoters for advocacy programs
 * - Recommended actions by classification
 * - Contact priority assignment
 * - Routing based on NPS classification (informationRequirement)
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Classificação NPS")
class CalculoNpsDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY_CLASSIFICACAO = "DMN_ClassificacaoNPS";
    private static final String DECISION_KEY_ROTEAMENTO = "DMN_RoteamentoNPS";

    @Test
    @DisplayName("Should classify as DETRATOR crítico - score 0-3")
    void deveClassificarComoDetratoCriticoScore0a3() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 2);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("DETRATOR");
        assertThat(result.getSingleResult().get("acaoRecomendada"))
            .isEqualTo("CONTATO_IMEDIATO_OUVIDORIA");
        assertThat(result.getSingleResult().get("prioridadeContato"))
            .isEqualTo("CRITICA");
    }

    @Test
    @DisplayName("Should classify as DETRATOR - score 4-6")
    void deveClassificarComoDetratoScore4a6() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 5);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("DETRATOR");
        assertThat(result.getSingleResult().get("acaoRecomendada"))
            .isEqualTo("CONTATO_PRIORIDADE_ALTA");
        assertThat(result.getSingleResult().get("prioridadeContato"))
            .isEqualTo("ALTA");
    }

    @Test
    @DisplayName("Should classify as NEUTRO - score 7-8")
    void deveClassificarComoNeutroScore7a8() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 7);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("NEUTRO");
        assertThat(result.getSingleResult().get("acaoRecomendada"))
            .isEqualTo("ACOMPANHAMENTO_PADRAO");
        assertThat(result.getSingleResult().get("prioridadeContato"))
            .isEqualTo("MEDIA");
    }

    @Test
    @DisplayName("Should classify as PROMOTOR - score 9-10")
    void deveClassificarComoPromotorScore9a10() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 10);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("PROMOTOR");
        assertThat(result.getSingleResult().get("acaoRecomendada"))
            .isEqualTo("PROGRAMA_ADVOCACY_RECONHECIMENTO");
        assertThat(result.getSingleResult().get("prioridadeContato"))
            .isEqualTo("BAIXA");
    }

    @Test
    @DisplayName("Should route DETRATOR to OUVIDORIA with 4h SLA")
    void deveRotearDetratoresParaOuvidoria() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 4);

        // Act - Classification
        DmnDecisionTableResult classificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        String classificacao = (String) classificationResult.getSingleResult().get("classificacaoNPS");
        variables.putValue("classificacaoNPS", classificacao);

        // Act - Routing
        DmnDecisionTableResult routingResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ROTEAMENTO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(routingResult.getSingleResult().get("destinatarioNPS"))
            .isEqualTo("OUVIDORIA");
        assertThat(routingResult.getSingleResult().get("slaRespostaNPS"))
            .isEqualTo(4);
    }

    @Test
    @DisplayName("Should route NEUTRO to RELACIONAMENTO with 48h SLA")
    void deveRotearNeutrosParaRelacionamento() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 8);

        // Act - Classification
        DmnDecisionTableResult classificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        String classificacao = (String) classificationResult.getSingleResult().get("classificacaoNPS");
        variables.putValue("classificacaoNPS", classificacao);

        // Act - Routing
        DmnDecisionTableResult routingResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ROTEAMENTO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(routingResult.getSingleResult().get("destinatarioNPS"))
            .isEqualTo("RELACIONAMENTO");
        assertThat(routingResult.getSingleResult().get("slaRespostaNPS"))
            .isEqualTo(48);
    }

    @Test
    @DisplayName("Should route PROMOTOR to MARKETING with 72h SLA")
    void deveRotearPromotoresParaMarketing() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 9);

        // Act - Classification
        DmnDecisionTableResult classificationResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        String classificacao = (String) classificationResult.getSingleResult().get("classificacaoNPS");
        variables.putValue("classificacaoNPS", classificacao);

        // Act - Routing
        DmnDecisionTableResult routingResult = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_ROTEAMENTO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(routingResult.getSingleResult().get("destinatarioNPS"))
            .isEqualTo("MARKETING");
        assertThat(routingResult.getSingleResult().get("slaRespostaNPS"))
            .isEqualTo(72);
    }

    @Test
    @DisplayName("Should handle boundary case - score 0 (minimum detractor)")
    void deveClassificarScore0ComoDetratoCritico() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 0);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("DETRATOR");
        assertThat(result.getSingleResult().get("prioridadeContato"))
            .isEqualTo("CRITICA");
    }

    @Test
    @DisplayName("Should handle boundary case - score 3 (critical detractor limit)")
    void deveClassificarScore3ComoDetratoCritico() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 3);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert - [0..3] should be critical detractor
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("DETRATOR");
        assertThat(result.getSingleResult().get("prioridadeContato"))
            .isEqualTo("CRITICA");
    }

    @Test
    @DisplayName("Should handle boundary case - score 4 (regular detractor)")
    void deveClassificarScore4ComoDetratoRegular() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 4);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert - [4..6] should be regular detractor (ALTA priority)
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("DETRATOR");
        assertThat(result.getSingleResult().get("prioridadeContato"))
            .isEqualTo("ALTA");
    }

    @Test
    @DisplayName("Should handle boundary case - score 6 (detractor limit)")
    void deveClassificarScore6ComoDetrato() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 6);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert - [4..6] should be detractor
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("DETRATOR");
    }

    @Test
    @DisplayName("Should handle boundary case - score 7 (neutral start)")
    void deveClassificarScore7ComoNeutro() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 7);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert - [7..8] should be neutral
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("NEUTRO");
    }

    @Test
    @DisplayName("Should handle boundary case - score 8 (neutral limit)")
    void deveClassificarScore8ComoNeutro() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 8);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert - [7..8] should be neutral
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("NEUTRO");
    }

    @Test
    @DisplayName("Should handle boundary case - score 9 (promoter start)")
    void deveClassificarScore9ComoPromotor() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 9);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert - [9..10] should be promoter
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("PROMOTOR");
    }

    @Test
    @DisplayName("Should handle boundary case - score 10 (maximum promoter)")
    void deveClassificarScore10ComoPromotor() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("scoreNPS", 10);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
            .singleResult()
            .evaluate(variables);

        // Assert - [9..10] should be promoter
        assertThat(result.getSingleResult().get("classificacaoNPS"))
            .isEqualTo("PROMOTOR");
    }

    @Test
    @DisplayName("Should validate informationRequirement - routing depends on classification")
    void deveValidarDependenciaEntreDecisoes() {
        // Arrange - Test all score ranges
        for (int score = 0; score <= 10; score++) {
            VariableMap variables = Variables.createVariables()
                .putValue("scoreNPS", score);

            // Act - Classification (required decision)
            DmnDecisionTableResult classificationResult = repositoryService
                .createDecisionDefinitionQuery()
                .decisionDefinitionKey(DECISION_KEY_CLASSIFICACAO)
                .singleResult()
                .evaluate(variables);

            String classificacao = (String) classificationResult.getSingleResult().get("classificacaoNPS");
            variables.putValue("classificacaoNPS", classificacao);

            // Act - Routing (dependent decision)
            DmnDecisionTableResult routingResult = repositoryService
                .createDecisionDefinitionQuery()
                .decisionDefinitionKey(DECISION_KEY_ROTEAMENTO)
                .singleResult()
                .evaluate(variables);

            // Assert - Routing should match classification
            String destinatario = (String) routingResult.getSingleResult().get("destinatarioNPS");

            if (score <= 6) {
                assertThat(destinatario).isEqualTo("OUVIDORIA");
            } else if (score <= 8) {
                assertThat(destinatario).isEqualTo("RELACIONAMENTO");
            } else {
                assertThat(destinatario).isEqualTo("MARKETING");
            }
        }
    }
}
