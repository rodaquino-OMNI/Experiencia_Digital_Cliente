package com.austa.saude.experiencia.test.helpers;

import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Mock Service Factory
 *
 * Creates pre-configured mock services for testing
 * Reduces boilerplate code in test classes
 */
public class MockServiceFactory {

    /**
     * Create mock HealthScreeningService
     */
    public static Object createHealthScreeningService() {
        Object service = Mockito.mock(Object.class); // Replace with actual service interface

        // Configure default behavior
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("riskScore", 50);
        defaultResult.put("riskLevel", "MODERATE");
        defaultResult.put("chronicConditions", new String[]{"none"});

        when(service.toString()).thenReturn("MockHealthScreeningService");

        return service;
    }

    /**
     * Create mock RiskStratificationService
     */
    public static Object createRiskStratificationService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("riskLevel", "MODERATE");
        defaultResult.put("carePlanRequired", true);
        defaultResult.put("navigatorRequired", false);

        when(service.toString()).thenReturn("MockRiskStratificationService");

        return service;
    }

    /**
     * Create mock AuthorizationService
     */
    public static Object createAuthorizationService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("approved", true);
        defaultResult.put("authorizationNumber", "AUTH-123456");
        defaultResult.put("validUntil", "2025-12-31");

        when(service.toString()).thenReturn("MockAuthorizationService");

        return service;
    }

    /**
     * Create mock PredictiveService
     */
    public static Object createPredictiveService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("predictedRisk", 0.65);
        defaultResult.put("triggers", new String[]{"GAP_IN_CARE"});
        defaultResult.put("interventionRequired", true);

        when(service.toString()).thenReturn("MockPredictiveService");

        return service;
    }

    /**
     * Create mock NavigationService
     */
    public static Object createNavigationService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("navigatorAssigned", "NAVIGATOR-001");
        defaultResult.put("preferredProvider", "PROVIDER-TIER-A-123");
        defaultResult.put("appointmentScheduled", true);

        when(service.toString()).thenReturn("MockNavigationService");

        return service;
    }

    /**
     * Create mock NotificationService
     */
    public static Object createNotificationService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("sent", true);
        defaultResult.put("channel", "WHATSAPP");
        defaultResult.put("messageId", "MSG-123456");

        when(service.toString()).thenReturn("MockNotificationService");

        return service;
    }

    /**
     * Create mock IntegrationService (Tasy ERP)
     */
    public static Object createIntegrationService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("success", true);
        defaultResult.put("recordId", "TASY-123456");
        defaultResult.put("timestamp", System.currentTimeMillis());

        when(service.toString()).thenReturn("MockIntegrationService");

        return service;
    }

    /**
     * Create mock KafkaProducer
     */
    public static Object createKafkaProducer() {
        Object producer = Mockito.mock(Object.class);

        when(producer.toString()).thenReturn("MockKafkaProducer");

        return producer;
    }

    /**
     * Create mock ML Model Service
     */
    public static Object createMLModelService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultPrediction = new HashMap<>();
        defaultPrediction.put("prediction", "HIGH_RISK");
        defaultPrediction.put("confidence", 0.87);
        defaultPrediction.put("factors", new String[]{"age", "comorbidities", "utilization"});

        when(service.toString()).thenReturn("MockMLModelService");

        return service;
    }

    /**
     * Create mock OCR Service
     */
    public static Object createOCRService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("text", "Sample extracted text");
        defaultResult.put("confidence", 0.95);
        defaultResult.put("documentType", "MEDICAL_REPORT");

        when(service.toString()).thenReturn("MockOCRService");

        return service;
    }

    /**
     * Create mock NLP Service
     */
    public static Object createNLPService() {
        Object service = Mockito.mock(Object.class);

        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("intent", "AUTHORIZATION_REQUEST");
        defaultResult.put("confidence", 0.92);
        defaultResult.put("entities", new HashMap<>());

        when(service.toString()).thenReturn("MockNLPService");

        return service;
    }
}
