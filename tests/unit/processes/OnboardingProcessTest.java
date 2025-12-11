package com.austa.saude.experiencia.test.unit.processes;

import com.austa.saude.experiencia.test.helpers.CamundaTestHelper;
import com.austa.saude.experiencia.test.helpers.TestDataBuilder;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.junit5.ProcessEngineExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Process Tests for Onboarding Subprocess
 *
 * Tests the complete BPMN process flow for beneficiary onboarding
 * Validates process logic, gateways, events, and state transitions
 *
 * BPMN: 1-Onboarding-Screening-Subprocess.bpmn
 */
@ExtendWith(ProcessEngineExtension.class)
@DisplayName("Onboarding Process Tests")
class OnboardingProcessTest {

    private ProcessEngine processEngine;
    private CamundaTestHelper testHelper;
    private Map<String, Object> testVariables;

    @BeforeEach
    void setUp(ProcessEngine processEngine) {
        this.processEngine = processEngine;
        this.testHelper = new CamundaTestHelper(processEngine);

        testVariables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
            .withId("BEN-TEST-001")
            .withName("João Silva")
            .withRiskLevel("MODERATE")
            .build();
    }

    @Test
    @DisplayName("Should complete onboarding successfully for low-risk beneficiary")
    @Deployment(resources = "bpmn/1-onboarding-screening-subprocess.bpmn")
    void shouldCompleteOnboardingLowRisk() {
        // Arrange
        testVariables.put("riskScore", 25);
        testVariables.put("cptDetected", false);

        // Act
        ProcessInstance instance = testHelper.startProcess(
            "onboarding-subprocess",
            "BEN-TEST-001",
            testVariables
        );

        // Assert - Process should complete
        assertThat(instance).isEnded();

        // Verify process passed through expected activities
        testHelper.assertHasPassed(instance, "ServiceTask_WelcomeMessage");
        testHelper.assertHasPassed(instance, "ServiceTask_HealthScreening");
        testHelper.assertHasPassed(instance, "ServiceTask_RiskStratification");
        testHelper.assertHasPassed(instance, "EndEvent_OnboardingComplete");

        // Verify output variables
        testHelper.assertVariableEquals(instance, "onboardingComplete", true);
        testHelper.assertVariableEquals(instance, "riskLevel", "LOW");
        testHelper.assertVariableEquals(instance, "carePlanRequired", false);
    }

    @Test
    @DisplayName("Should trigger CPT detection and review for high-risk beneficiary")
    @Deployment(resources = "bpmn/1-onboarding-screening-subprocess.bpmn")
    void shouldTriggerCPTDetection() {
        // Arrange
        testVariables.put("riskScore", 85);
        Map<String, Object> clinicalData = new HashMap<>();
        clinicalData.put("chronicConditions", new String[]{"diabetes", "hypertension"});
        testVariables.put("clinicalData", clinicalData);

        // Act
        ProcessInstance instance = testHelper.startProcess(
            "onboarding-subprocess",
            testVariables
        );

        // Assert - Should wait at CPT review task
        assertThat(instance).isActive();
        testHelper.assertWaitingAt(instance, "UserTask_CPTReview");

        // Verify CPT detection was triggered
        testHelper.assertHasPassed(instance, "ServiceTask_CPTDetection");

        // Complete manual review
        testHelper.completeTask(instance, "UserTask_CPTReview",
            Map.of("cptConfirmed", true, "carencyPeriod", 24));

        // Assert - Process should complete after review
        assertThat(instance).isEnded();
        testHelper.assertVariableEquals(instance, "cptDetected", true);
    }

    @Test
    @DisplayName("Should create care plan for moderate-risk beneficiary")
    @Deployment(resources = "bpmn/1-onboarding-screening-subprocess.bpmn")
    void shouldCreateCarePlan() {
        // Arrange
        testVariables.put("riskScore", 60);
        testVariables.put("riskLevel", "MODERATE");

        // Act
        ProcessInstance instance = testHelper.startProcess(
            "onboarding-subprocess",
            testVariables
        );

        // Assert
        assertThat(instance).isEnded();

        // Verify care plan creation
        testHelper.assertHasPassed(instance, "ServiceTask_CreateCarePlan");
        testHelper.assertVariableEquals(instance, "carePlanCreated", true);

        Object carePlanId = testHelper.getVariable(instance, "carePlanId");
        assertThat(carePlanId).isNotNull();
    }

    @Test
    @DisplayName("Should handle OCR document processing")
    @Deployment(resources = "bpmn/1-onboarding-screening-subprocess.bpmn")
    void shouldProcessOCRDocuments() {
        // Arrange
        testVariables.put("documentsProvided", true);
        testVariables.put("documentData", "base64EncodedDocument...");

        // Act
        ProcessInstance instance = testHelper.startProcess(
            "onboarding-subprocess",
            testVariables
        );

        // Assert
        testHelper.assertHasPassed(instance, "ServiceTask_OCRProcessing");

        Object ocrComplete = testHelper.getVariable(instance, "ocrComplete");
        assertThat(ocrComplete).isEqualTo(true);
    }

    @Test
    @DisplayName("Should handle message cancellation event")
    @Deployment(resources = "bpmn/1-onboarding-screening-subprocess.bpmn")
    void shouldHandleCancellation() {
        // Arrange
        testVariables.put("riskScore", 60);

        // Act
        ProcessInstance instance = testHelper.startProcess(
            "onboarding-subprocess",
            "BEN-CANCEL-001",
            testVariables
        );

        // Send cancellation message
        testHelper.triggerMessage("Msg_CancelOnboarding", "BEN-CANCEL-001");

        // Assert - Process should be ended
        assertThat(instance).isEnded();

        // Verify cancellation was handled
        testHelper.assertHasPassed(instance, "BoundaryEvent_Cancel");
        testHelper.assertVariableEquals(instance, "onboardingCancelled", true);
    }

    @Test
    @DisplayName("Should handle error and compensation")
    @Deployment(resources = "bpmn/1-onboarding-screening-subprocess.bpmn")
    void shouldHandleErrorAndCompensation() {
        // Arrange
        testVariables.put("simulateError", true);
        testVariables.put("errorType", "EXTERNAL_SERVICE_FAILURE");

        // Act
        ProcessInstance instance = testHelper.startProcess(
            "onboarding-subprocess",
            testVariables
        );

        // Assert - Should trigger error handling
        testHelper.assertHasPassed(instance, "BoundaryEvent_Error");
        testHelper.assertHasPassed(instance, "ServiceTask_LogError");

        // Verify compensation was triggered
        testHelper.assertVariableEquals(instance, "errorHandled", true);
        testHelper.assertVariableEquals(instance, "compensationExecuted", true);
    }

    @Test
    @DisplayName("Should route to manual review for incomplete data")
    @Deployment(resources = "bpmn/1-onboarding-screening-subprocess.bpmn")
    void shouldRouteToManualReview() {
        // Arrange
        testVariables.put("dataComplete", false);
        testVariables.put("missingFields", new String[]{"healthHistory", "documents"});

        // Act
        ProcessInstance instance = testHelper.startProcess(
            "onboarding-subprocess",
            testVariables
        );

        // Assert - Should wait at manual review task
        assertThat(instance).isActive();
        testHelper.assertWaitingAt(instance, "UserTask_ManualReview");
        testHelper.assertTaskExists(instance, "UserTask_ManualReview");

        // Complete manual review
        testHelper.completeTask(instance, "UserTask_ManualReview",
            Map.of("dataCompleted", true, "reviewerNotes", "Data updated"));

        assertThat(instance).isEnded();
    }
}
