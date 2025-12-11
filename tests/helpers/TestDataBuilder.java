package com.austa.saude.experiencia.test.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Test Data Builder
 *
 * Provides fluent API for creating test data objects
 * Ensures consistent and maintainable test data across all tests
 */
public class TestDataBuilder {

    /**
     * Beneficiary Builder
     */
    public static class BeneficiaryBuilder {
        private String id = UUID.randomUUID().toString();
        private String name = "João Silva";
        private String cpf = "12345678900";
        private String email = "joao.silva@email.com";
        private String phone = "+5511999999999";
        private LocalDate birthDate = LocalDate.of(1980, 1, 1);
        private String riskLevel = "MODERATE";
        private List<String> chronicConditions = new ArrayList<>();
        private Map<String, Object> healthData = new HashMap<>();
        private boolean cptDetected = false;

        public static BeneficiaryBuilder aBeneficiary() {
            return new BeneficiaryBuilder();
        }

        public BeneficiaryBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public BeneficiaryBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public BeneficiaryBuilder withCpf(String cpf) {
            this.cpf = cpf;
            return this;
        }

        public BeneficiaryBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public BeneficiaryBuilder withPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public BeneficiaryBuilder withBirthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public BeneficiaryBuilder withRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public BeneficiaryBuilder withChronicConditions(String... conditions) {
            this.chronicConditions = Arrays.asList(conditions);
            return this;
        }

        public BeneficiaryBuilder withHealthData(Map<String, Object> healthData) {
            this.healthData = healthData;
            return this;
        }

        public BeneficiaryBuilder withCptDetected(boolean detected) {
            this.cptDetected = detected;
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> beneficiary = new HashMap<>();
            beneficiary.put("beneficiaryId", id);
            beneficiary.put("name", name);
            beneficiary.put("cpf", cpf);
            beneficiary.put("email", email);
            beneficiary.put("phone", phone);
            beneficiary.put("birthDate", birthDate);
            beneficiary.put("riskLevel", riskLevel);
            beneficiary.put("chronicConditions", chronicConditions);
            beneficiary.put("healthData", healthData);
            beneficiary.put("cptDetected", cptDetected);
            return beneficiary;
        }
    }

    /**
     * Authorization Request Builder
     */
    public static class AuthorizationBuilder {
        private String requestId = UUID.randomUUID().toString();
        private String beneficiaryId = "BEN-123";
        private String procedureCode = "40301010";
        private String procedureDescription = "Consulta médica";
        private String requestorType = "PROVIDER";
        private String urgency = "ROUTINE";
        private LocalDateTime requestDate = LocalDateTime.now();
        private Map<String, Object> clinicalData = new HashMap<>();

        public static AuthorizationBuilder anAuthorization() {
            return new AuthorizationBuilder();
        }

        public AuthorizationBuilder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public AuthorizationBuilder withBeneficiaryId(String beneficiaryId) {
            this.beneficiaryId = beneficiaryId;
            return this;
        }

        public AuthorizationBuilder withProcedureCode(String code) {
            this.procedureCode = code;
            return this;
        }

        public AuthorizationBuilder withProcedureDescription(String description) {
            this.procedureDescription = description;
            return this;
        }

        public AuthorizationBuilder withUrgency(String urgency) {
            this.urgency = urgency;
            return this;
        }

        public AuthorizationBuilder withRequestorType(String type) {
            this.requestorType = type;
            return this;
        }

        public AuthorizationBuilder withClinicalData(Map<String, Object> data) {
            this.clinicalData = data;
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> authorization = new HashMap<>();
            authorization.put("requestId", requestId);
            authorization.put("beneficiaryId", beneficiaryId);
            authorization.put("procedureCode", procedureCode);
            authorization.put("procedureDescription", procedureDescription);
            authorization.put("requestorType", requestorType);
            authorization.put("urgency", urgency);
            authorization.put("requestDate", requestDate);
            authorization.put("clinicalData", clinicalData);
            return authorization;
        }
    }

    /**
     * Interaction Builder
     */
    public static class InteractionBuilder {
        private String interactionId = UUID.randomUUID().toString();
        private String beneficiaryId = "BEN-123";
        private String channel = "WHATSAPP";
        private String category = "ROUTINE";
        private String subject = "Dúvida sobre cobertura";
        private String content = "Gostaria de saber se meu plano cobre cirurgia bariátrica";
        private LocalDateTime timestamp = LocalDateTime.now();

        public static InteractionBuilder anInteraction() {
            return new InteractionBuilder();
        }

        public InteractionBuilder withId(String id) {
            this.interactionId = id;
            return this;
        }

        public InteractionBuilder withBeneficiaryId(String beneficiaryId) {
            this.beneficiaryId = beneficiaryId;
            return this;
        }

        public InteractionBuilder withChannel(String channel) {
            this.channel = channel;
            return this;
        }

        public InteractionBuilder withCategory(String category) {
            this.category = category;
            return this;
        }

        public InteractionBuilder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public InteractionBuilder withContent(String content) {
            this.content = content;
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> interaction = new HashMap<>();
            interaction.put("interactionId", interactionId);
            interaction.put("beneficiaryId", beneficiaryId);
            interaction.put("channel", channel);
            interaction.put("category", category);
            interaction.put("subject", subject);
            interaction.put("content", content);
            interaction.put("timestamp", timestamp);
            return interaction;
        }
    }

    /**
     * Care Plan Builder
     */
    public static class CarePlanBuilder {
        private String planId = UUID.randomUUID().toString();
        private String beneficiaryId = "BEN-123";
        private String riskLevel = "MODERATE";
        private List<String> protocols = new ArrayList<>();
        private List<Map<String, Object>> interventions = new ArrayList<>();
        private LocalDate startDate = LocalDate.now();
        private LocalDate reviewDate = LocalDate.now().plusMonths(3);

        public static CarePlanBuilder aCarePlan() {
            return new CarePlanBuilder();
        }

        public CarePlanBuilder withPlanId(String planId) {
            this.planId = planId;
            return this;
        }

        public CarePlanBuilder withBeneficiaryId(String beneficiaryId) {
            this.beneficiaryId = beneficiaryId;
            return this;
        }

        public CarePlanBuilder withRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public CarePlanBuilder withProtocols(String... protocols) {
            this.protocols = Arrays.asList(protocols);
            return this;
        }

        public CarePlanBuilder withInterventions(List<Map<String, Object>> interventions) {
            this.interventions = interventions;
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> carePlan = new HashMap<>();
            carePlan.put("planId", planId);
            carePlan.put("beneficiaryId", beneficiaryId);
            carePlan.put("riskLevel", riskLevel);
            carePlan.put("protocols", protocols);
            carePlan.put("interventions", interventions);
            carePlan.put("startDate", startDate);
            carePlan.put("reviewDate", reviewDate);
            return carePlan;
        }
    }

    /**
     * Predictive Event Builder
     */
    public static class PredictiveEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private String beneficiaryId = "BEN-123";
        private String eventType = "GAP_IN_CARE";
        private double score = 0.75;
        private String trigger = "EXAME_VENCIDO";
        private Map<String, Object> metadata = new HashMap<>();

        public static PredictiveEventBuilder aPredictiveEvent() {
            return new PredictiveEventBuilder();
        }

        public PredictiveEventBuilder withEventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public PredictiveEventBuilder withBeneficiaryId(String beneficiaryId) {
            this.beneficiaryId = beneficiaryId;
            return this;
        }

        public PredictiveEventBuilder withEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public PredictiveEventBuilder withScore(double score) {
            this.score = score;
            return this;
        }

        public PredictiveEventBuilder withTrigger(String trigger) {
            this.trigger = trigger;
            return this;
        }

        public PredictiveEventBuilder withMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", eventId);
            event.put("beneficiaryId", beneficiaryId);
            event.put("eventType", eventType);
            event.put("score", score);
            event.put("trigger", trigger);
            event.put("metadata", metadata);
            event.put("timestamp", LocalDateTime.now());
            return event;
        }
    }
}
