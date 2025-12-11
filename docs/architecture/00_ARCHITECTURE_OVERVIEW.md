# Architecture Overview - Camunda 7 BPMN Platform

## Document Control
- **Version**: 1.0
- **Author**: System Architecture Designer (Swarm Agent)
- **Date**: 2025-12-11
- **Status**: Final
- **Swarm ID**: swarm-1765461163705-5dcagdbkh

## Executive Summary
This document provides a comprehensive overview of the technical architecture for the **AUSTA Operadora Digital** platform, a Camunda 7 BPM-based system designed to orchestrate the complete digital healthcare experience for health plan beneficiaries.

## Architecture Vision
Transform healthcare operations from **reactive to proactive**, **manual to automated**, and **fragmented to integrated** through intelligent process orchestration, event-driven architecture, and AI-powered decision-making.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      DIGITAL HEALTH OPERATOR PLATFORM                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    PRESENTATION LAYER                            │  │
│  │  - WhatsApp Business API  - Web Portal  - Mobile App            │  │
│  └────────────────────────┬─────────────────────────────────────────┘  │
│                           │                                             │
│  ┌────────────────────────┴─────────────────────────────────────────┐  │
│  │              CAMUNDA 7 BPM ORCHESTRATION LAYER                   │  │
│  │                                                                   │  │
│  │  ┌─────────────────────────────────────────────────────────┐    │  │
│  │  │  Main Orchestrator (PROC-ORC-001)                       │    │  │
│  │  │  - Beneficiary Journey Lifecycle Management             │    │  │
│  │  │  - State Machine Coordination                           │    │  │
│  │  │  - Event Subprocess Handling                            │    │  │
│  │  └─────────────────────────────────────────────────────────┘    │  │
│  │                                                                   │  │
│  │  ┌──────────┬──────────┬──────────┬──────────┬──────────┐       │  │
│  │  │ SUB-001  │ SUB-002  │ SUB-003  │ SUB-004  │ SUB-005  │       │  │
│  │  │Onboarding│ Proactive│ Reception│Self-Svc  │ AI Agents│       │  │
│  │  └──────────┴──────────┴──────────┴──────────┴──────────┘       │  │
│  │  ┌──────────┬──────────┬──────────┬──────────┬──────────┐       │  │
│  │  │ SUB-006  │ SUB-007  │ SUB-008  │ SUB-009  │ SUB-010  │       │  │
│  │  │  Authz   │Navigation│ Chronic  │Complaints│Follow-up │       │  │
│  │  └──────────┴──────────┴──────────┴──────────┴──────────┘       │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                           │                                             │
│  ┌────────────────────────┴─────────────────────────────────────────┐  │
│  │                   BUSINESS SERVICES LAYER                        │  │
│  │  - Beneficiary Management    - Risk Stratification               │  │
│  │  - Care Plan Orchestration   - Authorization Engine              │  │
│  │  - Communication Service     - ML/AI Integration                 │  │
│  └────────────────────────┬─────────────────────────────────────────┘  │
│                           │                                             │
│  ┌────────────────────────┴─────────────────────────────────────────┐  │
│  │                    EVENT STREAMING LAYER                         │  │
│  │                      Apache Kafka Cluster                        │  │
│  │  - 12 Topic Categories  - Event-Driven Integration               │  │
│  │  - Real-time Processing - Async Communication                    │  │
│  └────────────────────────┬─────────────────────────────────────────┘  │
│                           │                                             │
│  ┌────────────────────────┴─────────────────────────────────────────┐  │
│  │                  DATA PERSISTENCE LAYER                          │  │
│  │  ┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐   │  │
│  │  │   PostgreSQL     │  │ Redis Cache  │  │ Audit & Reporting│   │  │
│  │  │  - 4 Schemas     │  │ - Sessions   │  │ - LGPD Compliance│   │  │
│  │  │  - Process Vars  │  │ - API Cache  │  │ - Analytics Views│   │  │
│  │  └──────────────────┘  └──────────────┘  └──────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                           │                                             │
│  ┌────────────────────────┴─────────────────────────────────────────┐  │
│  │                  EXTERNAL INTEGRATIONS                           │  │
│  │  ┌──────────┬──────────┬──────────┬──────────┬──────────┐       │  │
│  │  │ Tasy ERP │ WhatsApp │ IBM RPA  │ ML/AI    │  Email   │       │  │
│  │  │  (OAuth) │(REST API)│(REST API)│  Engine  │  (SMTP)  │       │  │
│  │  └──────────┴──────────┴──────────┴──────────┴──────────┘       │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Key Architectural Principles

### 1. Event-Driven Architecture
- **Kafka as Event Bus**: All system events published to Kafka topics
- **Loose Coupling**: Services communicate via events, not direct calls
- **Temporal Decoupling**: Producers/consumers operate independently
- **Scalability**: Horizontal scaling through partitioned topics

### 2. Domain-Driven Design
- **Bounded Contexts**: Clear boundaries between beneficiary, authorization, care management
- **Ubiquitous Language**: Business terms used in code (beneficiary, care plan, journey state)
- **Aggregate Roots**: Beneficiary, Authorization, Care Plan
- **Value Objects**: Risk Score, Contact Info, Address

### 3. Hexagonal Architecture (Ports & Adapters)
- **Core Domain**: Pure business logic with no external dependencies
- **Ports**: Interfaces defining contracts (TasyPort, WhatsAppPort)
- **Adapters**: Implementations of ports (TasyRestAdapter, WhatsAppApiAdapter)
- **Dependency Inversion**: Core depends on abstractions, not implementations

### 4. Resilience Patterns
- **Circuit Breakers**: Prevent cascading failures (Resilience4j)
- **Retries with Backoff**: Exponential backoff for transient failures
- **Timeouts**: Connection and read timeouts for all external calls
- **Bulkheads**: Isolate thread pools per integration
- **Fallbacks**: Graceful degradation when services unavailable

## Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **BPM Engine** | Camunda Platform | 7.20.0 | Process orchestration |
| **Application Framework** | Spring Boot | 3.2.1 | Application infrastructure |
| **JVM** | OpenJDK | 17 | Runtime environment |
| **Database** | PostgreSQL | 16 | Transactional data store |
| **Cache** | Redis | 7 | Session & API caching |
| **Event Streaming** | Apache Kafka | 3.6.1 | Event-driven messaging |
| **Containerization** | Docker | 24 | Application packaging |
| **Orchestration** | Kubernetes | 1.28+ | Container orchestration |
| **Monitoring** | Prometheus + Grafana | Latest | Observability |
| **API Gateway** | NGINX Ingress | Latest | Traffic management |

## Module Architecture

### Maven Multi-Module Structure
```
operadora-digital-parent/
├── operadora-domain           # Core domain entities
├── operadora-services         # Business logic services
├── operadora-bpmn-delegates   # Camunda delegates
├── operadora-integration      # External system adapters
├── operadora-messaging        # Kafka producers/consumers
└── operadora-webapp           # Spring Boot application
```

### Dependency Graph
```
webapp → bpmn-delegates → services → [integration, messaging] → domain
```

## Process Architecture

### Main Orchestrator: PROC-ORC-001
- **Purpose**: Coordinate entire beneficiary journey lifecycle
- **Trigger**: Message event on beneficiary creation
- **Lifecycle**: Long-running (months to years per instance)
- **State Management**: Tracks beneficiary through journey states
- **Event Subprocesses**: Handles global events (cancellation, óbito, fraud)

### Subprocesses (10 Total)
| ID | Name | Trigger | Duration |
|----|------|---------|----------|
| SUB-001 | Onboarding & Screening | Message | ~30 min |
| SUB-002 | Proactive Motor | Timer (Daily) | Continuous |
| SUB-003 | Reception & Classification | Message | <3 min |
| SUB-004 | Self-Service | Signal | <1 min |
| SUB-005 | AI Agents | Signal | <5 min |
| SUB-006 | Intelligent Authorization | Message | <5 min |
| SUB-007 | Care Navigation | Signal | Days-Weeks |
| SUB-008 | Chronic Disease Management | Signal | Months |
| SUB-009 | Complaints Management | Message | Hours-Days |
| SUB-010 | Follow-up & Feedback | Signal | <10 min |

## Data Architecture

### Database Schemas

#### 1. Camunda Schema (`camunda`)
- **Managed by**: Camunda engine
- **Tables**: ~40 engine tables (ACT_RE_*, ACT_RU_*, ACT_HI_*)
- **Purpose**: Process execution, history, jobs

#### 2. Business Schema (`operadora`)
- **Tables**: 8 core tables
  - `beneficiaries` - Beneficiary master data
  - `health_profiles` - Health screening data
  - `risk_stratifications` - ML risk assessments
  - `care_plans` - Care coordination plans
  - `chronic_program_enrollments` - Program participation
  - `interactions` - All beneficiary touchpoints
  - `authorization_requests` - Authorization workflow
  - `process_variables` - Extended process data
- **Purpose**: Business domain data

#### 3. Audit Schema (`audit`)
- **Tables**: 2 tables
  - `audit_log` - Comprehensive audit trail
  - `consent_records` - LGPD consent management
- **Purpose**: Compliance, LGPD, auditability

#### 4. Reporting Schema (`reporting`)
- **Views**: Materialized views for analytics
  - `mv_active_care_journeys` - Real-time journey tracking
  - `mv_authorization_metrics` - Authorization performance
- **Purpose**: Operational dashboards, BI

### Data Flow
```
External System → Kafka → Process Instance → Database → Materialized View → Dashboard
```

## Integration Architecture

### Integration Patterns

| System | Pattern | Sync/Async | SLA |
|--------|---------|------------|-----|
| **Tasy ERP** | REST API + OAuth2 | Sync | <500ms |
| **WhatsApp API** | REST + Webhook | Async | ~2-5s |
| **IBM RPA** | External Task | Async | Minutes |
| **ML/AI Engine** | REST API | Sync | <1s |

### Resilience Configuration
```yaml
Circuit Breaker:
  - Sliding window: 10 requests
  - Failure threshold: 50%
  - Open duration: 30s

Retry:
  - Max attempts: 3
  - Backoff: Exponential (2s, 4s, 8s)
  - Jitter: Enabled

Timeout:
  - Connection: 10s
  - Read: 30s
```

## Deployment Architecture

### Kubernetes Deployment

```yaml
Namespaces:
  - operadora-digital         # Application namespace
  - operadora-data           # Data layer (PG, Redis, Kafka)
  - operadora-monitoring     # Observability stack

Deployments:
  - camunda-app (3 replicas)
  - external-task-workers (2-10 autoscaling)
  - postgres (3-node cluster)
  - redis (3-node cluster)
  - kafka (3 brokers)

Resources per Camunda Pod:
  - CPU: 1000m (limit), 500m (request)
  - Memory: 2Gi (limit), 1Gi (request)
```

### High Availability
- **Camunda Engine**: 3 replicas with session affinity
- **PostgreSQL**: Master-replica with automatic failover
- **Kafka**: 3 brokers with replication factor 3
- **Redis**: Sentinel mode with 3 instances

## Security Architecture

### Authentication & Authorization
- **External Users**: OAuth2 + JWT tokens
- **Internal Services**: mTLS + Service accounts
- **Database**: Role-based access control (RBAC)
- **API Gateway**: Rate limiting + WAF

### Data Protection
- **Encryption at Rest**: PostgreSQL TDE, encrypted volumes
- **Encryption in Transit**: TLS 1.3 for all communications
- **Secrets Management**: Kubernetes secrets + HashiCorp Vault
- **LGPD Compliance**: Audit logs, consent management, data retention policies

## Observability

### Three Pillars

#### 1. Metrics (Prometheus)
- **Application Metrics**: JVM, Spring Boot actuator
- **Camunda Metrics**: Process instances, job executor
- **Integration Metrics**: API latency, error rates
- **Business Metrics**: Authorizations/min, NPS, FCR

#### 2. Logs (ELK Stack)
- **Structured Logging**: JSON format with correlation IDs
- **Log Levels**: DEBUG (dev), INFO (staging), WARN (prod)
- **Retention**: 30 days hot, 90 days cold

#### 3. Traces (Jaeger)
- **Distributed Tracing**: End-to-end request flows
- **Correlation**: Process instance ID as trace ID
- **Sampling**: 100% errors, 10% success

### Dashboards
- **Operational**: Real-time process health
- **Business**: KPIs (SLA compliance, NPS, cost per beneficiary)
- **Technical**: Latency percentiles, error rates, resource utilization

## Key Design Decisions (ADRs)

### ADR-001: Build Tool Selection
- **Decision**: Maven (over Gradle)
- **Rationale**: Camunda ecosystem alignment, team familiarity, enterprise stability

### ADR-002: Module Organization
- **Decision**: Multi-module Maven project (6 modules)
- **Rationale**: Clear boundaries, reusability, testability, faster builds

### ADR-003: Integration Patterns
- **Decision**: Hybrid (sync REST + async external tasks + Kafka)
- **Rationale**: Optimized per use case, balances latency vs reliability

## Performance Targets

| Metric | Target | Current (Baseline) |
|--------|--------|--------------------|
| **Authorization SLA** | <5 minutes (85%) | 15-30 minutes |
| **First Contact Resolution** | >90% | 45% |
| **Process Throughput** | 10,000 instances/day | 2,000/day |
| **API Latency (P95)** | <500ms | 1-2s |
| **Database Queries (P95)** | <100ms | 200-500ms |

## Scalability Considerations

### Horizontal Scaling
- **Camunda Pods**: 3-10 pods (autoscaling on CPU/memory)
- **External Task Workers**: Independent scaling per worker type
- **Database**: Read replicas for reporting queries
- **Kafka**: Additional brokers as throughput increases

### Vertical Scaling
- **Camunda Engine**: Up to 4 CPU, 8Gi memory per pod
- **PostgreSQL**: Up to 16 CPU, 64Gi memory
- **Kafka Brokers**: Up to 8 CPU, 32Gi memory

## Risk Mitigation

| Risk | Mitigation Strategy |
|------|---------------------|
| **Database Bottleneck** | Connection pooling, read replicas, query optimization |
| **Kafka Lag** | Horizontal consumer scaling, partition tuning |
| **External System Outage** | Circuit breakers, fallbacks, cached data |
| **Process Instance Explosion** | History cleanup, archiving, instance limits |
| **Memory Leaks** | Heap dump analysis, pod auto-restart, resource limits |

## Documentation Index

### Architecture Documents
1. [01_PROJECT_STRUCTURE.md](./01_PROJECT_STRUCTURE.md) - Directory and package structure
2. [02_DEPENDENCY_SPECIFICATION.md](./02_DEPENDENCY_SPECIFICATION.md) - Maven POM configuration
3. [03_DATABASE_SCHEMA.md](./03_DATABASE_SCHEMA.md) - PostgreSQL schema design
4. [04_KAFKA_ARCHITECTURE.md](./04_KAFKA_ARCHITECTURE.md) - Event streaming topology
5. [05_DEPLOYMENT_STRATEGY.md](./05_DEPLOYMENT_STRATEGY.md) - Docker & Kubernetes config
6. [06_INTEGRATION_ARCHITECTURE.md](./06_INTEGRATION_ARCHITECTURE.md) - External system integration

### Architecture Decision Records (ADRs)
1. [ADR-001: Build Tool Selection](./adr/ADR-001-build-tool-selection.md)
2. [ADR-002: Module Organization](./adr/ADR-002-module-organization.md)
3. [ADR-003: Integration Patterns](./adr/ADR-003-integration-patterns.md)

## Next Steps

### Implementation Phases
1. **Phase 1 - Foundation** (Weeks 1-4)
   - Project structure setup
   - Database schema creation
   - Kafka infrastructure
   - Base Camunda configuration

2. **Phase 2 - Core Processes** (Weeks 5-12)
   - Implement PROC-ORC-001 orchestrator
   - Implement SUB-001 to SUB-005 subprocesses
   - Integration adapters (Tasy, WhatsApp)

3. **Phase 3 - Advanced Features** (Weeks 13-18)
   - SUB-006 to SUB-010 subprocesses
   - ML/AI integration
   - RPA orchestration

4. **Phase 4 - Production Readiness** (Weeks 19-24)
   - Performance tuning
   - Security hardening
   - Monitoring dashboards
   - Load testing

## Conclusion
This architecture provides a solid foundation for a scalable, resilient, and maintainable BPM platform that will transform AUSTA's digital healthcare operations. The event-driven, microservices-ready design allows for future evolution while delivering immediate value through intelligent process orchestration.

---

**Document Version**: 1.0
**Last Updated**: 2025-12-11
**Architecture Designer**: System Architecture Designer (Swarm Agent)
**Swarm ID**: swarm-1765461163705-5dcagdbkh
