package com.healthplan.services.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

/**
 * AI Triage Service - Manages intelligent health triage conversations.
 *
 * Coordinates AI-powered symptom assessment, protocol matching,
 * and escalation decision making.
 *
 * @author Digital Experience Team
 * @since 2.0.0 - Phase 2 (AI Automation Layer)
 */
@Slf4j
@Service
public class AiTriageService {

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private ConversationContextService contextService;

    /**
     * Starts AI triage conversation for beneficiary.
     *
     * @param beneficiaryId Beneficiary unique identifier
     * @param initialSymptoms Initial symptom description
     * @return Conversation session ID
     * @throws AiTriageException if conversation cannot be started
     */
    public String startTriageConversation(String beneficiaryId, String initialSymptoms) {
        log.info("Starting AI triage for beneficiary: {}", beneficiaryId);

        try {
            // Generate conversation session
            String sessionId = UUID.randomUUID().toString();

            // Initialize conversation context
            ConversationContext context = ConversationContext.builder()
                .sessionId(sessionId)
                .beneficiaryId(beneficiaryId)
                .startTime(LocalDateTime.now())
                .currentState("INITIAL_ASSESSMENT")
                .symptoms(new ArrayList<>(List.of(initialSymptoms)))
                .build();

            contextService.saveContext(context);

            // Generate initial AI response
            String aiResponse = generateInitialResponse(initialSymptoms);

            log.info("AI triage started successfully. Session: {}", sessionId);
            return sessionId;

        } catch (Exception e) {
            log.error("Error starting AI triage for beneficiary: {}", beneficiaryId, e);
            throw new AiTriageException("Failed to start triage conversation", e);
        }
    }

    /**
     * Processes beneficiary response in ongoing triage.
     *
     * @param sessionId Active conversation session
     * @param userResponse Beneficiary's response
     * @return AI follow-up question or recommendation
     * @throws AiTriageException if response processing fails
     */
    public TriageResponse processResponse(String sessionId, String userResponse) {
        log.info("Processing triage response for session: {}", sessionId);

        try {
            ConversationContext context = contextService.getContext(sessionId);

            // Analyze response and extract symptoms
            List<String> extractedSymptoms = extractSymptoms(userResponse);
            context.getSymptoms().addAll(extractedSymptoms);

            // Update severity assessment
            int severityScore = calculateSeverity(context.getSymptoms());
            context.setSeverityScore(severityScore);

            // Determine next action
            TriageResponse response = determineNextAction(context);

            // Update context state
            context.setCurrentState(response.getNextState());
            context.setLastUpdate(LocalDateTime.now());
            contextService.updateContext(context);

            log.info("Triage response processed. Next action: {}", response.getActionType());
            return response;

        } catch (Exception e) {
            log.error("Error processing triage response for session: {}", sessionId, e);
            throw new AiTriageException("Failed to process response", e);
        }
    }

    /**
     * Queries clinical protocols based on symptoms.
     *
     * @param symptoms List of identified symptoms
     * @param patientAge Patient age for protocol matching
     * @return Matched clinical protocols
     */
    public List<ClinicalProtocol> queryProtocols(List<String> symptoms, Integer patientAge) {
        log.info("Querying protocols for {} symptoms, age: {}", symptoms.size(), patientAge);

        try {
            return protocolService.matchProtocols(symptoms, patientAge);
        } catch (Exception e) {
            log.error("Error querying protocols", e);
            throw new AiTriageException("Protocol query failed", e);
        }
    }

    /**
     * Generates AI-powered health recommendation.
     *
     * @param sessionId Conversation session
     * @return Generated recommendation
     */
    public HealthRecommendation generateRecommendation(String sessionId) {
        log.info("Generating recommendation for session: {}", sessionId);

        try {
            ConversationContext context = contextService.getContext(sessionId);
            List<ClinicalProtocol> protocols = protocolService.matchProtocols(
                context.getSymptoms(),
                context.getPatientAge()
            );

            return HealthRecommendation.builder()
                .sessionId(sessionId)
                .severity(mapSeverity(context.getSeverityScore()))
                .recommendation(generateRecommendationText(protocols, context))
                .protocols(protocols)
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error generating recommendation for session: {}", sessionId, e);
            throw new AiTriageException("Recommendation generation failed", e);
        }
    }

    /**
     * Checks if human escalation is needed.
     *
     * @param sessionId Conversation session
     * @return true if escalation required
     */
    public boolean needsEscalation(String sessionId) {
        try {
            ConversationContext context = contextService.getContext(sessionId);

            // Critical severity triggers immediate escalation
            if (context.getSeverityScore() >= 8) {
                log.warn("Critical severity detected. Escalation required: {}", sessionId);
                return true;
            }

            // Check for red flag symptoms
            boolean hasRedFlags = context.getSymptoms().stream()
                .anyMatch(this::isRedFlagSymptom);

            if (hasRedFlags) {
                log.warn("Red flag symptoms detected. Escalation required: {}", sessionId);
                return true;
            }

            // AI confidence too low
            if (context.getConfidenceScore() < 0.6) {
                log.info("Low AI confidence. Escalation recommended: {}", sessionId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking escalation need for session: {}", sessionId, e);
            // Fail safe - escalate on error
            return true;
        }
    }

    /**
     * Prepares context for human transfer.
     *
     * @param sessionId Conversation session
     * @return Transfer context package
     */
    public TransferContext prepareTransferContext(String sessionId) {
        log.info("Preparing transfer context for session: {}", sessionId);

        try {
            ConversationContext context = contextService.getContext(sessionId);

            return TransferContext.builder()
                .sessionId(sessionId)
                .beneficiaryId(context.getBeneficiaryId())
                .conversationHistory(contextService.getHistory(sessionId))
                .symptoms(context.getSymptoms())
                .severityScore(context.getSeverityScore())
                .aiRecommendations(context.getRecommendations())
                .protocols(context.getMatchedProtocols())
                .transferReason(determineTransferReason(context))
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error preparing transfer context for session: {}", sessionId, e);
            throw new AiTriageException("Transfer context preparation failed", e);
        }
    }

    /**
     * Registers completed AI service interaction.
     *
     * @param sessionId Conversation session
     * @param outcome Service outcome
     */
    public void registerInteraction(String sessionId, String outcome) {
        log.info("Registering AI interaction. Session: {}, Outcome: {}", sessionId, outcome);

        try {
            ConversationContext context = contextService.getContext(sessionId);

            AiInteractionRecord record = AiInteractionRecord.builder()
                .sessionId(sessionId)
                .beneficiaryId(context.getBeneficiaryId())
                .startTime(context.getStartTime())
                .endTime(LocalDateTime.now())
                .outcome(outcome)
                .severityScore(context.getSeverityScore())
                .numberOfExchanges(context.getExchangeCount())
                .escalated(context.isEscalated())
                .build();

            contextService.saveInteractionRecord(record);

            log.info("AI interaction registered successfully: {}", sessionId);

        } catch (Exception e) {
            log.error("Error registering AI interaction for session: {}", sessionId, e);
            // Don't throw - this is logging only
        }
    }

    // Private helper methods

    private String generateInitialResponse(String symptoms) {
        // AI response generation logic
        return "Entendo. Para te ajudar melhor, pode me dizer há quanto tempo sente esses sintomas?";
    }

    private List<String> extractSymptoms(String response) {
        // NLP symptom extraction
        return new ArrayList<>();
    }

    private int calculateSeverity(List<String> symptoms) {
        // Severity scoring algorithm (0-10)
        return 5;
    }

    private TriageResponse determineNextAction(ConversationContext context) {
        // Decision tree for next action
        return new TriageResponse();
    }

    private String mapSeverity(int score) {
        if (score >= 8) return "CRITICAL";
        if (score >= 6) return "HIGH";
        if (score >= 4) return "MEDIUM";
        return "LOW";
    }

    private String generateRecommendationText(List<ClinicalProtocol> protocols, ConversationContext context) {
        // Generate human-readable recommendation
        return "Baseado nos sintomas relatados...";
    }

    private boolean isRedFlagSymptom(String symptom) {
        // Check against red flag database
        List<String> redFlags = List.of(
            "chest pain", "difficulty breathing", "severe bleeding",
            "loss of consciousness", "stroke symptoms"
        );
        return redFlags.stream().anyMatch(symptom.toLowerCase()::contains);
    }

    private String determineTransferReason(ConversationContext context) {
        if (context.getSeverityScore() >= 8) return "CRITICAL_SEVERITY";
        if (context.getConfidenceScore() < 0.6) return "LOW_CONFIDENCE";
        return "COMPLEX_CASE";
    }
}
