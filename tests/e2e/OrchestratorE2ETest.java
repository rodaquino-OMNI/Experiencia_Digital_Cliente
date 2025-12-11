package com.austa.saude.experiencia.test.e2e;

import com.austa.saude.experiencia.test.helpers.CamundaTestHelper;
import com.austa.saude.experiencia.test.helpers.TestDataBuilder;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * End-to-End Tests for Orchestrator Process
 *
 * Tests complete customer journey from onboarding to follow-up
 * Validates all 10 subprocesses working together
 *
 * Coverage:
 * - Complete beneficiary lifecycle
 * - All subprocess interactions
 * - Message correlation across processes
 * - Error handling and compensation
 * - Performance under realistic scenarios
 *
 * BPMN: orchestrator-main-process.bpmn
 * Subprocesses: All 10 specialized subprocesses
 */
@SpringBootTest
@Testcontainers
@DisplayName("Orchestrator End-to-End Tests")
class OrchestratorE2ETest {

    @Autowired
    private ProcessEngine processEngine;

    private CamundaTestHelper testHelper;

    @BeforeEach
    void setUp() {
        testHelper = new CamundaTestHelper(processEngine);
    }

    @Test
    @DisplayName("Should complete full journey: onboarding to first authorization")
    @Deployment(resources = {
        "bpmn/orchestrator-main-process.bpmn",
        "bpmn/1-onboarding-screening-subprocess.bpmn",
        "bpmn/2-proactive-monitoring-subprocess.bpmn",
        "bpmn/3-interaction-reception-subprocess.bpmn",
        "bpmn/4-resolution-authorization-subprocess.bpmn",
        "bpmn/8-followup-nps-subprocess.bpmn"
    })
    void shouldCompleteFullJourney() {
        // PHASE 1: ONBOARDING
        Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
            .withId("BEN-E2E-001")
            .withName("Maria Santos")
            .withEmail("maria.santos@email.com")
            .withPhone("+5511988887777")
            .build();

        ProcessInstance orchestrator = testHelper.startProcess(
            "orchestrator-main-process",
            "BEN-E2E-001",
            variables
        );

        assertThat(orchestrator).isActive();

        // Verify onboarding completed
        testHelper.assertHasPassed(orchestrator, "CallActivity_Onboarding");
        testHelper.assertVariableEquals(orchestrator, "onboardingComplete", true);

        String riskLevel = (String) testHelper.getVariable(orchestrator, "riskLevel");
        assertThat(riskLevel).isIn("LOW", "MODERATE", "HIGH", "CRITICAL");

        // PHASE 2: PROACTIVE MONITORING
        // Verify proactive monitoring is active
        testHelper.assertHasPassed(orchestrator, "CallActivity_ProactiveMonitoring");

        // PHASE 3: FIRST INTERACTION
        // Simulate beneficiary requesting authorization
        Map<String, Object> interactionData = TestDataBuilder.InteractionBuilder.anInteraction()
            .withBeneficiaryId("BEN-E2E-001")
            .withChannel("WHATSAPP")
            .withCategory("AUTHORIZATION_REQUEST")
            .withContent("Preciso de autorização para consulta com cardiologista")
            .build();

        testHelper.setVariable(orchestrator, "interactionReceived", true);
        testHelper.setVariable(orchestrator, "interactionData", interactionData);

        // Verify interaction classification
        testHelper.assertHasPassed(orchestrator, "CallActivity_InteractionClassification");
        testHelper.assertVariableEquals(orchestrator, "intent", "AUTHORIZATION_REQUEST");

        // PHASE 4: AUTHORIZATION
        Map<String, Object> authRequest = TestDataBuilder.AuthorizationBuilder.anAuthorization()
            .withBeneficiaryId("BEN-E2E-001")
            .withProcedureCode("40301012") // Cardiologist consultation
            .withUrgency("ROUTINE")
            .build();

        testHelper.setVariable(orchestrator, "authorizationRequest", authRequest);

        // Verify authorization processed
        testHelper.assertHasPassed(orchestrator, "CallActivity_Authorization");

        Boolean approved = (Boolean) testHelper.getVariable(orchestrator, "approved");
        assertThat(approved).isTrue();

        String authNumber = (String) testHelper.getVariable(orchestrator, "authorizationNumber");
        assertThat(authNumber).isNotNull().startsWith("AUTH-");

        // PHASE 5: FOLLOW-UP
        // Simulate post-consultation follow-up
        testHelper.setVariable(orchestrator, "consultationCompleted", true);

        testHelper.assertHasPassed(orchestrator, "CallActivity_FollowUp");

        // Verify NPS sent
        testHelper.assertVariableEquals(orchestrator, "npsSent", true);

        // COMPLETE
        assertThat(orchestrator).isEnded();

        // Verify all key metrics recorded
        assertThat(testHelper.getVariable(orchestrator, "totalInteractions")).isNotNull();
        assertThat(testHelper.getVariable(orchestrator, "authorizationsProcessed")).isEqualTo(1);
        assertThat(testHelper.getVariable(orchestrator, "journeyComplete")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should handle high-risk beneficiary with navigation")
    @Deployment(resources = {
        "bpmn/orchestrator-main-process.bpmn",
        "bpmn/1-onboarding-screening-subprocess.bpmn",
        "bpmn/5-navigation-care-coordination-subprocess.bpmn",
        "bpmn/6-chronic-disease-management-subprocess.bpmn"
    })
    void shouldHandleHighRiskWithNavigation() {
        // Arrange - High-risk beneficiary
        Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
            .withId("BEN-HIGHRISK-001")
            .withRiskLevel("HIGH")
            .withChronicConditions("diabetes", "hypertension", "COPD")
            .build();

        variables.put("riskScore", 85);

        // Act
        ProcessInstance orchestrator = testHelper.startProcess(
            "orchestrator-main-process",
            "BEN-HIGHRISK-001",
            variables
        );

        // Assert - Navigation should be triggered
        testHelper.assertHasPassed(orchestrator, "CallActivity_Onboarding");
        testHelper.assertVariableEquals(orchestrator, "navigatorRequired", true);

        // Verify navigator assignment
        testHelper.assertHasPassed(orchestrator, "CallActivity_NavigationCoordination");

        String navigatorId = (String) testHelper.getVariable(orchestrator, "navigatorId");
        assertThat(navigatorId).isNotNull();

        // Verify care plan created
        testHelper.assertVariableEquals(orchestrator, "carePlanCreated", true);

        // Verify chronic disease management active
        testHelper.assertHasPassed(orchestrator, "CallActivity_ChronicDiseaseManagement");
    }

    @Test
    @DisplayName("Should handle NIP (complaint) escalation")
    @Deployment(resources = {
        "bpmn/orchestrator-main-process.bpmn",
        "bpmn/3-interaction-reception-subprocess.bpmn",
        "bpmn/7-special-cases-nip-subprocess.bpmn"
    })
    void shouldHandleNIPEscalation() {
        // Arrange
        Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
            .withId("BEN-NIP-001")
            .build();

        ProcessInstance orchestrator = testHelper.startProcess(
            "orchestrator-main-process",
            "BEN-NIP-001",
            variables
        );

        // Act - Simulate complaint
        Map<String, Object> nipInteraction = TestDataBuilder.InteractionBuilder.anInteraction()
            .withBeneficiaryId("BEN-NIP-001")
            .withCategory("COMPLAINT")
            .withContent("Gostaria de registrar uma reclamação formal...")
            .build();

        testHelper.setVariable(orchestrator, "interactionData", nipInteraction);
        testHelper.setVariable(orchestrator, "isNIP", true);

        // Assert - NIP process should be triggered
        testHelper.assertHasPassed(orchestrator, "CallActivity_SpecialCasesNIP");

        String nipProtocol = (String) testHelper.getVariable(orchestrator, "nipProtocol");
        assertThat(nipProtocol).isNotNull().startsWith("NIP-");

        testHelper.assertVariableEquals(orchestrator, "nipEscalated", true);
        testHelper.assertVariableEquals(orchestrator, "ansNotificationRequired", true);
    }

    @Test
    @DisplayName("Should handle process cancellation across all subprocesses")
    @Deployment(resources = {
        "bpmn/orchestrator-main-process.bpmn",
        "bpmn/1-onboarding-screening-subprocess.bpmn",
        "bpmn/2-proactive-monitoring-subprocess.bpmn"
    })
    void shouldHandleGlobalCancellation() {
        // Arrange
        Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
            .withId("BEN-CANCEL-001")
            .build();

        ProcessInstance orchestrator = testHelper.startProcess(
            "orchestrator-main-process",
            "BEN-CANCEL-001",
            variables
        );

        assertThat(orchestrator).isActive();

        // Act - Send global cancellation message
        testHelper.triggerMessage("Msg_CancelarProcesso", "BEN-CANCEL-001",
            Map.of("cancellationReason", "Contract terminated"));

        // Assert - All processes should be cancelled
        assertThat(orchestrator).isEnded();

        testHelper.assertHasPassed(orchestrator, "BoundaryEvent_GlobalCancellation");
        testHelper.assertVariableEquals(orchestrator, "processCancelled", true);
        testHelper.assertVariableEquals(orchestrator, "compensationExecuted", true);
    }

    @Test
    @DisplayName("Should handle error and retry with compensation")
    @Deployment(resources = {
        "bpmn/orchestrator-main-process.bpmn",
        "bpmn/4-resolution-authorization-subprocess.bpmn",
        "bpmn/10-error-handling-compensation-subprocess.bpmn"
    })
    void shouldHandleErrorAndCompensation() {
        // Arrange
        Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
            .withId("BEN-ERROR-001")
            .build();

        variables.put("simulateAuthorizationError", true);

        // Act
        ProcessInstance orchestrator = testHelper.startProcess(
            "orchestrator-main-process",
            "BEN-ERROR-001",
            variables
        );

        // Assert - Error handling should be triggered
        testHelper.assertHasPassed(orchestrator, "BoundaryEvent_Error");
        testHelper.assertHasPassed(orchestrator, "CallActivity_ErrorHandling");

        // Verify retry logic
        Integer retryCount = (Integer) testHelper.getVariable(orchestrator, "retryCount");
        assertThat(retryCount).isGreaterThan(0);

        // Verify compensation
        testHelper.assertVariableEquals(orchestrator, "compensationExecuted", true);
        testHelper.assertVariableEquals(orchestrator, "errorLogged", true);
    }

    @Test
    @DisplayName("Should measure end-to-end performance metrics")
    @Deployment(resources = "bpmn/orchestrator-main-process.bpmn")
    void shouldMeasurePerformanceMetrics() {
        // Arrange
        long startTime = System.currentTimeMillis();

        Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
            .withId("BEN-PERF-001")
            .build();

        // Act
        ProcessInstance orchestrator = testHelper.startProcess(
            "orchestrator-main-process",
            variables
        );

        // Complete process
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert - Performance expectations
        assertThat(duration).isLessThan(5000); // Should complete in < 5 seconds

        // Verify metrics collected
        Map<String, Object> metrics = (Map<String, Object>) testHelper.getVariable(
            orchestrator, "performanceMetrics");

        assertThat(metrics).isNotNull();
        assertThat(metrics).containsKeys(
            "totalDuration",
            "onboardingDuration",
            "authorizationDuration",
            "totalServiceTasks"
        );
    }
}
