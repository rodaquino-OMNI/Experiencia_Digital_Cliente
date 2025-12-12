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
 * Integration tests for DMN_GatilhosProativos decision table.
 *
 * Tests proactive triggers identification using COLLECT policy:
 * - GAT-001: Annual checkup pending
 * - GAT-002: Medication running out
 * - GAT-003: Altered exam without return
 * - GAT-004: High hospitalization risk score
 * - GAT-005: Low treatment adherence
 * - GAT-008: Gap in care (chronic without consultation)
 * - GAT-009: Post-hospital discharge follow-up
 * - GAT-010: Pregnant without prenatal care
 * - GAT-011: Dissatisfaction detected
 * - GAT-012: Carency period expiring
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Identificação de Gatilhos Proativos")
class IdentificacaoGatilhosDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY = "DMN_GatilhosProativos";

    @Test
    @DisplayName("Should trigger GAT-001 - annual checkup pending")
    void deveIdentificarGatilho001CheckupAnual() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 400)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 30)
            .putValue("taxaAdesaoTratamento", 85)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 50)
            .putValue("diasAposAlta", 100)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 60);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - COLLECT policy should return all matching triggers
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-001");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("MEDIA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("check-up");
    }

    @Test
    @DisplayName("Should trigger GAT-002 - medication running out")
    void deveIdentificarGatilho002MedicamentoAcabando() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 5)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 20)
            .putValue("taxaAdesaoTratamento", 90)
            .putValue("temCondicaoCronica", true)
            .putValue("diasSemConsulta", 30)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-002");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("renovação receita");
    }

    @Test
    @DisplayName("Should trigger GAT-003 - altered exam without return appointment")
    void deveIdentificarGatilho003ExameAlteradoSemRetorno() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", true)
            .putValue("scorePredicaoInternacao", 40)
            .putValue("taxaAdesaoTratamento", 80)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 60)
            .putValue("diasAposAlta", 150)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 90);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-003");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("retorno");
    }

    @Test
    @DisplayName("Should trigger GAT-004 - high hospitalization risk (CRITICA)")
    void deveIdentificarGatilho004RiscoInternacaoAlto() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 85)
            .putValue("taxaAdesaoTratamento", 75)
            .putValue("temCondicaoCronica", true)
            .putValue("diasSemConsulta", 45)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 120);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-004");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("navegador");
    }

    @Test
    @DisplayName("Should trigger GAT-005 - low treatment adherence")
    void deveIdentificarGatilho005BaixaAdesaoTratamento() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 40)
            .putValue("taxaAdesaoTratamento", 45)
            .putValue("temCondicaoCronica", true)
            .putValue("diasSemConsulta", 60)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-005");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("MEDIA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("Nudge");
    }

    @Test
    @DisplayName("Should trigger GAT-008 - gap in care (chronic condition without consultation)")
    void deveIdentificarGatilho008GapInCare() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 35)
            .putValue("taxaAdesaoTratamento", 70)
            .putValue("temCondicaoCronica", true)
            .putValue("diasSemConsulta", 120)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-008");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("gap in care");
    }

    @Test
    @DisplayName("Should trigger GAT-009 - post-hospital discharge follow-up")
    void deveIdentificarGatilho009PosAltaHospitalar() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 50)
            .putValue("taxaAdesaoTratamento", 75)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 40)
            .putValue("diasAposAlta", 15)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-009");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("pós-alta");
    }

    @Test
    @DisplayName("Should trigger GAT-010 - pregnant without prenatal care (CRITICA)")
    void deveIdentificarGatilho010GestanteSemPreNatal() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 25)
            .putValue("taxaAdesaoTratamento", 80)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 30)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", true)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-010");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("pré-natal");
    }

    @Test
    @DisplayName("Should trigger GAT-011 - dissatisfaction detected")
    void deveIdentificarGatilho011InsatisfacaoDetectada() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 25)
            .putValue("taxaAdesaoTratamento", 85)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 40)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", true)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-011");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("satisfação");
    }

    @Test
    @DisplayName("Should trigger GAT-012 - carency period expiring")
    void deveIdentificarGatilho012VencimentoCarencia() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 25)
            .putValue("taxaAdesaoTratamento", 85)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 40)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 5);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-012");
        assertThat(result.getSingleResult().get("prioridade"))
            .isEqualTo("MEDIA");
        assertThat(result.getSingleResult().get("acaoSugerida"))
            .contains("carência");
    }

    @Test
    @DisplayName("Should trigger multiple gatilhos using COLLECT policy")
    void deveIdentificarMultiplosGatilhosSimultaneamente() {
        // Arrange - Scenario matching multiple triggers
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 400) // GAT-001
            .putValue("diasRestantesMedicamento", 5) // GAT-002
            .putValue("exameAlteradoSemRetorno", true) // GAT-003
            .putValue("scorePredicaoInternacao", 25)
            .putValue("taxaAdesaoTratamento", 85)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 40)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - COLLECT policy should return all matching triggers
        assertThat(result.getResultList()).hasSize(3);
        assertThat(result.collectEntries("gatilhoId"))
            .containsExactlyInAnyOrder("GAT-001", "GAT-002", "GAT-003");
    }

    @Test
    @DisplayName("Should not trigger any gatilho when all conditions are healthy")
    void naoDeveIdentificarGatilhosQuandoTudoNormal() {
        // Arrange - Healthy scenario
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 180)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 25)
            .putValue("taxaAdesaoTratamento", 85)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 40)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - No triggers should fire
        assertThat(result.getResultList()).isEmpty();
    }

    @Test
    @DisplayName("Should handle boundary cases - exactly 365 days checkup")
    void deveIdentificarGatilhoComExatamente365DiasCheckup() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("diasUltimoCheckup", 365)
            .putValue("diasRestantesMedicamento", 30)
            .putValue("exameAlteradoSemRetorno", false)
            .putValue("scorePredicaoInternacao", 25)
            .putValue("taxaAdesaoTratamento", 85)
            .putValue("temCondicaoCronica", false)
            .putValue("diasSemConsulta", 40)
            .putValue("diasAposAlta", 200)
            .putValue("gestante", false)
            .putValue("sentimentoNegativoRecente", false)
            .putValue("diasVencimentoCarencia", 100);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - >= 365 should trigger
        assertThat(result.getResultList()).hasSize(1);
        assertThat(result.getSingleResult().get("gatilhoId"))
            .isEqualTo("GAT-001");
    }
}
