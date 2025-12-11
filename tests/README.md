# Test Suite - Customer Digital Experience Platform

## Overview

Comprehensive test suite for AUSTA Saúde's Customer Digital Experience platform. This suite provides 90%+ code coverage across all BPMN processes, Java delegates, DMN decision tables, and integration points.

## Test Structure

```
tests/
├── unit/                          # Unit tests (fast, isolated)
│   ├── delegates/                # Java delegate tests
│   ├── processes/                # BPMN process tests
│   └── dmn/                      # DMN decision table tests
├── integration/                   # Integration tests (with TestContainers)
│   ├── message-correlation/      # Message correlation tests
│   └── external-tasks/           # External task worker tests
├── e2e/                          # End-to-end tests
├── fixtures/                     # Test data and fixtures
├── helpers/                      # Test utilities and helpers
└── pom.xml                       # Maven configuration

```

## Test Categories

### 1. Unit Tests
- **Delegates**: Test each Java delegate in isolation with mocked dependencies
- **Processes**: Test BPMN process logic without external dependencies
- **DMN**: Test decision table logic with various input combinations

### 2. Integration Tests
- **Message Correlation**: Test Kafka message handling and process correlation
- **External Tasks**: Test external task workers and completion
- **Database**: Test data persistence and retrieval
- **API**: Test REST API endpoints

### 3. End-to-End Tests
- **Orchestrator**: Test complete process flows from start to end
- **Multi-Process**: Test interactions between multiple processes
- **Error Handling**: Test exception scenarios and compensation
- **Performance**: Test system performance under load

## Running Tests

### Run All Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Run All Tests (Unit + Integration + E2E)
```bash
mvn verify -Pall-tests
```

### Run Performance Tests
```bash
mvn test -Pperformance
```

### Run Specific Test Class
```bash
mvn test -Dtest=OnboardingDelegateTest
```

### Run with Coverage Report
```bash
mvn clean verify
# Report available at: target/site/jacoco/index.html
```

## Test Coverage Requirements

- **Line Coverage**: Minimum 80%
- **Branch Coverage**: Minimum 75%
- **Method Coverage**: Minimum 80%

Coverage is enforced by JaCoCo Maven plugin during the build.

## Writing Tests

### Unit Test Example (Delegate)
```java
@ExtendWith(MockitoExtension.class)
class OnboardingDelegateTest {

    @Mock
    private DelegateExecution execution;

    @Mock
    private HealthScreeningService screeningService;

    @InjectMocks
    private OnboardingDelegate delegate;

    @Test
    void shouldProcessScreeningSuccessfully() {
        // Arrange
        when(execution.getVariable("beneficiaryId")).thenReturn("123");
        when(screeningService.analyzeRisk("123")).thenReturn(new RiskScore(75));

        // Act
        delegate.execute(execution);

        // Assert
        verify(execution).setVariable("riskScore", 75);
        verify(execution).setVariable("riskLevel", "HIGH");
    }
}
```

### Process Test Example (BPMN)
```java
@CamundaSpringBootTest
class OnboardingProcessTest {

    @Autowired
    private ProcessEngine processEngine;

    @Test
    @Deployment(resources = "bpmn/onboarding-subprocess.bpmn")
    void shouldCompleteOnboardingSuccessfully() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("beneficiaryId", "123");
        variables.put("healthData", createHealthData());

        // Act
        ProcessInstance instance = processEngine.getRuntimeService()
            .startProcessInstanceByKey("onboarding-subprocess", variables);

        // Assert
        assertThat(instance).isEnded();
        assertThat(instance).variables()
            .containsEntry("onboardingComplete", true)
            .containsEntry("riskLevel", "MODERATE");
    }
}
```

### Integration Test Example
```java
@SpringBootTest
@Testcontainers
class MessageCorrelationIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private RuntimeService runtimeService;

    @Test
    void shouldCorrelateCancellationMessage() {
        // Arrange
        ProcessInstance instance = runtimeService
            .startProcessInstanceByKey("orchestrator-process");

        // Act
        runtimeService.createMessageCorrelation("Msg_CancelarProcesso")
            .processInstanceBusinessKey(instance.getBusinessKey())
            .correlate();

        // Assert
        assertThat(instance).isEnded();
        assertThat(instance).hasPassed("CancellationEvent");
    }
}
```

## Test Data Builders

Use test data builders for consistent and maintainable test data:

```java
BeneficiaryBuilder.aBeneficiary()
    .withId("123")
    .withName("João Silva")
    .withRiskLevel(RiskLevel.HIGH)
    .withChronicConditions("diabetes", "hypertension")
    .build();
```

## Mocking Guidelines

1. **Mock External Dependencies**: Always mock external services (databases, APIs, message queues)
2. **Use Real Camunda Engine**: For process tests, use real Camunda engine with H2 in-memory database
3. **TestContainers for Integration**: Use TestContainers for integration tests requiring Kafka, PostgreSQL, etc.

## Best Practices

1. **Test Naming**: Use descriptive names following pattern `shouldExpectedBehaviorWhenStateUnderTest`
2. **Arrange-Act-Assert**: Structure tests with clear AAA pattern
3. **One Assertion Per Test**: Each test should verify one specific behavior
4. **Fast Tests**: Unit tests should complete in <100ms, integration tests in <5s
5. **Isolated Tests**: Tests should not depend on each other
6. **Cleanup**: Always clean up resources in @AfterEach

## Test Documentation

Each test file should include:
```java
/**
 * Tests for {@link OnboardingDelegate}
 *
 * Coverage:
 * - Happy path: new beneficiary onboarding
 * - Error handling: invalid health data
 * - Edge cases: missing required fields
 * - Boundary conditions: extreme risk scores
 */
```

## Continuous Integration

Tests are automatically run in CI/CD pipeline:
- **Pull Request**: All unit and integration tests
- **Merge to Main**: All tests + coverage check
- **Nightly**: All tests + performance tests + security scans

## Troubleshooting

### Tests Failing Locally
```bash
# Clean and rebuild
mvn clean install

# Run with debug logging
mvn test -X

# Skip tests temporarily (not recommended)
mvn install -DskipTests
```

### Coverage Below Threshold
```bash
# Generate coverage report
mvn jacoco:report

# View report
open target/site/jacoco/index.html
```

## Resources

- [Camunda BPM Assert Documentation](https://github.com/camunda/camunda-bpm-assert)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://site.mockito.org/)
- [TestContainers Documentation](https://www.testcontainers.org/)

## Contact

For questions or issues with tests, contact:
- **Test Lead**: TESTER Agent (Hive Mind Swarm)
- **Architecture**: ARCHITECT Agent
- **Code Review**: REVIEWER Agent
