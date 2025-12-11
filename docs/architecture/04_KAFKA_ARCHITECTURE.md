# Kafka Event Architecture - Messaging Topology

## Document Control
- **Version**: 1.0
- **Author**: System Architecture Designer
- **Date**: 2025-12-11
- **Status**: Draft

## Overview
This document defines the complete Apache Kafka event-driven architecture for the Camunda 7 BPMN platform, including topic design, message formats, producer/consumer patterns, and integration points.

## Kafka Cluster Configuration

### Cluster Topology
- **Environment**: Production
- **Brokers**: 3 (minimum for high availability)
- **Replication Factor**: 3
- **Min In-Sync Replicas**: 2
- **Partitions**: Topic-specific (detailed below)

### Connection Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
    producer:
      acks: all
      retries: 3
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      compression-type: snappy
    consumer:
      group-id: operadora-digital
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.austa.operadora.*
```

## Topic Architecture

### Topic Naming Convention
```
<domain>.<entity>.<event-type>
```

Examples:
- `operadora.beneficiary.created`
- `operadora.authorization.approved`
- `operadora.interaction.received`

## Topic Catalog

### 1. Beneficiary Lifecycle Topics

#### 1.1 `operadora.beneficiary.created`
**Purpose**: New beneficiary added to system (from Tasy ERP integration)

**Partitions**: 6 (partition by beneficiary_id hash)

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T10:30:00Z",
  "event_type": "BeneficiaryCreated",
  "correlation_id": "uuid",
  "payload": {
    "beneficiary_id": "uuid",
    "external_id": "TASY-12345",
    "cpf": "12345678901",
    "full_name": "João Silva",
    "email": "joao@example.com",
    "phone": "+5511999999999",
    "contract_number": "CNT-2024-001",
    "plan_code": "PLANO-BRONZE",
    "admission_date": "2024-12-01"
  },
  "metadata": {
    "source_system": "TASY_ERP",
    "producer_id": "tasy-connector-01",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Camunda Process Starter**: Triggers `PROC-ORC-001` (main orchestrator)
- **Notification Service**: Sends welcome message via WhatsApp
- **Analytics Pipeline**: Records beneficiary in data warehouse

#### 1.2 `operadora.beneficiary.journey-state-changed`
**Purpose**: Beneficiary transitions to new journey state

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T10:30:00Z",
  "event_type": "BeneficiaryJourneyStateChanged",
  "correlation_id": "uuid",
  "payload": {
    "beneficiary_id": "uuid",
    "previous_state": "ONBOARDING",
    "new_state": "ACTIVE",
    "transition_reason": "ONBOARDING_COMPLETED",
    "process_instance_id": "abc123"
  },
  "metadata": {
    "source_system": "CAMUNDA_BPM",
    "producer_id": "orchestrator-process",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Proactive Motor**: Triggers proactive monitoring rules
- **Care Plan Service**: Adjusts care plans based on state
- **Notification Service**: Sends state-appropriate communications

### 2. Interaction & Communication Topics

#### 2.1 `operadora.interaction.received`
**Purpose**: New interaction from beneficiary (any channel)

**Partitions**: 12 (high-volume topic)

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T10:30:00Z",
  "event_type": "InteractionReceived",
  "correlation_id": "uuid",
  "payload": {
    "interaction_id": "uuid",
    "beneficiary_id": "uuid",
    "channel": "WHATSAPP",
    "interaction_type": "INBOUND_MESSAGE",
    "subject": "Preciso agendar consulta",
    "content": "Olá, gostaria de marcar uma consulta com cardiologista",
    "urgency_level": "MEDIUM",
    "received_at": "2024-12-11T10:30:00Z",
    "sender_phone": "+5511999999999"
  },
  "metadata": {
    "source_system": "WHATSAPP_BUSINESS_API",
    "producer_id": "whatsapp-connector-01",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Reception & Classification Service**: Classifies interaction (SUB-003)
- **AI Agent Router**: Routes to appropriate AI agent if applicable
- **Analytics Pipeline**: Records interaction metrics

#### 2.2 `operadora.interaction.classified`
**Purpose**: Interaction classified and routed

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T10:30:15Z",
  "event_type": "InteractionClassified",
  "correlation_id": "uuid",
  "payload": {
    "interaction_id": "uuid",
    "beneficiary_id": "uuid",
    "classification": {
      "category": "SCHEDULING",
      "subcategory": "CONSULTATION_SCHEDULING",
      "urgency": "MEDIUM",
      "complexity": "SIMPLE",
      "sentiment": "NEUTRAL"
    },
    "routing_decision": {
      "target_layer": "SELF_SERVICE",
      "target_process": "SUB-004",
      "reason": "SIMPLE_SCHEDULING_REQUEST",
      "confidence": 0.95
    }
  },
  "metadata": {
    "source_system": "AI_CLASSIFIER",
    "model_version": "1.2.3",
    "producer_id": "classifier-service",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Process Router**: Starts appropriate subprocess (SUB-004, SUB-005, SUB-007)
- **Metrics Collector**: Tracks classification accuracy

#### 2.3 `operadora.notification.outbound`
**Purpose**: Outbound notification to beneficiary

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T10:35:00Z",
  "event_type": "NotificationOutbound",
  "correlation_id": "uuid",
  "payload": {
    "notification_id": "uuid",
    "beneficiary_id": "uuid",
    "channel": "WHATSAPP",
    "template_id": "APPOINTMENT_CONFIRMATION",
    "template_params": {
      "appointment_date": "2024-12-15",
      "appointment_time": "14:00",
      "physician_name": "Dr. Maria Santos",
      "specialty": "Cardiologia",
      "location": "Clínica Central"
    },
    "priority": "HIGH",
    "scheduled_send_at": "2024-12-11T10:35:00Z"
  },
  "metadata": {
    "source_system": "NOTIFICATION_SERVICE",
    "producer_id": "notification-orchestrator",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **WhatsApp Sender**: Sends message via WhatsApp Business API
- **Email Sender**: Sends email if channel is EMAIL
- **SMS Sender**: Sends SMS if channel is SMS
- **Delivery Tracker**: Tracks delivery status

### 3. Authorization Topics

#### 3.1 `operadora.authorization.requested`
**Purpose**: New authorization request received

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T11:00:00Z",
  "event_type": "AuthorizationRequested",
  "correlation_id": "uuid",
  "payload": {
    "authorization_id": "uuid",
    "request_number": "AUTH-2024-12345",
    "beneficiary_id": "uuid",
    "request_type": "EXAM",
    "procedure_code": "40101010",
    "procedure_name": "Ecocardiograma com Doppler",
    "icd10_code": "I50.0",
    "requesting_physician": {
      "physician_id": "PHYS-001",
      "name": "Dr. João Carvalho",
      "crm": "12345-SP"
    },
    "provider": {
      "provider_id": "PROV-100",
      "name": "Lab Central"
    },
    "urgency": "ROUTINE",
    "requested_at": "2024-12-11T11:00:00Z"
  },
  "metadata": {
    "source_system": "TASY_ERP",
    "producer_id": "tasy-authorization-connector",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Authorization Process Starter**: Triggers SUB-006 (Intelligent Authorization)
- **Eligibility Checker**: Validates beneficiary eligibility
- **Clinical Protocol Engine**: Applies business rules

#### 3.2 `operadora.authorization.decided`
**Purpose**: Authorization decision made (approved/denied)

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T11:05:00Z",
  "event_type": "AuthorizationDecided",
  "correlation_id": "uuid",
  "payload": {
    "authorization_id": "uuid",
    "request_number": "AUTH-2024-12345",
    "beneficiary_id": "uuid",
    "decision": "APPROVED",
    "decision_type": "AUTOMATIC",
    "authorization_code": "AUTH-CODE-789",
    "valid_from": "2024-12-11",
    "valid_until": "2024-12-31",
    "authorized_quantity": 1,
    "processing_time_seconds": 285,
    "decided_at": "2024-12-11T11:05:00Z"
  },
  "metadata": {
    "source_system": "AUTHORIZATION_ENGINE",
    "decision_engine_version": "2.1.0",
    "producer_id": "auth-decision-service",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Tasy ERP Connector**: Updates authorization in Tasy
- **Notification Service**: Notifies beneficiary and provider
- **Audit Service**: Records decision for compliance
- **Analytics Pipeline**: Tracks authorization metrics

### 4. Risk & Proactive Monitoring Topics

#### 4.1 `operadora.risk.stratification-updated`
**Purpose**: Beneficiary risk stratification calculated or updated

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T12:00:00Z",
  "event_type": "RiskStratificationUpdated",
  "correlation_id": "uuid",
  "payload": {
    "beneficiary_id": "uuid",
    "stratification_id": "uuid",
    "previous_risk_level": "MODERATE",
    "new_risk_level": "HIGH",
    "risk_score": 78.5,
    "risk_factors": [
      {"factor": "MULTIPLE_ER_VISITS", "weight": 0.25},
      {"factor": "MEDICATION_NON_ADHERENCE", "weight": 0.20},
      {"factor": "CHRONIC_DISEASE_UNCONTROLLED", "weight": 0.33}
    ],
    "model_version": "1.5.0",
    "confidence": 0.89,
    "valid_from": "2024-12-11",
    "valid_until": "2025-01-11"
  },
  "metadata": {
    "source_system": "ML_RISK_ENGINE",
    "model_algorithm": "XGBOOST",
    "producer_id": "risk-calculator-01",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Care Plan Service**: Adjusts care plans based on new risk
- **Proactive Motor**: Triggers proactive interventions
- **Navigator Assignment**: Assigns navigator for high-risk beneficiaries
- **Analytics Pipeline**: Tracks risk distribution

#### 4.2 `operadora.proactive.trigger-fired`
**Purpose**: Proactive trigger condition met

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T13:00:00Z",
  "event_type": "ProactiveTriggerFired",
  "correlation_id": "uuid",
  "payload": {
    "trigger_id": "uuid",
    "trigger_rule_id": "RULE-MEDICATION-REFILL",
    "trigger_rule_name": "Medication Refill Reminder",
    "beneficiary_id": "uuid",
    "trigger_condition": {
      "type": "MEDICATION_EXPIRING",
      "medication_name": "Losartana 50mg",
      "days_until_expiry": 5,
      "refills_remaining": 0
    },
    "recommended_action": {
      "action_type": "SEND_REMINDER",
      "channel": "WHATSAPP",
      "template_id": "MEDICATION_REFILL_REMINDER",
      "urgency": "MEDIUM"
    }
  },
  "metadata": {
    "source_system": "PROACTIVE_MOTOR",
    "rule_engine_version": "1.3.0",
    "producer_id": "proactive-engine-01",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Notification Service**: Sends proactive notification
- **Care Navigator**: Creates task for navigator if needed
- **Analytics Pipeline**: Tracks proactive intervention effectiveness

### 5. Care Management Topics

#### 5.1 `operadora.care-plan.created`
**Purpose**: New care plan created for beneficiary

**Partitions**: 3

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T14:00:00Z",
  "event_type": "CarePlanCreated",
  "correlation_id": "uuid",
  "payload": {
    "care_plan_id": "uuid",
    "beneficiary_id": "uuid",
    "plan_type": "CHRONIC_DISEASE",
    "plan_name": "Diabetes Type 2 Management",
    "start_date": "2024-12-11",
    "goals": [
      {"goal": "HbA1c < 7%", "target_date": "2025-03-11"},
      {"goal": "Weight reduction -5kg", "target_date": "2025-03-11"}
    ],
    "interventions": [
      {"type": "MONTHLY_CONSULTATION", "frequency": "MONTHLY"},
      {"type": "QUARTERLY_LAB_TESTS", "frequency": "QUARTERLY"},
      {"type": "NUTRITION_COUNSELING", "frequency": "BIWEEKLY"}
    ],
    "assigned_navigator_id": "uuid"
  },
  "metadata": {
    "source_system": "CARE_PLAN_SERVICE",
    "producer_id": "care-plan-creator",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Care Navigator**: Assigns tasks to navigator
- **Scheduling Service**: Schedules initial appointments
- **Analytics Pipeline**: Tracks care plan enrollment

#### 5.2 `operadora.chronic-program.enrolled`
**Purpose**: Beneficiary enrolled in chronic disease program

**Partitions**: 3

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T15:00:00Z",
  "event_type": "ChronicProgramEnrolled",
  "correlation_id": "uuid",
  "payload": {
    "enrollment_id": "uuid",
    "beneficiary_id": "uuid",
    "program_code": "DIABETES_MGMT",
    "program_name": "Programa de Gestão de Diabetes",
    "disease_codes": ["E11.9"],
    "enrollment_date": "2024-12-11",
    "protocol_version": "2.0",
    "contact_frequency": "WEEKLY"
  },
  "metadata": {
    "source_system": "CHRONIC_PROGRAM_SERVICE",
    "producer_id": "program-enrollment",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Proactive Motor**: Configures program-specific triggers
- **Care Navigator**: Initiates contact protocol
- **Analytics Pipeline**: Tracks program enrollment

### 6. Process Events Topics

#### 6.1 `operadora.process.started`
**Purpose**: Camunda process instance started

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T16:00:00Z",
  "event_type": "ProcessStarted",
  "correlation_id": "uuid",
  "payload": {
    "process_definition_key": "SUB-001",
    "process_definition_name": "Onboarding Inteligente",
    "process_instance_id": "abc123",
    "business_key": "beneficiary:uuid",
    "start_user_id": "system",
    "variables": {
      "beneficiaryId": "uuid",
      "admissionDate": "2024-12-01"
    }
  },
  "metadata": {
    "source_system": "CAMUNDA_BPM",
    "producer_id": "process-engine",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Process Monitoring Dashboard**: Real-time process tracking
- **Analytics Pipeline**: Process performance metrics

#### 6.2 `operadora.process.completed`
**Purpose**: Camunda process instance completed

**Partitions**: 6

**Message Schema**:
```json
{
  "schema_version": "1.0",
  "event_id": "uuid",
  "event_timestamp": "2024-12-11T16:30:00Z",
  "event_type": "ProcessCompleted",
  "correlation_id": "uuid",
  "payload": {
    "process_definition_key": "SUB-001",
    "process_instance_id": "abc123",
    "business_key": "beneficiary:uuid",
    "end_state": "COMPLETED",
    "duration_seconds": 1800,
    "variables": {
      "onboardingComplete": true,
      "riskLevel": "MODERATE",
      "healthProfileComplete": true
    }
  },
  "metadata": {
    "source_system": "CAMUNDA_BPM",
    "producer_id": "process-engine",
    "trace_id": "uuid"
  }
}
```

**Consumers**:
- **Process Monitoring Dashboard**: Update process status
- **Analytics Pipeline**: Calculate process metrics
- **ML Training Pipeline**: Feed process outcome data

## Producer Implementation Patterns

### Java Producer Example (Spring Kafka)
```java
@Service
@RequiredArgsConstructor
public class BeneficiaryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBeneficiaryCreated(Beneficiary beneficiary) {
        BeneficiaryCreatedEvent event = BeneficiaryCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventTimestamp(Instant.now())
            .eventType("BeneficiaryCreated")
            .correlationId(UUID.randomUUID().toString())
            .payload(mapToPayload(beneficiary))
            .metadata(buildMetadata())
            .build();

        // Partition by beneficiary ID for ordering
        String key = beneficiary.getId().toString();

        kafkaTemplate.send("operadora.beneficiary.created", key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event", ex);
                } else {
                    log.info("Event published: {}", result.getRecordMetadata());
                }
            });
    }
}
```

## Consumer Implementation Patterns

### Java Consumer Example (Spring Kafka)
```java
@Service
@Slf4j
public class BeneficiaryEventConsumer {

    @Autowired
    private RuntimeService runtimeService;

    @KafkaListener(
        topics = "operadora.beneficiary.created",
        groupId = "orchestrator-starter",
        concurrency = "3"
    )
    public void handleBeneficiaryCreated(
            @Payload BeneficiaryCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("Received event: {} from partition {}", event.getEventId(), partition);

        try {
            // Start orchestrator process
            ProcessInstance processInstance = runtimeService
                .createMessageCorrelation("BeneficiarioAdicionado")
                .setVariable("beneficiaryId", event.getPayload().getBeneficiaryId())
                .setVariable("externalId", event.getPayload().getExternalId())
                .setVariable("correlationId", event.getCorrelationId())
                .correlateWithResult();

            log.info("Started process: {}", processInstance.getId());

        } catch (Exception ex) {
            log.error("Failed to process event", ex);
            throw ex; // Will trigger retry or DLQ
        }
    }
}
```

## Error Handling & Dead Letter Queues

### DLQ Configuration
For each main topic, create a corresponding DLQ:
- `operadora.beneficiary.created.dlq`
- `operadora.interaction.received.dlq`
- etc.

### Retry Configuration
```yaml
spring:
  kafka:
    consumer:
      properties:
        max.poll.interval.ms: 300000
        session.timeout.ms: 45000
    listener:
      ack-mode: manual
      retry:
        max-attempts: 3
        backoff:
          multiplier: 2
          initial-interval: 1000
          max-interval: 10000
```

## Monitoring & Metrics

### Key Kafka Metrics to Monitor
1. **Throughput**: Messages/second per topic
2. **Lag**: Consumer lag per partition
3. **Error Rate**: Failed messages / total messages
4. **Latency**: End-to-end message latency (P50, P95, P99)

### Prometheus Metrics Exposure
```java
@Component
public class KafkaMetricsCollector {

    @Autowired
    private MeterRegistry meterRegistry;

    public void recordMessageProcessed(String topic, boolean success) {
        meterRegistry.counter("kafka.messages.processed",
            "topic", topic,
            "status", success ? "success" : "failure"
        ).increment();
    }

    public void recordProcessingTime(String topic, long durationMs) {
        meterRegistry.timer("kafka.message.processing.time",
            "topic", topic
        ).record(Duration.ofMillis(durationMs));
    }
}
```

## Related Documents
- [01_PROJECT_STRUCTURE.md](./01_PROJECT_STRUCTURE.md)
- [03_DATABASE_SCHEMA.md](./03_DATABASE_SCHEMA.md)
- [05_DEPLOYMENT_STRATEGY.md](./05_DEPLOYMENT_STRATEGY.md)
