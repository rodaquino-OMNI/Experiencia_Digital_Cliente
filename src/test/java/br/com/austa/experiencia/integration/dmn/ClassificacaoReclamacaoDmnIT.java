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
 * Integration tests for DMN_PrioridadeReclamacao decision table.
 *
 * Tests complaint prioritization including:
 * - CRITICA: Health impact + high risk, regulatory agencies, urgent denials
 * - ALTA: Repeated complaints, network care issues, health impact, time exceeded
 * - MEDIA: Billing issues, appointment problems
 * - BAIXA: Informational, suggestions, compliments
 * - Responsible assignment and SLA definition
 */
@SpringBootTest
@DisplayName("DMN Integration Tests - Classificação de Reclamação")
class ClassificacaoReclamacaoDmnIT extends BaseIntegrationTest {

    private static final String DECISION_KEY = "DMN_PrioridadeReclamacao";

    @Test
    @DisplayName("Should classify as CRITICA - health impact with high risk beneficiary")
    void deveClassificarComoCriticaImpactoSaudeAltoRisco() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "NEGACAO_AUTORIZACAO")
            .putValue("impactoSaude", true)
            .putValue("canalOrigem", "WHATSAPP")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "COMPLEXO")
            .putValue("tempoAbertura", 2);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(2);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("GERENTE_MEDICO");
    }

    @Test
    @DisplayName("Should classify as CRITICA - regulatory agency complaint")
    void deveClassificarComoCriticaOrgaoRegulador() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "ANS")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "ANS")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 12);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(4);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("DIRETORIA_OUVIDORIA");
    }

    @Test
    @DisplayName("Should classify as CRITICA - urgent procedure denial with health impact")
    void deveClassificarComoCriticaNegacaoProcedimentoUrgente() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "NEGATIVA_COBERTURA")
            .putValue("impactoSaude", true)
            .putValue("canalOrigem", "TELEFONE")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "MODERADO")
            .putValue("tempoAbertura", 6);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(4);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("AUDITOR_MEDICO_SENIOR");
    }

    @Test
    @DisplayName("Should classify as ALTA - repeated unresolved complaint")
    void deveClassificarComoAltaReclamacaoRepetida() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "AGENDAMENTO")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "APP")
            .putValue("reclamacaoRepetida", true)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 24);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(8);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("COORDENADOR_OUVIDORIA");
    }

    @Test
    @DisplayName("Should classify as ALTA - network care complaint")
    void deveClassificarComoAltaAtendimentoRede() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "ATENDIMENTO_REDE")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "WHATSAPP")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "MODERADO")
            .putValue("tempoAbertura", 10);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(12);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("GESTOR_REDE_CREDENCIADA");
    }

    @Test
    @DisplayName("Should classify as ALTA - health impact")
    void deveClassificarComoAltaImpactoSaude() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "CONSULTA")
            .putValue("impactoSaude", true)
            .putValue("canalOrigem", "TELEFONE")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 12);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(8);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("NAVEGADOR_SENIOR");
    }

    @Test
    @DisplayName("Should classify as ALTA - time exceeded > 48h")
    void deveClassificarComoAltaTempoExcedido() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "FATURAMENTO")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "EMAIL")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 72);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("ALTA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(4);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("COORDENADOR_OUVIDORIA");
    }

    @Test
    @DisplayName("Should classify as MEDIA - billing complaint")
    void deveClassificarComoMediaFaturamento() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "COBRANCA")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "APP")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 12);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("MEDIA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(24);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("ANALISTA_FINANCEIRO");
    }

    @Test
    @DisplayName("Should classify as MEDIA - appointment issues")
    void deveClassificarComoMediaAgendamento() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "AGENDAMENTO")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "WHATSAPP")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 18);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("MEDIA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(24);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("ATENDENTE_OUVIDORIA");
    }

    @Test
    @DisplayName("Should classify as BAIXA - informational complaint")
    void deveClassificarComoBaixaInformacional() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "INFORMACAO")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "EMAIL")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 24);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("BAIXA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(48);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("ATENDENTE_BACKOFFICE");
    }

    @Test
    @DisplayName("Should classify as BAIXA - suggestion or compliment")
    void deveClassificarComoBaixaSugestao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "ELOGIO")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "APP")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 12);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("BAIXA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(72);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("QUALIDADE");
    }

    @Test
    @DisplayName("Should classify as MEDIA - default rule for unknown type")
    void deveClassificarComoMediaPorPadrao() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "DESCONHECIDO")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "WHATSAPP")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 15);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("MEDIA");
        assertThat(result.getSingleResult().get("slaResolucao"))
            .isEqualTo(24);
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("ATENDENTE_OUVIDORIA");
    }

    @Test
    @DisplayName("Should handle boundary case - exactly 48 hours")
    void deveManterMediaComExatamente48Horas() {
        // Arrange
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "AGENDAMENTO")
            .putValue("impactoSaude", false)
            .putValue("canalOrigem", "APP")
            .putValue("reclamacaoRepetida", false)
            .putValue("classificacaoRisco", "BAIXO")
            .putValue("tempoAbertura", 48);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - >48 triggers ALTA, =48 should be MEDIA
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("MEDIA");
    }

    @Test
    @DisplayName("Should use FIRST hit policy - health impact takes precedence")
    void deveUsarPoliticaFIRSTComPrecedenciaImpactoSaude() {
        // Arrange - Could match multiple rules
        VariableMap variables = Variables.createVariables()
            .putValue("tipoReclamacao", "FATURAMENTO")
            .putValue("impactoSaude", true)
            .putValue("canalOrigem", "ANS")
            .putValue("reclamacaoRepetida", true)
            .putValue("classificacaoRisco", "ALTO")
            .putValue("tempoAbertura", 72);

        // Act
        DmnDecisionTableResult result = repositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DECISION_KEY)
            .singleResult()
            .evaluate(variables);

        // Assert - First critical rule (health impact + high risk) should fire
        assertThat(result.getSingleResult().get("prioridadeReclamacao"))
            .isEqualTo("CRITICA");
        assertThat(result.getSingleResult().get("responsavelReclamacao"))
            .isEqualTo("GERENTE_MEDICO");
    }
}
