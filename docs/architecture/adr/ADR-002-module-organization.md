# ADR-002: Multi-Module Maven Project Organization

## Status
**Accepted** - 2025-12-11

## Context
The Camunda 7 BPM platform contains multiple logical components: domain models, BPMN delegates, business services, external integrations, and messaging. We need to decide between a monolithic single-module structure vs a multi-module Maven project.

## Decision Drivers
- **Separation of Concerns**: Clear boundaries between layers
- **Reusability**: Ability to reuse modules across applications
- **Build Performance**: Compile only changed modules
- **Dependency Management**: Control inter-module dependencies
- **Team Collaboration**: Reduce merge conflicts
- **Deployment Flexibility**: Deploy modules independently (future)

## Considered Options

### Option 1: Monolithic Single Module
**Structure**:
```
operadora-digital/
├── src/main/java/com/austa/operadora/
│   ├── domain/
│   ├── bpmn/
│   ├── services/
│   ├── integration/
│   └── messaging/
└── pom.xml
```

**Pros**:
- Simple initial setup
- Single JAR deployment
- Easier refactoring across layers

**Cons**:
- No enforced layer boundaries
- Circular dependencies risk
- Slower builds (recompile everything)
- Harder to isolate unit tests

### Option 2: Multi-Module Maven Project
**Structure**:
```
operadora-digital-parent/
├── operadora-domain/           # Domain entities & value objects
├── operadora-bpmn-delegates/   # BPMN service task delegates
├── operadora-services/         # Business logic services
├── operadora-integration/      # External system connectors
├── operadora-messaging/        # Kafka producers/consumers
├── operadora-webapp/           # Spring Boot application
└── pom.xml (parent)
```

**Pros**:
- Clear dependency graph (domain → services → bpmn → webapp)
- Reusable modules (domain can be shared with other apps)
- Faster incremental builds
- Enforced architectural boundaries
- Independent versioning per module

**Cons**:
- More complex initial setup
- Requires Maven expertise
- More POMs to maintain

## Decision
**Selected: Multi-Module Maven Project**

## Module Dependency Graph
```
operadora-webapp
    ↓
operadora-bpmn-delegates
    ↓
operadora-services
    ↓           ↓
operadora-integration  operadora-messaging
    ↓           ↓
operadora-domain
```

### Module Responsibilities

| Module | Purpose | Dependencies |
|--------|---------|--------------|
| `operadora-domain` | Entities, VOs, enums | None (pure domain) |
| `operadora-integration` | Tasy, WhatsApp, RPA clients | domain |
| `operadora-messaging` | Kafka producers/consumers | domain |
| `operadora-services` | Business logic services | domain, integration, messaging |
| `operadora-bpmn-delegates` | Camunda service tasks | domain, services |
| `operadora-webapp` | Spring Boot app + Camunda | All modules |

## Rationale

1. **Architectural Clarity**: Multi-module structure enforces hexagonal architecture boundaries. Domain layer has zero external dependencies.

2. **Build Performance**: Maven reactor builds only changed modules. Domain changes trigger full rebuild, but integration changes only rebuild dependent modules.

3. **Testability**: Each module has isolated unit tests. Domain tests run in milliseconds without Spring context.

4. **Reusability**: Domain and services modules can be reused in:
   - Batch processing applications
   - External task workers (separate deployment)
   - Migration scripts

5. **Team Collaboration**: Different teams can work on different modules with minimal merge conflicts.

6. **Future Microservices**: If we decide to extract microservices later, modules provide natural boundaries.

## Implementation Guidelines

### Parent POM Configuration
```xml
<modules>
    <module>operadora-domain</module>
    <module>operadora-integration</module>
    <module>operadora-messaging</module>
    <module>operadora-services</module>
    <module>operadora-bpmn-delegates</module>
    <module>operadora-webapp</module>
</modules>
```

### Dependency Management in Parent
- Define versions for all dependencies in `<dependencyManagement>`
- Child modules inherit versions without specifying them
- Enforces consistent library versions across modules

### Module-Specific POMs
- Only declare `<dependencies>` without versions
- Avoid duplicate dependency declarations
- Use Maven Enforcer Plugin to prevent circular dependencies

## Consequences

### Positive
- Clean architecture with enforced boundaries
- Faster incremental builds after initial setup
- Isolated testing per module
- Reusable components for future applications
- Easier to reason about dependencies

### Negative
- More complex project structure
- Requires understanding Maven reactor builds
- More POMs to maintain (6 total)
- Slightly longer initial setup time

## Migration Path
If project complexity proves unnecessary:
1. Merge modules into single module
2. Preserve package structure from modules
3. Convert to Gradle for better monorepo support

## Related Decisions
- ADR-001: Build Tool Selection
- ADR-004: Camunda 7 Deployment Model
- ADR-006: Testing Strategy

## References
- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
