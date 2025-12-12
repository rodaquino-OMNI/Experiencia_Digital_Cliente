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
 * Integration tests for DMN_PrioridadeAtendimento decision table.
 *
 * Tests navigation priority assignment including:
 * - Priority 1 (COMPLEXO risk, ALTO+INTERNACAO, critical cases)
 * - Priority 2 (ALTO+complaints, ALTO+VIP, ALTO+waiting)
 * - Priority 3 (ALTO general, MODERADO+complaint/VIP)
 * - Priority 4-5 (MODERADO waiting, MODERADO general)
 * - Priority 7 (BAIXO general)
 * - Navigator assignment (SENIOR, PLENO, JUNIOR)
 * - Urgent flag and maximum waiting time (SLA)
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Prioridade de Atendimento")
class PrioridadeAtendimentoDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY = "DMN_PrioridadeAtendimento";

    @Test
    @DisplayName("Should assign priority 1 for COMPLEXO risk - maximum priority")
    void deveAtribuirPrioridade1ParaRiscoComplexo() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "COMPLEXO")
            .putValue("tipoJornada", "ONBOARDING")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(1);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(30);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_SENIOR");
        assertThat(result.getSingleResult().get("flagUrgente"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should assign priority 1 for ALTO risk with hospitalization journey")
    void deveAtribuirPrioridade1ParaAltoRiscoComInternacao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tipoJornada", "INTERNACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(1);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(60);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_SENIOR");
        assertThat(result.getSingleResult().get("flagUrgente"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should assign priority 2 for ALTO risk with open complaint")
    void deveAtribuirPrioridade2ParaAltoRiscoComReclamacao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", true)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(2);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(60);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_SENIOR");
        assertThat(result.getSingleResult().get("flagUrgente"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should assign priority 2 for ALTO risk with VIP beneficiary")
    void deveAtribuirPrioridade2ParaAltoRiscoVIP() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(2);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(60);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_SENIOR");
        assertThat(result.getSingleResult().get("flagUrgente"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should assign priority 2 for ALTO risk waiting >= 3 days")
    void deveAtribuirPrioridade2ParaAltoRiscoComEspera() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 4)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(2);
        assertThat(result.getSingleResult().get("flagUrgente"))
            .isEqualTo(true);
    }

    @Test
    @DisplayName("Should assign priority 3 for ALTO risk general")
    void deveAtribuirPrioridade3ParaAltoRiscoGeral() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(3);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(120);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_PLENO");
        assertThat(result.getSingleResult().get("flagUrgente"))
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Should assign priority 3 for MODERADO risk with complaint")
    void deveAtribuirPrioridade3ParaModeradoRiscoComReclamacao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "MODERADO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", true)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(3);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_PLENO");
    }

    @Test
    @DisplayName("Should assign priority 3 for MODERADO risk with VIP")
    void deveAtribuirPrioridade3ParaModeradoRiscoVIP() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "MODERADO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(3);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_PLENO");
    }

    @Test
    @DisplayName("Should assign priority 4 for MODERADO risk waiting >= 5 days")
    void deveAtribuirPrioridade4ParaModeradoRiscoComEspera() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "MODERADO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 7)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(4);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(240);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_PLENO");
    }

    @Test
    @DisplayName("Should assign priority 5 for MODERADO risk general")
    void deveAtribuirPrioridade5ParaModeradoRiscoGeral() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "MODERADO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(5);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(480);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_JUNIOR");
        assertThat(result.getSingleResult().get("flagUrgente"))
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Should assign priority 5 for BAIXO risk with complaint")
    void deveAtribuirPrioridade5ParaBaixoRiscoComReclamacao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", true)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(5);
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_JUNIOR");
    }

    @Test
    @DisplayName("Should assign priority 7 for BAIXO risk general - lowest priority")
    void deveAtribuirPrioridade7ParaBaixoRiscoGeral() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 0)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(7);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(1440); // 24 hours
        assertThat(result.getSingleResult().get("navegadorPreferencial"))
            .isEqualTo("NAVEGADOR_JUNIOR");
        assertThat(result.getSingleResult().get("flagUrgente"))
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Should handle boundary case - exactly 3 days waiting for ALTO")
    void deveEscalonarPrioridadeComExatamente3DiasEspera() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tipoJornada", "NAVEGACAO")
            .putValue("diasEmEspera", 3)
            .putValue("tentativasContato", 0)
            .putValue("reclamacaoAberta", false)
            .putValue("vip", false);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - >= 3 days should trigger priority 2
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(2);
    }

    @Test
    @DisplayName("Should use FIRST hit policy - COMPLEXO takes precedence")
    void deveUsarPoliticaFIRSTComPrecedenciaComplexo() {
        // Arrange - Multiple conditions that could match different rules
        VariableMap variables = Variables.createVariables()
            .putValue("classificacaoRisco", "COMPLEXO")
            .putValue("tipoJornada", "INTERNACAO")
            .putValue("diasEmEspera", 5)
            .putValue("tentativasContato", 3)
            .putValue("reclamacaoAberta", true)
            .putValue("vip", true);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - First rule (COMPLEXO) should fire
        assertThat(result.getSingleResult().get("prioridadeFinal"))
            .isEqualTo(1);
        assertThat(result.getSingleResult().get("tempoMaximoEspera"))
            .isEqualTo(30); // COMPLEXO has 30 min SLA, not 60 min from INTERNACAO
    }
}
