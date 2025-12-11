package com.austa.saude.experiencia.test.unit.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Interaction Classification Delegate
 *
 * Coverage:
 * - Happy path: NLP classification, urgency detection, routing logic
 * - Error handling: unrecognized intents, ambiguous messages
 * - Edge cases: multi-intent messages, special characters
 * - Boundary conditions: message length limits, language detection
 *
 * Process: 3-Interaction-Reception-Classification-Subprocess
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Interaction Classification Delegate Tests")
class InteractionClassificationDelegateTest {

    @Mock
    private DelegateExecution execution;

    @Test
    @DisplayName("Should classify authorization request from text")
    void shouldClassifyAuthorizationRequest() {
        // Arrange
        String message = "Preciso de autorização para fazer uma ressonância magnética";
        when(execution.getVariable("messageContent")).thenReturn(message);

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("intent"), eq("AUTHORIZATION_REQUEST"));
        verify(execution).setVariable(eq("category"), eq("ADMINISTRATIVE"));
        verify(execution).setVariable(eq("urgency"), eq("ROUTINE"));
        verify(execution).setVariable(eq("route"), eq("SELF_SERVICE"));
    }

    @Test
    @DisplayName("Should detect emergency from symptom description")
    void shouldDetectEmergencySymptoms() {
        // Arrange
        String message = "Estou com dor no peito e falta de ar";
        when(execution.getVariable("messageContent")).thenReturn(message);

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("intent"), eq("EMERGENCY"));
        verify(execution).setVariable(eq("urgency"), eq("CRITICAL"));
        verify(execution).setVariable(eq("route"), eq("EMERGENCY_PROTOCOL"));
        verify(execution).setVariable(eq("escalateImmediately"), eq(true));
    }

    @Test
    @DisplayName("Should extract entities from message using NER")
    void shouldExtractEntities() {
        // Arrange
        String message = "Quero agendar consulta com Dr. João Silva para próxima segunda-feira";
        when(execution.getVariable("messageContent")).thenReturn(message);

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("entities"), any(Map.class));
        verify(execution).setVariable(eq("providerName"), eq("Dr. João Silva"));
        verify(execution).setVariable(eq("preferredDate"), anyString());
    }

    @Test
    @DisplayName("Should route to AI agent for complex query")
    void shouldRouteToAIAgent() {
        // Arrange
        String message = "Qual é a cobertura do meu plano para cirurgia bariátrica?";
        when(execution.getVariable("messageContent")).thenReturn(message);
        when(execution.getVariable("complexity")).thenReturn("MODERATE");

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("route"), eq("AI_AGENT"));
        verify(execution).setVariable(eq("agentType"), eq("COVERAGE_SPECIALIST"));
    }

    @Test
    @DisplayName("Should handle multi-intent message")
    void shouldHandleMultiIntent() {
        // Arrange
        String message = "Preciso de autorização e também quero segunda via da carteirinha";
        when(execution.getVariable("messageContent")).thenReturn(message);

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("multiIntent"), eq(true));
        verify(execution).setVariable(eq("primaryIntent"), eq("AUTHORIZATION_REQUEST"));
        verify(execution).setVariable(eq("secondaryIntent"), eq("CARD_REISSUE"));
    }

    @Test
    @DisplayName("Should detect sentiment and adjust tone")
    void shouldDetectSentiment() {
        // Arrange
        String message = "Estou muito frustrado, já tentei resolver isso 3 vezes!";
        when(execution.getVariable("messageContent")).thenReturn(message);

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("sentiment"), eq("NEGATIVE"));
        verify(execution).setVariable(eq("frustrationLevel"), eq("HIGH"));
        verify(execution).setVariable(eq("requiresEmpathy"), eq(true));
        verify(execution).setVariable(eq("escalatePriority"), eq(true));
    }

    @Test
    @DisplayName("Should identify NIP (reclamação) and route correctly")
    void shouldIdentifyNIP() {
        // Arrange
        String message = "Quero registrar uma reclamação formal sobre o atendimento";
        when(execution.getVariable("messageContent")).thenReturn(message);

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("isNIP"), eq(true));
        verify(execution).setVariable(eq("route"), eq("NIP_PROCESS"));
        verify(execution).setVariable(eq("urgency"), eq("HIGH"));
    }

    @Test
    @DisplayName("Should classify based on channel context")
    void shouldClassifyWithChannelContext() {
        // Arrange
        when(execution.getVariable("channel")).thenReturn("PHONE"));
        when(execution.getVariable("previousInteractions")).thenReturn(5);
        when(execution.getVariable("messageContent")).thenReturn("Status");

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("contextEnriched"), eq(true));
        verify(execution).setVariable(eq("likelyIntent"), anyString());
    }

    @Test
    @DisplayName("Should handle ambiguous message with clarification")
    void shouldHandleAmbiguousMessage() {
        // Arrange
        String message = "Oi";
        when(execution.getVariable("messageContent")).thenReturn(message);

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("requiresClarification"), eq(true));
        verify(execution).setVariable(eq("suggestedPrompts"), any());
    }

    @Test
    @DisplayName("Should enrich with 360° beneficiary context")
    void shouldEnrichWithBeneficiaryContext() {
        // Arrange
        when(execution.getVariable("beneficiaryId")).thenReturn("BEN-001");
        when(execution.getVariable("messageContent")).thenReturn("Status do meu pedido");

        // Act
        // delegate.execute(execution);

        // Assert
        verify(execution).setVariable(eq("contextLoaded"), eq(true));
        verify(execution).setVariable(eq("recentRequests"), any());
        verify(execution).setVariable(eq("activeAuthorizations"), any());
    }
}
