# Architecture Documentation Index

## Overview
This directory contains the complete technical architecture documentation for the **AUSTA Operadora Digital** platform - a Camunda 7 BPM-based digital healthcare experience orchestration system.

## Quick Start
👉 **Start here**: [00_ARCHITECTURE_OVERVIEW.md](./00_ARCHITECTURE_OVERVIEW.md)

## Document Structure

### Core Architecture Documents

| Document | Description | Key Content |
|----------|-------------|-------------|
| [00_ARCHITECTURE_OVERVIEW.md](./00_ARCHITECTURE_OVERVIEW.md) | Executive summary and high-level architecture | System vision, technology stack, deployment architecture |
| [01_PROJECT_STRUCTURE.md](./01_PROJECT_STRUCTURE.md) | Complete project directory and package structure | Maven modules, Java packages, file organization |
| [02_DEPENDENCY_SPECIFICATION.md](./02_DEPENDENCY_SPECIFICATION.md) | Maven POM configuration and dependency management | Camunda 7.20, Spring Boot 3.2, PostgreSQL, Kafka |
| [03_DATABASE_SCHEMA.md](./03_DATABASE_SCHEMA.md) | PostgreSQL database schema design | 4 schemas, 12+ tables, audit trail, LGPD compliance |
| [04_KAFKA_ARCHITECTURE.md](./04_KAFKA_ARCHITECTURE.md) | Event-driven messaging topology | 12 topic categories, message schemas, producers/consumers |
| [05_DEPLOYMENT_STRATEGY.md](./05_DEPLOYMENT_STRATEGY.md) | Docker and Kubernetes deployment | Multi-stage Dockerfile, K8s manifests, CI/CD pipeline |
| [06_INTEGRATION_ARCHITECTURE.md](./06_INTEGRATION_ARCHITECTURE.md) | External system integration patterns | Tasy ERP, WhatsApp, IBM RPA, ML/AI engine |

### Architecture Decision Records (ADRs)

Located in `./adr/` directory:

| ADR | Decision | Status |
|-----|----------|--------|
| [ADR-001](./adr/ADR-001-build-tool-selection.md) | Build Tool Selection: Maven vs Gradle | ✅ Accepted |
| [ADR-002](./adr/ADR-002-module-organization.md) | Multi-Module Maven Project Organization | ✅ Accepted |
| [ADR-003](./adr/ADR-003-integration-patterns.md) | External Integration Pattern Selection | ✅ Accepted |

## Architecture Layers

### 1. Presentation Layer
- **WhatsApp Business API**: Primary communication channel (90%+ beneficiaries)
- **Web Portal**: Secondary channel for complex interactions
- **Mobile App**: iOS/Android native applications

### 2. Orchestration Layer (Camunda 7 BPM)
- **Main Orchestrator**: `PROC-ORC-001` - Beneficiary lifecycle management
- **10 Subprocesses**: SUB-001 through SUB-010 for specialized workflows
- **Event Subprocesses**: Global event handling (cancellation, óbito, fraud)

### 3. Business Services Layer
- **6 Maven Modules**:
  - `operadora-domain`: Core entities and value objects
  - `operadora-services`: Business logic
  - `operadora-bpmn-delegates`: Camunda service tasks
  - `operadora-integration`: External system adapters
  - `operadora-messaging`: Kafka integration
  - `operadora-webapp`: Spring Boot application

### 4. Event Streaming Layer (Apache Kafka)
- **12 Topic Categories**:
  - Beneficiary lifecycle events
  - Interactions and communications
  - Authorization workflow
  - Risk and proactive monitoring
  - Care management
  - Process execution events

### 5. Data Persistence Layer
- **PostgreSQL**: 4 schemas (camunda, operadora, audit, reporting)
- **Redis**: Session management and API caching
- **Audit Trail**: LGPD-compliant audit logging

### 6. External Integration Layer
- **Tasy ERP**: OAuth2 REST API (master data source)
- **WhatsApp Business API**: REST + Webhook (communication)
- **IBM RPA**: External tasks (automation)
- **ML/AI Engine**: REST API (risk stratification, sentiment analysis)

## Key Technical Decisions

### Technology Choices
1. **Camunda 7.20.0**: Chosen for on-premise deployment control and mature ecosystem
2. **Spring Boot 3.2.1**: Latest LTS with Java 17 support
3. **PostgreSQL 16**: JSONB support for flexible data structures
4. **Apache Kafka 3.6.1**: High-throughput event streaming with exactly-once semantics
5. **Maven**: Build tool aligned with Camunda ecosystem

### Architecture Patterns
1. **Event-Driven Architecture**: Kafka as event bus for loose coupling
2. **Domain-Driven Design**: Clear bounded contexts and ubiquitous language
3. **Hexagonal Architecture**: Core domain independent of external systems
4. **Resilience Patterns**: Circuit breakers, retries, bulkheads, fallbacks

### Module Organization
- **Multi-Module Maven Project**: 6 modules with clear dependency graph
- **Dependency Flow**: webapp → bpmn → services → [integration, messaging] → domain
- **Rationale**: Enforced boundaries, reusability, faster incremental builds

### Integration Strategy
- **Hybrid Approach**:
  - Synchronous REST for fast, reliable operations (<2s)
  - Async External Tasks for long-running operations (>5s)
  - Kafka for fire-and-forget and rate-limited integrations

## Deployment Architecture

### Kubernetes Deployment
```
Production Configuration:
- Camunda Pods: 3 replicas (autoscaling 3-10)
- PostgreSQL: 3-node cluster with automatic failover
- Redis: Sentinel mode with 3 instances
- Kafka: 3 brokers with replication factor 3
- Resources per Camunda Pod: 1 CPU, 2Gi RAM
```

### High Availability
- **Application**: Multi-replica deployment with session affinity
- **Database**: Master-replica with automatic failover
- **Cache**: Redis Sentinel for automatic failover
- **Messaging**: Kafka with 3 brokers and RF=3

## Security & Compliance

### Authentication
- **External APIs**: OAuth2 + JWT tokens
- **Internal Services**: mTLS + Kubernetes service accounts
- **Database**: Role-based access control (RBAC)

### Data Protection
- **Encryption at Rest**: PostgreSQL TDE, encrypted Kubernetes volumes
- **Encryption in Transit**: TLS 1.3 for all communications
- **LGPD Compliance**: Audit trail, consent management, data retention

### Monitoring & Observability
- **Metrics**: Prometheus + Grafana
- **Logs**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Traces**: Jaeger distributed tracing

## Performance Targets

| Metric | Target | Baseline |
|--------|--------|----------|
| Authorization SLA | <5 min (85%) | 15-30 min |
| First Contact Resolution | >90% | 45% |
| Process Throughput | 10K/day | 2K/day |
| API Latency (P95) | <500ms | 1-2s |

## Documentation Standards

### Document Naming
- **Sequential Numbers**: 00-99 for core documents
- **Descriptive Names**: Clear indication of content
- **File Extension**: `.md` for Markdown

### ADR Template
All ADRs follow standardized template:
1. Status (Proposed/Accepted/Superseded)
2. Context
3. Decision Drivers
4. Considered Options
5. Decision
6. Rationale
7. Consequences
8. Related Decisions

## Contributing to Documentation

### When to Create New Documents
1. **New Architecture Layer**: Add to core documents (01-06)
2. **Major Technical Decision**: Create new ADR
3. **Integration Guide**: Add to `integration/` subdirectory
4. **Deployment Guide**: Add to `deployment/` subdirectory

### Document Update Process
1. Update relevant document
2. Update version number and date
3. Update this README if adding new documents
4. Commit with descriptive message
5. Notify team of significant changes

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-11 | System Architect Agent | Initial complete architecture |

## Related Documentation

### Project Root
- [README.md](../../README.md) - Project overview
- [CLAUDE.md](../../CLAUDE.md) - Development environment setup

### Process Documentation
- [PROMPT_TÉCNICO_BPMN](../../PROMPT_TÉCNICO_BPMN) - Detailed BPMN requirements

### Code Documentation
- JavaDoc in source code
- API documentation (generated by Swagger/OpenAPI)

## Glossary

| Term | Definition |
|------|------------|
| **Beneficiary** | Health plan member |
| **Care Journey** | Complete lifecycle of beneficiary from onboarding to care completion |
| **Authorization** | Approval process for medical procedures |
| **Risk Stratification** | ML-based classification of beneficiary health risk |
| **Proactive Motor** | Background process that anticipates beneficiary needs |
| **Care Navigator** | Healthcare professional coordinating complex cases |
| **Self-Service Layer** | Automated task resolution without human intervention |
| **Journey State** | Current lifecycle state of beneficiary (NEW, ONBOARDING, ACTIVE, etc.) |

## Support

### Questions or Issues
- **Architecture Questions**: Create issue with label `architecture`
- **Documentation Updates**: Create PR with changes
- **Technical Discussions**: Use team Slack channel `#architecture`

### Useful Links
- [Camunda 7.20 Documentation](https://docs.camunda.org/manual/7.20/)
- [Spring Boot 3.2 Documentation](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

---

**Last Updated**: 2025-12-11
**Architecture Team**: System Architecture Designer (Swarm Agent)
**Swarm ID**: swarm-1765461163705-5dcagdbkh
