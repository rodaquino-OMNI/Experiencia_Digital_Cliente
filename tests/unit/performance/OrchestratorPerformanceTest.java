package com.austa.saude.experiencia.test.unit.performance;

import com.austa.saude.experiencia.test.helpers.TestDataBuilder;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance Tests for Orchestrator Process
 *
 * Tests system behavior under load and stress conditions
 *
 * Coverage:
 * - Concurrent process execution
 * - Memory usage under load
 * - Response time benchmarks
 * - Throughput testing
 */
@SpringBootTest
@DisplayName("Performance Tests")
class OrchestratorPerformanceTest {

    @Autowired
    private ProcessEngine processEngine;

    @Test
    @DisplayName("Should handle 100 concurrent process instances")
    void shouldHandle100ConcurrentInstances() throws InterruptedException, ExecutionException {
        // Arrange
        int instanceCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Long>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Act
        for (int i = 0; i < instanceCount; i++) {
            final int instanceNumber = i;

            Future<Long> future = executor.submit(() -> {
                long start = System.currentTimeMillis();

                Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
                    .withId("BEN-PERF-" + instanceNumber)
                    .build();

                ProcessInstance instance = processEngine.getRuntimeService()
                    .startProcessInstanceByKey("orchestrator-main-process", variables);

                return System.currentTimeMillis() - start;
            });

            futures.add(future);
        }

        // Collect results
        List<Long> durations = new ArrayList<>();
        for (Future<Long> future : futures) {
            durations.add(future.get());
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        long totalTime = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(durations).hasSize(instanceCount);

        // Calculate statistics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(0);
        long minDuration = durations.stream().mapToLong(Long::longValue).min().orElse(0);

        System.out.println("=== Performance Test Results ===");
        System.out.println("Total instances: " + instanceCount);
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Avg duration: " + avgDuration + "ms");
        System.out.println("Min duration: " + minDuration + "ms");
        System.out.println("Max duration: " + maxDuration + "ms");
        System.out.println("Throughput: " + (instanceCount * 1000.0 / totalTime) + " instances/sec");

        // Performance assertions
        assertThat(avgDuration).isLessThan(5000); // Average < 5 seconds
        assertThat(maxDuration).isLessThan(10000); // Max < 10 seconds
    }

    @Test
    @DisplayName("Should maintain performance with message correlation load")
    void shouldMaintainPerformanceWithMessages() throws InterruptedException {
        // Arrange
        int messageCount = 1000;
        List<ProcessInstance> instances = new ArrayList<>();

        // Start instances
        for (int i = 0; i < 10; i++) {
            Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
                .withId("BEN-MSG-" + i)
                .build();

            ProcessInstance instance = processEngine.getRuntimeService()
                .startProcessInstanceByKey("orchestrator-main-process", "BEN-MSG-" + i, variables);

            instances.add(instance);
        }

        long startTime = System.currentTimeMillis();

        // Act - Send many messages
        for (int i = 0; i < messageCount; i++) {
            String businessKey = "BEN-MSG-" + (i % 10); // Distribute across instances

            processEngine.getRuntimeService()
                .createMessageCorrelation("Msg_UpdateStatus")
                .processInstanceBusinessKey(businessKey)
                .setVariable("updateNumber", i)
                .correlate();
        }

        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(duration).isLessThan(60000); // Should complete in < 60 seconds

        double throughput = messageCount * 1000.0 / duration;
        System.out.println("Message throughput: " + throughput + " messages/sec");

        assertThat(throughput).isGreaterThan(10); // > 10 messages/sec
    }

    @Test
    @DisplayName("Should not cause memory leaks with long-running processes")
    void shouldNotCauseMemoryLeaks() {
        // Arrange
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Force garbage collection

        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Act - Create and complete many process instances
        for (int i = 0; i < 100; i++) {
            Map<String, Object> variables = TestDataBuilder.BeneficiaryBuilder.aBeneficiary()
                .withId("BEN-MEM-" + i)
                .build();

            ProcessInstance instance = processEngine.getRuntimeService()
                .startProcessInstanceByKey("orchestrator-main-process", variables);

            // Complete process
            processEngine.getRuntimeService().deleteProcessInstance(
                instance.getId(), "Performance test cleanup");
        }

        runtime.gc(); // Force garbage collection
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();

        // Assert
        long memoryIncrease = finalMemory - initialMemory;
        double memoryIncreaseMB = memoryIncrease / (1024.0 * 1024.0);

        System.out.println("Memory increase: " + memoryIncreaseMB + " MB");

        // Should not increase memory by more than 50MB
        assertThat(memoryIncreaseMB).isLessThan(50);
    }

    @Test
    @DisplayName("Should meet SLA for authorization processing")
    void shouldMeetAuthorizationSLA() {
        // Arrange - SLA: 85% auto-approved in < 5 minutes
        int totalRequests = 100;
        int autoApprovedCount = 0;
        List<Long> durations = new ArrayList<>();

        // Act
        for (int i = 0; i < totalRequests; i++) {
            long start = System.currentTimeMillis();

            Map<String, Object> authRequest = TestDataBuilder.AuthorizationBuilder.anAuthorization()
                .withBeneficiaryId("BEN-SLA-" + i)
                .withProcedureCode("40301010") // Routine consultation
                .build();

            ProcessInstance instance = processEngine.getRuntimeService()
                .startProcessInstanceByKey("authorization-subprocess", authRequest);

            long duration = System.currentTimeMillis() - start;
            durations.add(duration);

            Boolean autoApproved = (Boolean) processEngine.getRuntimeService()
                .getVariable(instance.getId(), "autoApproved");

            if (Boolean.TRUE.equals(autoApproved)) {
                autoApprovedCount++;
            }
        }

        // Assert
        double autoApprovalRate = (autoApprovedCount * 100.0) / totalRequests;
        System.out.println("Auto-approval rate: " + autoApprovalRate + "%");

        assertThat(autoApprovalRate).isGreaterThanOrEqualTo(85); // >= 85% auto-approved

        long p95Duration = durations.stream().sorted().skip((long) (totalRequests * 0.95)).findFirst().orElse(0L);
        System.out.println("P95 duration: " + p95Duration + "ms");

        assertThat(p95Duration).isLessThan(300000); // < 5 minutes
    }
}
