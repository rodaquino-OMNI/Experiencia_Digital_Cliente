# Project Structure - Camunda 7 BPMN Implementation

## Document Control
- **Version**: 1.0
- **Author**: System Architecture Designer
- **Date**: 2025-12-11
- **Status**: Draft

## Overview
This document defines the complete directory and package structure for the Camunda 7 BPM implementation of the Digital Health Operator platform.

## Root Directory Structure

```
experiencia-digital-cliente/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── austa/
│   │   │           └── operadora/
│   │   │               ├── bpmn/                    # BPMN-related code
│   │   │               │   ├── delegates/           # Service Task Delegates
│   │   │               │   │   ├── onboarding/
│   │   │               │   │   ├── proactive/
│   │   │               │   │   ├── reception/
│   │   │               │   │   ├── selfservice/
│   │   │               │   │   ├── ia/
│   │   │               │   │   ├── authorization/
│   │   │               │   │   ├── navigation/
│   │   │               │   │   ├── chronic/
│   │   │               │   │   ├── complaints/
│   │   │               │   │   └── followup/
│   │   │               │   ├── listeners/           # Execution/Task Listeners
│   │   │               │   ├── validators/          # Input Validators
│   │   │               │   └── connectors/          # External Task Workers
│   │   │               ├── dmn/                     # DMN Decision Logic
│   │   │               │   ├── evaluators/
│   │   │               │   └── functions/
│   │   │               ├── services/                # Business Services
│   │   │               │   ├── beneficiary/
│   │   │               │   ├── risk/
│   │   │               │   ├── authorization/
│   │   │               │   ├── communication/
│   │   │               │   ├── integration/
│   │   │               │   └── ml/
│   │   │               ├── domain/                  # Domain Models
│   │   │               │   ├── entities/
│   │   │               │   ├── valueobjects/
│   │   │               │   └── enums/
│   │   │               ├── repositories/            # Data Access
│   │   │               │   ├── jpa/
│   │   │               │   └── custom/
│   │   │               ├── messaging/               # Kafka Integration
│   │   │               │   ├── producers/
│   │   │               │   ├── consumers/
│   │   │               │   └── listeners/
│   │   │               ├── integration/             # External Systems
│   │   │               │   ├── tasy/
│   │   │               │   ├── whatsapp/
│   │   │               │   ├── rpa/
│   │   │               │   └── ml/
│   │   │               ├── config/                  # Configuration
│   │   │               │   ├── camunda/
│   │   │               │   ├── kafka/
│   │   │               │   ├── security/
│   │   │               │   └── database/
│   │   │               └── util/                    # Utilities
│   │   │                   ├── constants/
│   │   │                   └── helpers/
│   │   ├── resources/
│   │   │   ├── bpmn/                               # BPMN Process Files
│   │   │   │   ├── orchestrator/
│   │   │   │   │   └── PROC-ORC-001_Orquestracao_Cuidado_Experiencia.bpmn
│   │   │   │   ├── onboarding/
│   │   │   │   │   └── SUB-001_Onboarding_Screening.bpmn
│   │   │   │   ├── proactive/
│   │   │   │   │   └── SUB-002_Motor_Proativo.bpmn
│   │   │   │   ├── reception/
│   │   │   │   │   └── SUB-003_Recepcao_Classificacao.bpmn
│   │   │   │   ├── selfservice/
│   │   │   │   │   └── SUB-004_Self_Service.bpmn
│   │   │   │   ├── ia/
│   │   │   │   │   └── SUB-005_Agentes_IA.bpmn
│   │   │   │   ├── authorization/
│   │   │   │   │   └── SUB-006_Autorizacao_Inteligente.bpmn
│   │   │   │   ├── navigation/
│   │   │   │   │   └── SUB-007_Navegacao_Cuidado.bpmn
│   │   │   │   ├── chronic/
│   │   │   │   │   └── SUB-008_Gestao_Cronicos.bpmn
│   │   │   │   ├── complaints/
│   │   │   │   │   └── SUB-009_Gestao_Reclamacoes.bpmn
│   │   │   │   ├── followup/
│   │   │   │   │   └── SUB-010_Followup_Feedback.bpmn
│   │   │   │   └── support/
│   │   │   │       ├── ERROR_HANDLER.bpmn
│   │   │   │       └── COMPENSATION_HANDLER.bpmn
│   │   │   ├── dmn/                                # DMN Decision Tables
│   │   │   │   ├── risk-stratification.dmn
│   │   │   │   ├── authorization-rules.dmn
│   │   │   │   ├── routing-rules.dmn
│   │   │   │   ├── escalation-rules.dmn
│   │   │   │   └── communication-templates.dmn
│   │   │   ├── forms/                              # Camunda Forms
│   │   │   │   ├── onboarding/
│   │   │   │   ├── authorization/
│   │   │   │   └── complaints/
│   │   │   ├── scripts/                            # Groovy/JavaScript Scripts
│   │   │   │   ├── validators/
│   │   │   │   └── transformers/
│   │   │   ├── db/
│   │   │   │   ├── migration/                      # Flyway Migrations
│   │   │   │   │   ├── V1__create_camunda_tables.sql
│   │   │   │   │   ├── V2__create_beneficiary_tables.sql
│   │   │   │   │   ├── V3__create_process_variables_tables.sql
│   │   │   │   │   ├── V4__create_audit_tables.sql
│   │   │   │   │   └── V5__create_indexes.sql
│   │   │   │   └── seed/                           # Initial Data
│   │   │   │       ├── S1__reference_data.sql
│   │   │   │       └── S2__test_beneficiaries.sql
│   │   │   ├── config/
│   │   │   │   ├── application.yml                 # Main Configuration
│   │   │   │   ├── application-dev.yml
│   │   │   │   ├── application-test.yml
│   │   │   │   ├── application-prod.yml
│   │   │   │   ├── camunda.cfg.xml                 # Camunda Engine Config
│   │   │   │   └── logback-spring.xml              # Logging Configuration
│   │   │   ├── templates/                          # Message Templates
│   │   │   │   ├── whatsapp/
│   │   │   │   ├── email/
│   │   │   │   └── sms/
│   │   │   └── META-INF/
│   │   │       └── processes.xml                   # Process Application
│   │   └── webapp/                                 # Camunda Webapp Extensions
│   │       └── WEB-INF/
│   │           └── web.xml
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── austa/
│       │           └── operadora/
│       │               ├── bpmn/
│       │               │   ├── ProcessUnitTest.java
│       │               │   ├── orchestrator/
│       │               │   ├── onboarding/
│       │               │   └── ...
│       │               ├── integration/
│       │               │   ├── TasyIntegrationTest.java
│       │               │   └── KafkaIntegrationTest.java
│       │               └── services/
│       └── resources/
│           ├── bpmn/                               # Test Process Definitions
│           ├── application-test.yml
│           └── test-data/
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml                          # Local Development
│   ├── docker-compose.test.yml                     # Integration Tests
│   └── docker-compose.prod.yml                     # Production
├── k8s/                                            # Kubernetes Manifests
│   ├── configmaps/
│   ├── secrets/
│   ├── deployments/
│   ├── services/
│   ├── ingress/
│   └── monitoring/
├── scripts/
│   ├── deploy/
│   │   ├── deploy-bpmn.sh                         # Deploy BPMN to Camunda
│   │   └── rollback-bpmn.sh
│   ├── migration/
│   │   └── migrate-process-instances.sh
│   └── monitoring/
│       ├── export-metrics.sh
│       └── check-health.sh
├── docs/
│   ├── architecture/                               # THIS DIRECTORY
│   │   ├── adr/                                   # Architecture Decision Records
│   │   ├── diagrams/                              # C4, UML Diagrams
│   │   ├── integration/                           # Integration Specs
│   │   └── deployment/                            # Deployment Guides
│   ├── processes/                                  # Process Documentation
│   │   ├── orchestrator/
│   │   └── subprocesses/
│   ├── api/                                        # API Documentation
│   └── runbooks/                                   # Operational Guides
├── pom.xml                                         # Maven Configuration
├── .env.example                                    # Environment Variables Template
├── .gitignore
├── README.md
└── CHANGELOG.md
```

## Package Naming Conventions

### Base Package
```
com.austa.operadora
```

### Sub-packages Purpose

| Package | Purpose | Examples |
|---------|---------|----------|
| `bpmn.delegates` | Service Task implementations | `OnboardingScreeningDelegate.java` |
| `bpmn.listeners` | Execution/Task listeners | `ProcessStartListener.java` |
| `bpmn.validators` | Input validation logic | `BeneficiaryDataValidator.java` |
| `bpmn.connectors` | External task workers | `TasyConnectorWorker.java` |
| `dmn.evaluators` | DMN business logic | `RiskStratificationEvaluator.java` |
| `services` | Core business services | `BeneficiaryService.java` |
| `domain.entities` | JPA entities | `Beneficiary.java`, `CareJourney.java` |
| `domain.valueobjects` | Immutable value objects | `RiskScore.java`, `ContactInfo.java` |
| `domain.enums` | Enumerations | `BeneficiaryState.java`, `RiskLevel.java` |
| `repositories` | Data access layer | `BeneficiaryRepository.java` |
| `messaging.producers` | Kafka producers | `ProcessEventProducer.java` |
| `messaging.consumers` | Kafka consumers | `InteractionEventConsumer.java` |
| `integration.tasy` | Tasy ERP integration | `TasyApiClient.java` |
| `integration.whatsapp` | WhatsApp Business API | `WhatsAppService.java` |
| `config` | Spring configuration | `CamundaConfiguration.java` |

## File Naming Conventions

### BPMN Files
```
<PROCESS-ID>_<Descriptive_Name>.bpmn
```
Examples:
- `PROC-ORC-001_Orquestracao_Cuidado_Experiencia.bpmn`
- `SUB-001_Onboarding_Screening.bpmn`

### DMN Files
```
<decision-name>-<version>.dmn
```
Examples:
- `risk-stratification-v1.dmn`
- `authorization-rules-v2.dmn`

### Java Classes
```
<Domain><Action><Type>.java
```
Examples:
- `BeneficiaryOnboardingDelegate.java`
- `RiskCalculationService.java`
- `ProcessStartExecutionListener.java`

### Configuration Files
```
application-<environment>.yml
```
Examples:
- `application-dev.yml`
- `application-prod.yml`

## Process Definition Keys

### Orchestrator
- `PROC-ORC-001` - Main orchestration process

### Subprocesses
- `SUB-001` - Onboarding and Screening
- `SUB-002` - Proactive Motor
- `SUB-003` - Reception and Classification
- `SUB-004` - Self-Service
- `SUB-005` - AI Agents
- `SUB-006` - Intelligent Authorization
- `SUB-007` - Care Navigation
- `SUB-008` - Chronic Disease Management
- `SUB-009` - Complaints Management
- `SUB-010` - Follow-up and Feedback

### Support Processes
- `ERROR-HANDLER` - Global error handling
- `COMPENSATION-HANDLER` - Compensation logic

## Module Organization

### Core Modules (Maven Multi-Module)

```xml
<modules>
    <module>operadora-domain</module>           <!-- Domain models -->
    <module>operadora-bpmn-delegates</module>   <!-- BPMN implementations -->
    <module>operadora-services</module>         <!-- Business services -->
    <module>operadora-integration</module>      <!-- External integrations -->
    <module>operadora-messaging</module>        <!-- Kafka messaging -->
    <module>operadora-webapp</module>           <!-- Web application -->
</modules>
```

## Resource Organization

### BPMN Files Organization
All BPMN files are organized by subprocess category in `src/main/resources/bpmn/`. This allows:
- Clear separation of concerns
- Easy navigation and maintenance
- Modular deployment capabilities
- Version control per subprocess

### Configuration Files Organization
Configuration is environment-specific with profiles:
- `application.yml` - Common configuration
- `application-dev.yml` - Development overrides
- `application-test.yml` - Testing overrides
- `application-prod.yml` - Production overrides

## Build Artifacts

### Maven Build Output
```
target/
├── classes/                                    # Compiled classes
├── test-classes/                               # Test classes
├── operadora-digital-<version>.jar             # Executable JAR
├── operadora-digital-<version>-sources.jar     # Sources JAR
└── generated-sources/                          # Generated code
```

### Docker Image Layers
```
Layer 1: Base JRE (OpenJDK 17)
Layer 2: Camunda Runtime Libraries
Layer 3: Application Dependencies
Layer 4: Application Classes
Layer 5: Configuration Files
Layer 6: BPMN/DMN Resources
```

## Database Schema Organization

### Schema Namespaces
- `camunda` - Camunda engine tables (managed by Camunda)
- `operadora` - Application business tables
- `audit` - Audit and compliance tables
- `reporting` - Read-optimized reporting views

## Deployment Structure

### Kubernetes Deployment Units
1. **Camunda Engine Pod** - Process engine + embedded Tomcat
2. **External Task Workers Pod** - Scalable workers for async tasks
3. **PostgreSQL StatefulSet** - Database cluster
4. **Kafka Cluster** - Event streaming
5. **Redis Cache** - Session and cache layer

## Key Design Decisions

### 1. Monolithic vs Microservices
**Decision**: Start with modular monolith, prepare for microservices evolution.
**Rationale**:
- Faster initial development
- Easier operational overhead for MVP
- Clear module boundaries allow future extraction

### 2. BPMN File Organization
**Decision**: One file per subprocess, organized by category.
**Rationale**:
- Better version control
- Independent deployment cycles
- Reduced merge conflicts

### 3. Multi-Module Maven
**Decision**: Use Maven multi-module structure.
**Rationale**:
- Clear dependency management
- Reusable components
- Independent versioning per module

### 4. Configuration Management
**Decision**: Externalized configuration with Spring profiles.
**Rationale**:
- Environment-specific configuration
- No code changes between environments
- Secret management integration (Vault, K8s secrets)

## Related Documents
- [02_DEPENDENCY_SPECIFICATION.md](./02_DEPENDENCY_SPECIFICATION.md)
- [03_DATABASE_SCHEMA.md](./03_DATABASE_SCHEMA.md)
- [ADR-001: Build Tool Selection](./adr/ADR-001-build-tool-selection.md)
- [ADR-002: Module Organization](./adr/ADR-002-module-organization.md)
