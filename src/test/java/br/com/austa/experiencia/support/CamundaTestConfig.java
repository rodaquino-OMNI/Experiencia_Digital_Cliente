package br.com.austa.experiencia.support;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.test.helper.StandaloneInMemoryTestConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import jakarta.annotation.PostConstruct;

/**
 * Camunda BPM test configuration for process testing.
 *
 * Provides:
 * - In-memory H2 database for process engine
 * - Process engine configuration optimized for testing
 * - Auto-deployment of BPMN/DMN resources
 * - Access to Camunda services (RuntimeService, TaskService, etc.)
 *
 * Usage in tests:
 * <pre>
 * {@code
 * @SpringBootTest
 * @Import(CamundaTestConfig.class)
 * @ExtendWith(SpringExtension.class)
 * class MyProcessTest {
 *     @Autowired
 *     private RuntimeService runtimeService;
 * }
 * }
 * </pre>
 *
 * @see org.camunda.bpm.spring.boot.starter.test.helper.AbstractProcessEngineRuleTest
 */
@TestConfiguration
@EnableProcessApplication
@Import(StandaloneInMemoryTestConfiguration.class)
public class CamundaTestConfig {

    /**
     * Process Engine Rule for test lifecycle management.
     * Provides automatic process deployment and cleanup between tests.
     *
     * @return configured ProcessEngineRule
     */
    @Bean
    public ProcessEngineRule processEngineRule() {
        ProcessEngineRule rule = new ProcessEngineRule();
        rule.getProcessEngineConfiguration()
            .setDatabaseSchemaUpdate("create-drop")
            .setJdbcUrl("jdbc:h2:mem:camunda-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
            .setJdbcDriver("org.h2.Driver")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJobExecutorActivate(false) // Disable async processing in tests
            .setHistory("full"); // Enable full history for test assertions

        return rule;
    }

    /**
     * Repository Service for managing process definitions and deployments.
     *
     * @param processEngine the process engine
     * @return RepositoryService instance
     */
    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    /**
     * Runtime Service for executing process instances.
     *
     * @param processEngine the process engine
     * @return RuntimeService instance
     */
    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    /**
     * Task Service for managing user tasks.
     *
     * @param processEngine the process engine
     * @return TaskService instance
     */
    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    /**
     * History Service for querying historic process data.
     *
     * @param processEngine the process engine
     * @return HistoryService instance
     */
    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    /**
     * Management Service for admin operations and monitoring.
     *
     * @param processEngine the process engine
     * @return ManagementService instance
     */
    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    /**
     * Auto-deploy process resources on initialization.
     * Deploys all BPMN and DMN files from test resources.
     */
    @PostConstruct
    public void deployProcessResources() {
        // Deployment happens automatically via @Deployment annotation on test classes
        // Or programmatically via RepositoryService in individual tests
    }

    /**
     * Test helper: Creates a test deployment builder for BPMN resources.
     *
     * @param repositoryService the repository service
     * @param resourcePaths paths to BPMN/DMN files
     * @return deployment ID
     */
    public static String deployTestResources(
            RepositoryService repositoryService,
            String... resourcePaths) {

        var deployment = repositoryService.createDeployment()
            .name("Test Deployment")
            .enableDuplicateFiltering(false);

        for (String path : resourcePaths) {
            deployment.addClasspathResource(path);
        }

        return deployment.deploy().getId();
    }

    /**
     * Test helper: Cleanup all deployments.
     * Useful in @AfterEach methods.
     *
     * @param repositoryService the repository service
     */
    public static void cleanupAllDeployments(RepositoryService repositoryService) {
        repositoryService.createDeploymentQuery()
            .list()
            .forEach(deployment ->
                repositoryService.deleteDeployment(deployment.getId(), true));
    }

    /**
     * Test helper: Wait for async jobs to complete.
     *
     * @param managementService the management service
     * @param maxWaitMillis maximum wait time in milliseconds
     * @return true if all jobs completed, false if timeout
     */
    public static boolean waitForJobExecutorToProcessAllJobs(
            ManagementService managementService,
            long maxWaitMillis) {

        long endTime = System.currentTimeMillis() + maxWaitMillis;

        while (System.currentTimeMillis() < endTime) {
            long jobCount = managementService.createJobQuery().count();
            if (jobCount == 0) {
                return true;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * Test helper: Get process variables as typed map.
     *
     * @param runtimeService the runtime service
     * @param processInstanceId the process instance ID
     * @return map of variable names to values
     */
    public static java.util.Map<String, Object> getProcessVariables(
            RuntimeService runtimeService,
            String processInstanceId) {

        return runtimeService.getVariables(processInstanceId);
    }

    /**
     * Test helper: Complete user task with variables.
     *
     * @param taskService the task service
     * @param taskId the task ID
     * @param variables variables to set when completing task
     */
    public static void completeTask(
            TaskService taskService,
            String taskId,
            java.util.Map<String, Object> variables) {

        if (variables != null && !variables.isEmpty()) {
            taskService.complete(taskId, variables);
        } else {
            taskService.complete(taskId);
        }
    }

    /**
     * Test helper: Get active task by process instance.
     *
     * @param taskService the task service
     * @param processInstanceId the process instance ID
     * @return task ID or null if no active task
     */
    public static String getActiveTaskId(
            TaskService taskService,
            String processInstanceId) {

        var task = taskService.createTaskQuery()
            .processInstanceId(processInstanceId)
            .singleResult();

        return task != null ? task.getId() : null;
    }

    /**
     * Test helper: Assert process instance is ended.
     *
     * @param historyService the history service
     * @param processInstanceId the process instance ID
     * @return true if process is ended
     */
    public static boolean isProcessInstanceEnded(
            HistoryService historyService,
            String processInstanceId) {

        var historicInstance = historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();

        return historicInstance != null && historicInstance.getEndTime() != null;
    }

    /**
     * Test configuration summary for logging.
     *
     * @return formatted configuration string
     */
    public static String getTestConfigSummary() {
        return """
            Camunda Test Configuration:
              Database: H2 In-Memory (camunda-test)
              Schema: create-drop
              History Level: full
              Job Executor: disabled (synchronous execution)
              Deployment: Auto-deploy from test resources
            """;
    }
}
