package com.austa.saude.experiencia.test.helpers;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Camunda Test Helper
 *
 * Provides utility methods for Camunda process testing
 * Simplifies common testing operations and assertions
 */
public class CamundaTestHelper {

    private final ProcessEngine processEngine;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public CamundaTestHelper(ProcessEngine processEngine) {
        this.processEngine = processEngine;
        this.runtimeService = processEngine.getRuntimeService();
        this.taskService = processEngine.getTaskService();
    }

    /**
     * Start process instance with variables
     */
    public ProcessInstance startProcess(String processKey, Map<String, Object> variables) {
        return runtimeService.startProcessInstanceByKey(processKey, variables);
    }

    /**
     * Start process instance with business key and variables
     */
    public ProcessInstance startProcess(String processKey, String businessKey, Map<String, Object> variables) {
        return runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);
    }

    /**
     * Complete user task by task definition key
     */
    public void completeTask(ProcessInstance processInstance, String taskDefinitionKey) {
        completeTask(processInstance, taskDefinitionKey, new HashMap<>());
    }

    /**
     * Complete user task with variables
     */
    public void completeTask(ProcessInstance processInstance, String taskDefinitionKey, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .taskDefinitionKey(taskDefinitionKey)
            .singleResult();

        assertThat(task).isNotNull();
        taskService.complete(task.getId(), variables);
    }

    /**
     * Trigger message event
     */
    public void triggerMessage(String messageName, String businessKey) {
        runtimeService.createMessageCorrelation(messageName)
            .processInstanceBusinessKey(businessKey)
            .correlate();
    }

    /**
     * Trigger message event with variables
     */
    public void triggerMessage(String messageName, String businessKey, Map<String, Object> variables) {
        runtimeService.createMessageCorrelation(messageName)
            .processInstanceBusinessKey(businessKey)
            .setVariables(variables)
            .correlate();
    }

    /**
     * Trigger signal event
     */
    public void triggerSignal(String signalName) {
        runtimeService.signalEventReceived(signalName);
    }

    /**
     * Trigger signal event with variables
     */
    public void triggerSignal(String signalName, Map<String, Object> variables) {
        runtimeService.signalEventReceived(signalName, variables);
    }

    /**
     * Get process variable
     */
    public Object getVariable(ProcessInstance processInstance, String variableName) {
        return runtimeService.getVariable(processInstance.getId(), variableName);
    }

    /**
     * Get all process variables
     */
    public Map<String, Object> getVariables(ProcessInstance processInstance) {
        return runtimeService.getVariables(processInstance.getId());
    }

    /**
     * Set process variable
     */
    public void setVariable(ProcessInstance processInstance, String variableName, Object value) {
        runtimeService.setVariable(processInstance.getId(), variableName, value);
    }

    /**
     * Assert process is waiting at activity
     */
    public void assertWaitingAt(ProcessInstance processInstance, String activityId) {
        assertThat(runtimeService.createExecutionQuery()
            .processInstanceId(processInstance.getId())
            .activityId(activityId)
            .count()).isGreaterThan(0);
    }

    /**
     * Assert process has passed activity
     */
    public void assertHasPassed(ProcessInstance processInstance, String activityId) {
        assertThat(processEngine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .processInstanceId(processInstance.getId())
            .activityId(activityId)
            .finished()
            .count()).isGreaterThan(0);
    }

    /**
     * Assert process is ended
     */
    public void assertProcessEnded(ProcessInstance processInstance) {
        assertThat(runtimeService.createProcessInstanceQuery()
            .processInstanceId(processInstance.getId())
            .count()).isEqualTo(0);
    }

    /**
     * Assert process is active
     */
    public void assertProcessActive(ProcessInstance processInstance) {
        assertThat(runtimeService.createProcessInstanceQuery()
            .processInstanceId(processInstance.getId())
            .active()
            .count()).isEqualTo(1);
    }

    /**
     * Assert process is suspended
     */
    public void assertProcessSuspended(ProcessInstance processInstance) {
        assertThat(runtimeService.createProcessInstanceQuery()
            .processInstanceId(processInstance.getId())
            .suspended()
            .count()).isEqualTo(1);
    }

    /**
     * Assert variable exists and has value
     */
    public void assertVariableEquals(ProcessInstance processInstance, String variableName, Object expectedValue) {
        Object actualValue = getVariable(processInstance, variableName);
        assertThat(actualValue).isEqualTo(expectedValue);
    }

    /**
     * Get active tasks for process instance
     */
    public List<Task> getActiveTasks(ProcessInstance processInstance) {
        return taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .active()
            .list();
    }

    /**
     * Get task by definition key
     */
    public Task getTask(ProcessInstance processInstance, String taskDefinitionKey) {
        return taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .taskDefinitionKey(taskDefinitionKey)
            .singleResult();
    }

    /**
     * Assert task exists
     */
    public void assertTaskExists(ProcessInstance processInstance, String taskDefinitionKey) {
        Task task = getTask(processInstance, taskDefinitionKey);
        assertThat(task).isNotNull();
    }

    /**
     * Assert task does not exist
     */
    public void assertTaskDoesNotExist(ProcessInstance processInstance, String taskDefinitionKey) {
        Task task = getTask(processInstance, taskDefinitionKey);
        assertThat(task).isNull();
    }

    /**
     * Create test variables map
     */
    public static Map<String, Object> variables() {
        return new HashMap<>();
    }

    /**
     * Add variable to map (fluent)
     */
    public static Map<String, Object> with(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * Add variable to existing map (fluent)
     */
    public static Map<String, Object> and(Map<String, Object> map, String key, Object value) {
        map.put(key, value);
        return map;
    }
}
