package br.com.austa.experiencia.support;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests providing common test infrastructure.
 *
 * Features:
 * - TestContainers (PostgreSQL, Kafka, Redis)
 * - WireMock servers for external APIs
 * - Camunda BPM process engine
 * - Spring Boot test context
 * - Test profile activation
 * - Automatic cleanup between tests
 *
 * Usage:
 * <pre>
 * {@code
 * class MyIntegrationTest extends BaseIntegrationTest {
 *     @Test
 *     void myTest() {
 *         // Test implementation using inherited services
 *     }
 * }
 * }
 * </pre>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import({
    TestContainersConfig.class,
    MockServersConfig.class,
    CamundaTestConfig.class
})
public abstract class BaseIntegrationTest {

    // Camunda Services
    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected ManagementService managementService;

    /**
     * Setup before each test.
     * Override in subclasses for additional setup.
     */
    @BeforeEach
    public void setUp() {
        // Reset mock servers to default state
        MockServersConfig.resetAllMockServers();

        // Log test environment status
        logTestEnvironmentStatus();
    }

    /**
     * Cleanup after each test.
     * Override in subclasses for additional cleanup.
     */
    @AfterEach
    public void tearDown() {
        // Cleanup process instances
        cleanupProcessInstances();

        // Reset mock servers
        MockServersConfig.resetAllMockServers();
    }

    /**
     * Cleanup all running and historic process instances.
     */
    protected void cleanupProcessInstances() {
        // Delete all running process instances
        runtimeService.createProcessInstanceQuery()
            .list()
            .forEach(instance ->
                runtimeService.deleteProcessInstance(
                    instance.getId(),
                    "Test cleanup",
                    true,
                    true
                ));

        // Delete all historic process instances
        historyService.createHistoricProcessInstanceQuery()
            .list()
            .forEach(instance ->
                historyService.deleteHistoricProcessInstance(instance.getId()));
    }

    /**
     * Wait for all async jobs to complete.
     *
     * @param maxWaitMillis maximum wait time in milliseconds
     * @return true if all jobs completed, false if timeout
     */
    protected boolean waitForJobsToComplete(long maxWaitMillis) {
        return CamundaTestConfig.waitForJobExecutorToProcessAllJobs(
            managementService,
            maxWaitMillis
        );
    }

    /**
     * Complete user task by process instance ID.
     *
     * @param processInstanceId the process instance ID
     * @param variables variables to set when completing task
     */
    protected void completeUserTask(
            String processInstanceId,
            java.util.Map<String, Object> variables) {

        String taskId = CamundaTestConfig.getActiveTaskId(taskService, processInstanceId);
        if (taskId != null) {
            CamundaTestConfig.completeTask(taskService, taskId, variables);
        }
    }

    /**
     * Check if process instance has ended.
     *
     * @param processInstanceId the process instance ID
     * @return true if process ended
     */
    protected boolean isProcessEnded(String processInstanceId) {
        return CamundaTestConfig.isProcessInstanceEnded(historyService, processInstanceId);
    }

    /**
     * Get process variables as map.
     *
     * @param processInstanceId the process instance ID
     * @return map of variable names to values
     */
    protected java.util.Map<String, Object> getProcessVariables(String processInstanceId) {
        return CamundaTestConfig.getProcessVariables(runtimeService, processInstanceId);
    }

    /**
     * Log test environment status for debugging.
     */
    private void logTestEnvironmentStatus() {
        System.out.println("=".repeat(80));
        System.out.println("TEST ENVIRONMENT STATUS");
        System.out.println("=".repeat(80));
        System.out.println(TestContainersConfig.getContainerHealthStatus());
        System.out.println();
        System.out.println(MockServersConfig.getMockServersStatus());
        System.out.println();
        System.out.println(CamundaTestConfig.getTestConfigSummary());
        System.out.println("=".repeat(80));
    }

    /**
     * Create common test data for onboarding scenarios.
     *
     * @return map with beneficiary test data
     */
    protected java.util.Map<String, Object> createOnboardingTestData() {
        return java.util.Map.of(
            "beneficiarioId", "BEN-TEST-001",
            "cpf", "12345678901",
            "nome", "João Silva",
            "idade", 30,
            "email", "joao.silva@test.com",
            "telefone", "11999999999",
            "plano", "PREMIUM"
        );
    }

    /**
     * Create common test data for authorization scenarios.
     *
     * @return map with authorization test data
     */
    protected java.util.Map<String, Object> createAuthorizationTestData() {
        return java.util.Map.of(
            "pacienteId", "PAT-TEST-001",
            "procedimentoId", "PROC-001",
            "especialidade", "CARDIOLOGIA",
            "urgencia", "NORMAL",
            "justificativa", "Consulta de rotina"
        );
    }

    /**
     * Create common test data for self-service scenarios.
     *
     * @return map with self-service test data
     */
    protected java.util.Map<String, Object> createSelfServiceTestData() {
        return java.util.Map.of(
            "beneficiarioId", "BEN-TEST-001",
            "servicoId", "SERVICO-001",
            "canal", "WHATSAPP",
            "mensagem", "Preciso agendar uma consulta"
        );
    }

    /**
     * Assert that process reached specific activity.
     *
     * @param processInstanceId process instance ID
     * @param activityId activity ID to check
     * @return true if activity was reached
     */
    protected boolean hasReachedActivity(String processInstanceId, String activityId) {
        return historyService.createHistoricActivityInstanceQuery()
            .processInstanceId(processInstanceId)
            .activityId(activityId)
            .count() > 0;
    }

    /**
     * Get variable value from process history.
     *
     * @param processInstanceId process instance ID
     * @param variableName variable name
     * @return variable value or null
     */
    protected Object getHistoricVariableValue(String processInstanceId, String variableName) {
        var variable = historyService.createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .variableName(variableName)
            .singleResult();

        return variable != null ? variable.getValue() : null;
    }
}
