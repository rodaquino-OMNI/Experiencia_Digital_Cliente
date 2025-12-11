# DEV Environment Process Execution Validation Guide

## 📋 Purpose
Validate BPMN process deployment and execution in Camunda Platform 7.20.0 DEV environment.

---

## 🔧 Prerequisites

### Software Requirements
- ✅ Camunda Platform 7.20.0 (Community or Enterprise)
- ✅ PostgreSQL 16 (or H2 for quick testing)
- ✅ Java 17+
- ✅ Spring Boot 3.2.1+
- ✅ Docker & Docker Compose (optional)

### Configuration Files Needed
- `application.yml` - Spring Boot configuration
- `docker-compose.yml` - Container orchestration (if using Docker)
- `pom.xml` or `build.gradle` - Build configuration

---

## 🚀 Option 1: Docker-Based DEV Environment (Recommended)

### Step 1: Create Docker Compose Configuration

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: camunda-postgres
    environment:
      POSTGRES_DB: camunda
      POSTGRES_USER: camunda
      POSTGRES_PASSWORD: camunda
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - camunda-network

  camunda:
    image: camunda/camunda-bpm-platform:7.20.0
    container_name: camunda-platform
    depends_on:
      - postgres
    environment:
      DB_DRIVER: org.postgresql.Driver
      DB_URL: jdbc:postgresql://postgres:5432/camunda
      DB_USERNAME: camunda
      DB_PASSWORD: camunda
      DB_VALIDATE_ON_BORROW: "true"
      DB_VALIDATION_QUERY: "SELECT 1"
      JAVA_OPTS: "-Xmx768m -XX:MaxMetaspaceSize=256m"
    ports:
      - "8080:8080"
    volumes:
      - ./src/bpmn:/camunda/configuration/resources
    networks:
      - camunda-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/engine-rest/engine"]
      interval: 30s
      timeout: 10s
      retries: 5

volumes:
  postgres_data:

networks:
  camunda-network:
    driver: bridge
```

### Step 2: Launch Environment

```bash
# Start services
docker-compose up -d

# Check logs
docker-compose logs -f camunda

# Wait for healthy status
docker-compose ps

# Access Camunda Cockpit
# URL: http://localhost:8080/camunda
# Default credentials: demo/demo
```

---

## 🚀 Option 2: Spring Boot Embedded Camunda

### Step 1: Add Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Camunda BPM Spring Boot Starter -->
    <dependency>
        <groupId>org.camunda.bpm.springboot</groupId>
        <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
        <version>7.20.0</version>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- H2 for testing (optional) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Step 2: Configure application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/camunda
    username: camunda
    password: camunda
    driver-class-name: org.postgresql.Driver
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false

camunda:
  bpm:
    admin-user:
      id: admin
      password: admin
      firstName: Admin
      lastName: User
    
    filter:
      create: All tasks
    
    authorization:
      enabled: true
    
    generic-properties:
      properties:
        history-level: FULL
        history-time-to-live: 365
    
    database:
      schema-update: true
      type: postgres
```

### Step 3: Create Spring Boot Application

```java
package com.operadora.experienciadigital;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableProcessApplication
public class ExperienciaDigitalApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExperienciaDigitalApplication.class, args);
    }
}
```

### Step 4: Launch Application

```bash
# Build
mvn clean package

# Run
java -jar target/experiencia-digital-0.0.1-SNAPSHOT.jar

# Or use Maven directly
mvn spring-boot:run
```

---

## 📊 Deployment Testing

### Test 1: Manual Deployment via Cockpit

1. **Access Camunda Cockpit**
   - URL: `http://localhost:8080/camunda`
   - Login: admin/admin (or demo/demo)

2. **Navigate to Deployments**
   - Click "Deployments" in top menu
   - Click "Create Deployment"

3. **Upload BPMN Files**
   - Select files in priority order:
     ```
     Priority 1 (Core Processes):
     - PROC-ORC-001_Orquestracao_Cuidado_Experiencia.bpmn
     - SUB-001_Onboarding_Inteligente.bpmn
     - SUB-002_Motor_Proativo.bpmn
     
     Priority 2 (Supporting Processes):
     - SUB-003_Recepcao_Classificacao.bpmn
     - SUB-004_Self_Service.bpmn
     - SUB-005_Agentes_IA.bpmn
     - SUB-006_Autorizacao_Inteligente.bpmn
     - SUB-007_Navegacao_Cuidado.bpmn
     - SUB-008_Gestao_Cronicos.bpmn
     - SUB-009_Gestao_Reclamacoes.bpmn
     - SUB-010_Follow_Up_Feedback.bpmn
     ```

4. **Deploy and Verify**
   - Click "Deploy"
   - Check for successful deployment message
   - Navigate to "Process Definitions"
   - Verify all 11 processes appear in list

### Test 2: REST API Deployment

```bash
# Deploy single file
curl -X POST http://localhost:8080/engine-rest/deployment/create \
  -H "Content-Type: multipart/form-data" \
  -F "deployment-name=Experiencia Digital Cliente" \
  -F "enable-duplicate-filtering=true" \
  -F "deploy-changed-only=true" \
  -F "data=@src/bpmn/PROC-ORC-001_Orquestracao_Cuidado_Experiencia.bpmn"

# Deploy all files at once
curl -X POST http://localhost:8080/engine-rest/deployment/create \
  -H "Content-Type: multipart/form-data" \
  -F "deployment-name=Experiencia Digital Cliente - Full Suite" \
  -F "enable-duplicate-filtering=true" \
  -F "deploy-changed-only=true" \
  $(for f in src/bpmn/*.bpmn; do echo "-F data=@$f"; done)
```

### Test 3: Programmatic Deployment (Spring Boot)

```java
@Component
public class ProcessDeployer implements CommandLineRunner {
    
    @Autowired
    private RepositoryService repositoryService;
    
    @Override
    public void run(String... args) {
        deployProcesses();
    }
    
    private void deployProcesses() {
        try {
            Deployment deployment = repositoryService.createDeployment()
                .name("Experiencia Digital Cliente")
                .addClasspathResource("bpmn/PROC-ORC-001_Orquestracao_Cuidado_Experiencia.bpmn")
                .addClasspathResource("bpmn/SUB-001_Onboarding_Inteligente.bpmn")
                // ... add all other files
                .enableDuplicateFiltering(true)
                .deploy();
            
            log.info("Deployed {} process definitions", 
                repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .count());
                    
        } catch (Exception e) {
            log.error("Deployment failed", e);
        }
    }
}
```

---

## 🧪 Process Execution Testing

### Test 1: Start Process Instance (Minimal Test)

**Objective:** Verify process can start without errors

**Steps:**

1. **Via Cockpit UI:**
   - Navigate to "Process Definitions"
   - Select "PROC-ORC-001_Orquestracao_Cuidado_Experiencia"
   - Click "Start Instance"
   - Provide minimal variables:
     ```json
     {
       "beneficiarioId": "TEST-001",
       "contratoId": "CONTRACT-001",
       "dadosCadastrais": {
         "nome": "Test User",
         "email": "test@example.com",
         "telefone": "+5511999999999"
       }
     }
     ```
   - Click "Start"

2. **Expected Behavior:**
   - ❌ Process will FAIL at first Service Task
   - ✅ This is EXPECTED (delegate beans not implemented yet)
   - ✅ Process instance created successfully
   - ✅ Check "Process Instances" to see active instance

3. **Via REST API:**
   ```bash
   curl -X POST http://localhost:8080/engine-rest/process-definition/key/PROC-ORC-001/start \
     -H "Content-Type: application/json" \
     -d '{
       "variables": {
         "beneficiarioId": {"value": "TEST-001", "type": "String"},
         "contratoId": {"value": "CONTRACT-001", "type": "String"}
       }
     }'
   ```

### Test 2: Verify Process Definition Metadata

```bash
# Get process definition
curl http://localhost:8080/engine-rest/process-definition/key/PROC-ORC-001

# Expected response fields to verify:
# - id: Process definition ID
# - key: PROC-ORC-001
# - name: Orquestração do Cuidado e Experiência do Beneficiário
# - version: 1 (or higher)
# - versionTag: 2.0
# - historyTimeToLive: 365
# - startableInTasklist: true
```

### Test 3: Validate BPMN XML Rendering

```bash
# Get BPMN XML
curl http://localhost:8080/engine-rest/process-definition/key/PROC-ORC-001/xml

# Verify response contains:
# - <bpmn:definitions> root element
# - <bpmn:process> section with all tasks
# - <bpmndi:BPMNDiagram> section with visual elements
# - All camunda: extensions intact
```

---

## 🔍 Validation Checklist

### Deployment Validation

- [ ] All 11 BPMN files deploy without errors
- [ ] Process definitions appear in Cockpit
- [ ] Version numbers increment correctly on redeploy
- [ ] Deployment history is tracked
- [ ] No XML parsing errors logged

### Process Definition Validation

For EACH of the 11 processes:

- [ ] Process ID matches expected value
- [ ] Process name is human-readable
- [ ] Version tag = "2.0"
- [ ] History time to live = 365 days
- [ ] Process is executable (isExecutable="true")
- [ ] Process appears in Tasklist start menu

### Business Logic Validation

**Expected Failures (OK at this stage):**

- [ ] ❌ Service Tasks fail with "Unknown property used in expression: ${...}"
  - **Cause:** Delegate beans not implemented yet
  - **Status:** EXPECTED - Will be fixed in Phase 3

- [ ] ❌ Business Rule Tasks fail with "DMN decision not found"
  - **Cause:** DMN files not created yet
  - **Status:** EXPECTED - Will be fixed in Phase 2

- [ ] ❌ External Tasks remain in topic queue
  - **Cause:** External task workers not running
  - **Status:** EXPECTED - Will be configured in Phase 3

**What Should Work:**

- [x] ✅ Process instance creation
- [x] ✅ Process definition storage in database
- [x] ✅ Visual diagram rendering in Cockpit
- [x] ✅ Navigation between process elements
- [x] ✅ Process variable initialization
- [x] ✅ Process instance query APIs

---

## 📊 Monitoring & Debugging

### Check Deployment Logs

```bash
# Docker
docker-compose logs camunda | grep -i "deploy"

# Spring Boot
tail -f logs/spring.log | grep -i "deploy"

# Look for:
# - "ENGINE-07015 Detected @ProcessApplication class"
# - "ENGINE-07021 ProcessApplication 'experiencia-digital' registered"
# - "ENGINE-07022 Deployment summary for process archive 'experiencia-digital'"
```

### Check Process Engine Metrics

```bash
# Get deployment count
curl http://localhost:8080/engine-rest/deployment/count

# Get process definition count
curl http://localhost:8080/engine-rest/process-definition/count

# Get process instance count
curl http://localhost:8080/engine-rest/process-instance/count

# Check engine health
curl http://localhost:8080/engine-rest/engine
```

### Database Queries

```sql
-- Check deployments
SELECT * FROM act_re_deployment 
ORDER BY deploy_time_ DESC 
LIMIT 10;

-- Check process definitions
SELECT id_, key_, name_, version_, version_tag_ 
FROM act_re_procdef 
ORDER BY version_ DESC;

-- Check active process instances
SELECT id_, proc_def_id_, start_time_, state_ 
FROM act_ru_execution 
WHERE parent_id_ IS NULL;

-- Check incidents (errors)
SELECT id_, proc_inst_id_, incident_msg_, create_time_ 
FROM act_ru_incident 
ORDER BY create_time_ DESC;
```

---

## ⚠️ Common Issues & Solutions

### Issue 1: Deployment Fails - "DMN decision not found"

**Symptom:**
```
ENGINE-DMN-09001 Unable to find decision with key 'DMN_EstratificacaoRisco'
```

**Solution:**
- EXPECTED - DMN files will be created in Phase 2
- To bypass for testing: Comment out Business Rule Tasks temporarily

### Issue 2: Process Fails - "Unknown property in expression"

**Symptom:**
```
ENGINE-03017 Cannot evaluate expression ${contextoService.inicializar}: 
Unknown property used in expression: contextoService
```

**Solution:**
- EXPECTED - Delegate beans will be implemented in Phase 3
- To bypass for testing: Create stub beans or use mock implementations

### Issue 3: External Tasks Not Picked Up

**Symptom:**
- External tasks created but never completed
- Tasks visible in Cockpit but workers not processing

**Solution:**
- EXPECTED - External task workers not configured yet
- Configure workers in Phase 3 or use REST API to complete manually:
  ```bash
  curl -X POST http://localhost:8080/engine-rest/external-task/{id}/complete \
    -H "Content-Type: application/json" \
    -d '{"workerId": "test-worker"}'
  ```

### Issue 4: PostgreSQL Connection Refused

**Symptom:**
```
java.sql.SQLException: Connection refused
```

**Solution:**
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check connection
psql -h localhost -U camunda -d camunda

# Restart if needed
docker-compose restart postgres
```

---

## ✅ Success Criteria

**Phase Passes If:**

1. ✅ All 11 BPMN files deploy successfully
2. ✅ All processes visible in Cockpit
3. ✅ Can create process instances (even if they fail at first task)
4. ✅ Visual diagrams render in Cockpit
5. ✅ Database contains all process definitions
6. ✅ No XML validation errors

**Expected Limitations:**

- ⚠️ Processes will fail execution due to missing delegates
- ⚠️ Business Rule Tasks will fail due to missing DMN files
- ⚠️ External Tasks will queue but not complete

**These are ACCEPTABLE and will be resolved in subsequent phases.**

---

## 📝 Test Results Template

```markdown
## DEV Environment Validation Results

**Date:** [DATE]
**Environment:** [Docker/Spring Boot]
**Camunda Version:** 7.20.0
**Database:** [PostgreSQL 16 / H2]
**Tester:** [NAME]

### Deployment Test Results

| File | Deploy Status | Process Definition ID | Version |
|------|---------------|----------------------|---------|
| PROC-ORC-001 | ✅ PASS | PROC-ORC-001:1:xxx | 1 |
| SUB-001 | ✅ PASS | SUB-001:1:xxx | 1 |
| ... | ... | ... | ... |

**Total Deployed:** X/11

### Execution Test Results

| Process | Instance Created | First Task | Status |
|---------|------------------|------------|--------|
| PROC-ORC-001 | ✅ YES | ❌ FAIL (expected) | OK |
| SUB-001 | ✅ YES | ❌ FAIL (expected) | OK |

### Issues Found

- [List any unexpected issues]
- [All expected failures noted above are OK]

### Overall Assessment: [✅ PASS / ❌ FAIL]

**Recommendation:** 
- [✅ Ready for Phase 2: DMN Implementation]
- [❌ Fix issues before proceeding]
```

---

## 🚀 Next Steps After Validation

**If Tests Pass:**
1. ✅ Mark deployment as successful
2. ✅ Begin Phase 2: DMN Decision Table Implementation
3. ✅ Begin Phase 3: Delegate Bean Implementation
4. ✅ Configure External Task Workers

**If Tests Fail:**
1. Review deployment logs
2. Check XML validation errors
3. Verify database connectivity
4. Fix issues and redeploy
5. Re-run validation suite

---

**Validation Guide Version:** 1.0  
**Created:** 2025-12-11  
**Target Environment:** Camunda Platform 7.20.0 DEV
