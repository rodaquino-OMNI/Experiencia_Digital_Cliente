# ADR-003: External Integration Pattern Selection

## Status
**Accepted** - 2025-12-11

## Context
The Camunda 7 BPM platform must integrate with multiple external systems: Tasy ERP, WhatsApp Business API, IBM RPA, and ML/AI engines. We need to select consistent integration patterns that balance reliability, performance, and maintainability.

## Decision Drivers
- **Reliability**: Handle transient failures gracefully
- **Performance**: Minimize latency for synchronous operations
- **Maintainability**: Consistent patterns across integrations
- **Monitoring**: Observable integration health
- **Scalability**: Support high-throughput scenarios

## Considered Patterns

### Pattern 1: Synchronous REST Calls from Service Tasks
**Implementation**: Direct HTTP calls from JavaDelegate implementations

**Pros**:
- Simple implementation
- Immediate feedback on success/failure
- Easy to debug

**Cons**:
- Blocks process execution thread
- Tight coupling to external system availability
- Limited retry/timeout control
- Difficult to handle long-running operations

### Pattern 2: External Task Pattern (Async Workers)
**Implementation**: External task workers poll Camunda for work items

**Pros**:
- Decouples process engine from external systems
- Natural retry/timeout handling
- Horizontal scaling of workers
- Survives process engine restarts

**Cons**:
- More complex implementation
- Polling overhead
- Higher infrastructure requirements

### Pattern 3: Hybrid Approach
**Implementation**: Synchronous for fast operations, async for slow/unreliable

**Pros**:
- Best of both worlds
- Optimized for each use case
- Flexible based on SLA requirements

**Cons**:
- Two patterns to maintain
- Team must understand when to use each

## Decision
**Selected: Hybrid Approach with Clear Guidelines**

### Pattern Selection Matrix

| External System | Pattern | Rationale |
|----------------|---------|-----------|
| **Tasy ERP (Read)** | Synchronous REST | Fast (<500ms), reliable, needed for process flow |
| **Tasy ERP (Write)** | Async External Task | May be slow, transaction safety required |
| **WhatsApp API** | Async via Kafka | Rate limits, delivery not immediate |
| **IBM RPA** | Async External Task | Long-running bots (minutes) |
| **ML/AI (Risk Calc)** | Synchronous REST | Fast inference (<1s), caching available |
| **ML/AI (Batch)** | Async via Kafka | Large datasets, not time-sensitive |

## Implementation Guidelines

### Synchronous REST Pattern

**When to Use**:
- Operation completes in <2 seconds
- Result needed immediately for process decision
- External system has 99.9%+ SLA
- No rate limiting concerns

**Implementation**:
```java
@Component("tasyBeneficiaryFetcher")
public class TasyBeneficiaryFetcherDelegate implements JavaDelegate {

    @Autowired
    private TasyIntegrationService tasyService;

    @Autowired
    private CircuitBreaker tasyCircuitBreaker;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String externalId = (String) execution.getVariable("externalId");

        // Circuit breaker + retry logic
        BeneficiaryDTO beneficiary = tasyCircuitBreaker.executeSupplier(
            () -> tasyService.getBeneficiary(externalId)
        );

        execution.setVariable("beneficiaryData", beneficiary);
    }
}
```

**Required Patterns**:
- Circuit Breaker (Resilience4j)
- Retry with exponential backoff
- Timeouts (connection + read)
- Error handling with BPMN Error events

### External Task Pattern

**When to Use**:
- Operation may take >5 seconds
- External system has variable latency
- Need horizontal scaling of workers
- Complex error recovery required

**Implementation**:
```java
@Service
public class RpaSchedulingWorker {

    @Autowired
    private ExternalTaskService externalTaskService;

    @PostConstruct
    public void subscribe() {
        externalTaskService.subscribe("rpa-schedule-appointment")
            .lockDuration(60000) // 1 minute
            .handler((externalTask, externalTaskService) -> {
                try {
                    // Execute RPA bot
                    RpaExecutionResponse response = executeRpaBot(externalTask);

                    // Complete task
                    externalTaskService.complete(externalTask);

                } catch (BpmnError error) {
                    // Business error
                    externalTaskService.handleBpmnError(
                        externalTask,
                        error.getErrorCode(),
                        error.getMessage()
                    );
                } catch (Exception ex) {
                    // Technical error - retry
                    externalTaskService.handleFailure(
                        externalTask,
                        ex.getMessage(),
                        null,
                        3, // retries
                        10000 // retry timeout
                    );
                }
            })
            .open();
    }
}
```

### Async via Kafka Pattern

**When to Use**:
- Fire-and-forget operations
- External system has strict rate limits
- Event-driven architecture preferred
- Temporal decoupling desired

**Implementation**:
```java
@Component("whatsappSender")
public class WhatsAppSenderDelegate implements JavaDelegate {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        NotificationEvent event = buildNotificationEvent(execution);

        // Publish to Kafka (async)
        kafkaTemplate.send("operadora.notification.outbound",
            event.getRecipientPhone(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish notification", ex);
                }
            });

        // Continue process without waiting for delivery
    }
}
```

## Resilience Patterns

### Circuit Breaker Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      tasy:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 5

      whatsapp:
        sliding-window-size: 20
        failure-rate-threshold: 60
        wait-duration-in-open-state: 60s
```

### Retry Configuration
```yaml
resilience4j:
  retry:
    instances:
      tasy:
        max-attempts: 3
        wait-duration: 2s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2

      ml-engine:
        max-attempts: 2
        wait-duration: 1s
```

## Monitoring & Observability

### Metrics to Track
1. **Integration Latency**: P50, P95, P99 response times
2. **Error Rates**: HTTP errors, timeouts, circuit breaker trips
3. **Throughput**: Requests per second per integration
4. **Circuit Breaker State**: Open/closed/half-open transitions
5. **Kafka Lag**: Consumer lag per topic

### Prometheus Metrics
```java
@Component
public class IntegrationMetrics {

    @Autowired
    private MeterRegistry meterRegistry;

    public void recordIntegrationCall(
            String system,
            String operation,
            boolean success,
            long durationMs) {

        meterRegistry.timer("integration.call.duration",
            "system", system,
            "operation", operation,
            "status", success ? "success" : "failure"
        ).record(Duration.ofMillis(durationMs));

        meterRegistry.counter("integration.call.count",
            "system", system,
            "operation", operation,
            "status", success ? "success" : "failure"
        ).increment();
    }
}
```

## Consequences

### Positive
- **Reliability**: Circuit breakers prevent cascading failures
- **Performance**: Async patterns don't block process threads
- **Scalability**: External task workers scale horizontally
- **Maintainability**: Consistent patterns across integrations
- **Observability**: Rich metrics for monitoring

### Negative
- **Complexity**: Multiple patterns to understand and maintain
- **Infrastructure**: More components to deploy and monitor
- **Debugging**: Async operations harder to trace
- **Learning Curve**: Team must understand all three patterns

## Trade-offs
- **Simplicity vs Resilience**: More complex code for better reliability
- **Latency vs Scalability**: Async adds latency but improves throughput
- **Coupling vs Performance**: Sync calls are faster but tightly coupled

## Related Decisions
- ADR-004: Camunda 7 Deployment Model
- ADR-005: Kafka Topic Design
- ADR-007: Error Handling Strategy

## References
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Camunda External Tasks](https://docs.camunda.org/manual/7.20/user-guide/process-engine/external-tasks/)
- [Release It! by Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)
