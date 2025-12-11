# Database Schema Design - PostgreSQL for Camunda 7

## Document Control
- **Version**: 1.0
- **Author**: System Architecture Designer
- **Date**: 2025-12-11
- **Status**: Draft

## Overview
This document defines the complete PostgreSQL database schema for the Camunda 7 BPM implementation, including:
- Camunda engine tables (managed by Camunda)
- Application business tables
- Process variable persistence
- Audit and compliance tables
- Reporting views

## Schema Organization

### Schema Namespaces
```sql
CREATE SCHEMA IF NOT EXISTS camunda;      -- Camunda engine tables
CREATE SCHEMA IF NOT EXISTS operadora;    -- Business domain tables
CREATE SCHEMA IF NOT EXISTS audit;        -- Audit and compliance
CREATE SCHEMA IF NOT EXISTS reporting;    -- Read-optimized views
```

## 1. Business Domain Schema (`operadora`)

### 1.1 Beneficiary Management

```sql
-- Beneficiaries (Plan Members)
CREATE TABLE operadora.beneficiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(50) UNIQUE NOT NULL,           -- Tasy ERP ID
    cpf VARCHAR(11) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender CHAR(1) CHECK (gender IN ('M', 'F', 'O')),
    email VARCHAR(100),
    phone VARCHAR(20),
    whatsapp VARCHAR(20),

    -- Address
    address_street VARCHAR(200),
    address_number VARCHAR(20),
    address_complement VARCHAR(100),
    address_neighborhood VARCHAR(100),
    address_city VARCHAR(100),
    address_state CHAR(2),
    address_zip_code VARCHAR(9),

    -- Contract Information
    contract_number VARCHAR(50) NOT NULL,
    plan_code VARCHAR(20) NOT NULL,
    contract_status VARCHAR(20) NOT NULL
        CHECK (contract_status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    admission_date DATE NOT NULL,
    suspension_date DATE,
    cancellation_date DATE,

    -- Journey State
    journey_state VARCHAR(30) NOT NULL
        CHECK (journey_state IN (
            'NEW', 'ONBOARDING', 'ACTIVE', 'MONITORED',
            'IN_SERVICE', 'IN_CARE_JOURNEY', 'IN_CHRONIC_PROGRAM',
            'IN_INTENSIVE_CARE', 'COMPLETED', 'INACTIVE'
        )),
    journey_state_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Indexes
    CONSTRAINT chk_beneficiary_cpf CHECK (cpf ~ '^[0-9]{11}$')
);

CREATE INDEX idx_beneficiaries_cpf ON operadora.beneficiaries(cpf);
CREATE INDEX idx_beneficiaries_external_id ON operadora.beneficiaries(external_id);
CREATE INDEX idx_beneficiaries_journey_state ON operadora.beneficiaries(journey_state);
CREATE INDEX idx_beneficiaries_contract_status ON operadora.beneficiaries(contract_status);
```

### 1.2 Health Profile & Risk Stratification

```sql
-- Health Profiles
CREATE TABLE operadora.health_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id UUID NOT NULL REFERENCES operadora.beneficiaries(id),

    -- Physical Metrics
    height_cm NUMERIC(5,2),
    weight_kg NUMERIC(5,2),
    bmi NUMERIC(5,2) GENERATED ALWAYS AS (
        CASE
            WHEN height_cm > 0 THEN weight_kg / ((height_cm/100) * (height_cm/100))
            ELSE NULL
        END
    ) STORED,
    blood_type VARCHAR(5),

    -- Lifestyle
    smoker BOOLEAN,
    alcohol_consumption VARCHAR(20)
        CHECK (alcohol_consumption IN ('NONE', 'OCCASIONAL', 'MODERATE', 'HEAVY')),
    physical_activity VARCHAR(20)
        CHECK (physical_activity IN ('SEDENTARY', 'LIGHT', 'MODERATE', 'INTENSE')),

    -- Medical History (JSONB for flexibility)
    chronic_conditions JSONB,              -- Array of condition codes
    allergies JSONB,                       -- Array of allergen codes
    medications JSONB,                     -- Array of current medications
    family_history JSONB,                  -- Family medical history

    -- Pre-existing Conditions (CPT)
    has_preexisting BOOLEAN DEFAULT FALSE,
    preexisting_conditions JSONB,
    preexisting_declared BOOLEAN,
    preexisting_detected_by_ml BOOLEAN,
    preexisting_confirmed_date DATE,

    -- Metadata
    profile_complete BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_source VARCHAR(50),               -- 'SCREENING', 'MEDICAL_RECORD', 'SELF_REPORTED'

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_health_profiles_beneficiary ON operadora.health_profiles(beneficiary_id);
CREATE INDEX idx_health_profiles_preexisting ON operadora.health_profiles(has_preexisting);

-- Risk Stratification
CREATE TABLE operadora.risk_stratifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id UUID NOT NULL REFERENCES operadora.beneficiaries(id),

    -- Risk Assessment
    risk_level VARCHAR(20) NOT NULL
        CHECK (risk_level IN ('LOW', 'MODERATE', 'HIGH', 'COMPLEX')),
    risk_score NUMERIC(5,2) NOT NULL CHECK (risk_score BETWEEN 0 AND 100),

    -- Contributing Factors
    clinical_score NUMERIC(5,2),
    behavioral_score NUMERIC(5,2),
    utilization_score NUMERIC(5,2),
    predictive_score NUMERIC(5,2),

    -- Risk Factors (JSONB for ML model output)
    risk_factors JSONB,

    -- Model Information
    model_version VARCHAR(20) NOT NULL,
    model_algorithm VARCHAR(50),           -- 'XGBOOST', 'RANDOM_FOREST', etc.
    confidence_level NUMERIC(5,2),

    -- Effective Period
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    is_current BOOLEAN DEFAULT TRUE,

    -- Metadata
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    calculated_by VARCHAR(100),            -- User or system that triggered

    CONSTRAINT chk_valid_period CHECK (valid_until > valid_from)
);

CREATE INDEX idx_risk_stratifications_beneficiary ON operadora.risk_stratifications(beneficiary_id);
CREATE INDEX idx_risk_stratifications_current ON operadora.risk_stratifications(beneficiary_id, is_current)
    WHERE is_current = TRUE;
CREATE INDEX idx_risk_stratifications_level ON operadora.risk_stratifications(risk_level);
```

### 1.3 Care Plans & Programs

```sql
-- Care Plans
CREATE TABLE operadora.care_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id UUID NOT NULL REFERENCES operadora.beneficiaries(id),

    -- Plan Details
    plan_type VARCHAR(50) NOT NULL
        CHECK (plan_type IN ('PREVENTIVE', 'CHRONIC_DISEASE', 'CARE_TRANSITION', 'INTENSIVE_CARE')),
    plan_name VARCHAR(200) NOT NULL,
    plan_description TEXT,

    -- Status
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('DRAFT', 'ACTIVE', 'SUSPENDED', 'COMPLETED', 'CANCELLED')),

    -- Period
    start_date DATE NOT NULL,
    end_date DATE,

    -- Goals & Metrics (JSONB for flexibility)
    goals JSONB,                           -- Array of clinical goals
    interventions JSONB,                   -- Planned interventions

    -- Assignment
    assigned_navigator_id UUID,
    assigned_physician_id VARCHAR(50),     -- External ID from Tasy

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_care_plans_beneficiary ON operadora.care_plans(beneficiary_id);
CREATE INDEX idx_care_plans_status ON operadora.care_plans(status);
CREATE INDEX idx_care_plans_assigned_navigator ON operadora.care_plans(assigned_navigator_id);

-- Chronic Disease Programs Enrollment
CREATE TABLE operadora.chronic_program_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id UUID NOT NULL REFERENCES operadora.beneficiaries(id),
    care_plan_id UUID REFERENCES operadora.care_plans(id),

    -- Program Details
    program_code VARCHAR(50) NOT NULL,     -- 'DIABETES_MGMT', 'HYPERTENSION', 'COPD', etc.
    program_name VARCHAR(200) NOT NULL,
    disease_codes JSONB,                   -- ICD-10 codes

    -- Enrollment
    enrollment_date DATE NOT NULL,
    discharge_date DATE,
    enrollment_status VARCHAR(20) NOT NULL
        CHECK (enrollment_status IN ('ENROLLED', 'ACTIVE', 'PAUSED', 'DISCHARGED')),
    discharge_reason VARCHAR(100),

    -- Protocol Adherence
    protocol_version VARCHAR(20),
    adherence_score NUMERIC(5,2),
    last_contact_date DATE,
    next_contact_date DATE,
    contact_frequency VARCHAR(20),         -- 'DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY'

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chronic_enrollments_beneficiary ON operadora.chronic_program_enrollments(beneficiary_id);
CREATE INDEX idx_chronic_enrollments_program ON operadora.chronic_program_enrollments(program_code);
CREATE INDEX idx_chronic_enrollments_status ON operadora.chronic_program_enrollments(enrollment_status);
```

### 1.4 Interactions & Communications

```sql
-- Interactions (All touchpoints with beneficiary)
CREATE TABLE operadora.interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id UUID NOT NULL REFERENCES operadora.beneficiaries(id),

    -- Interaction Details
    interaction_type VARCHAR(50) NOT NULL
        CHECK (interaction_type IN (
            'INBOUND_CALL', 'OUTBOUND_CALL', 'WHATSAPP', 'EMAIL',
            'SMS', 'APP', 'PORTAL', 'IN_PERSON'
        )),
    interaction_category VARCHAR(50) NOT NULL
        CHECK (interaction_category IN (
            'INFORMATION', 'AUTHORIZATION', 'SCHEDULING', 'COMPLAINT',
            'NAVIGATION', 'FOLLOW_UP', 'EMERGENCY', 'ADMINISTRATIVE'
        )),

    -- Channel & Source
    channel VARCHAR(20) NOT NULL,
    source_system VARCHAR(50),

    -- Content
    subject VARCHAR(500),
    summary TEXT,
    full_content JSONB,                    -- Structured interaction data

    -- Classification
    urgency_level VARCHAR(20)
        CHECK (urgency_level IN ('LOW', 'MEDIUM', 'HIGH', 'EMERGENCY')),
    complexity_level VARCHAR(20)
        CHECK (complexity_level IN ('SIMPLE', 'MODERATE', 'COMPLEX')),
    sentiment VARCHAR(20)                  -- NLP-derived sentiment
        CHECK (sentiment IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE', 'VERY_NEGATIVE')),

    -- Resolution
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('RECEIVED', 'IN_PROGRESS', 'RESOLVED', 'ESCALATED', 'CLOSED')),
    resolution_type VARCHAR(50),
    first_contact_resolution BOOLEAN,

    -- Timing
    received_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP,
    resolved_at TIMESTAMP,
    response_time_seconds INTEGER,
    resolution_time_seconds INTEGER,

    -- Assignment
    assigned_to VARCHAR(100),
    assigned_team VARCHAR(50),

    -- Process Correlation
    process_instance_id VARCHAR(64),       -- Camunda process instance

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interactions_beneficiary ON operadora.interactions(beneficiary_id);
CREATE INDEX idx_interactions_type ON operadora.interactions(interaction_type);
CREATE INDEX idx_interactions_category ON operadora.interactions(interaction_category);
CREATE INDEX idx_interactions_status ON operadora.interactions(status);
CREATE INDEX idx_interactions_received_at ON operadora.interactions(received_at DESC);
CREATE INDEX idx_interactions_process_instance ON operadora.interactions(process_instance_id);
```

### 1.5 Authorizations

```sql
-- Authorization Requests
CREATE TABLE operadora.authorization_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id UUID NOT NULL REFERENCES operadora.beneficiaries(id),

    -- Request Details
    request_number VARCHAR(50) UNIQUE NOT NULL,
    request_type VARCHAR(50) NOT NULL
        CHECK (request_type IN ('CONSULTATION', 'EXAM', 'PROCEDURE', 'HOSPITALIZATION', 'MEDICATION')),

    -- Clinical Information
    procedure_code VARCHAR(20),            -- TUSS code
    procedure_name VARCHAR(500),
    icd10_code VARCHAR(10),
    clinical_indication TEXT,

    -- Requesting Physician
    requesting_physician_id VARCHAR(50),
    requesting_physician_name VARCHAR(200),
    requesting_physician_crm VARCHAR(20),

    -- Execution Provider
    provider_id VARCHAR(50),
    provider_name VARCHAR(200),

    -- Status & Decision
    status VARCHAR(20) NOT NULL
        CHECK (status IN (
            'RECEIVED', 'UNDER_REVIEW', 'PENDING_DOCS', 'APPROVED',
            'PARTIALLY_APPROVED', 'DENIED', 'CANCELLED', 'EXPIRED'
        )),
    decision_type VARCHAR(20)
        CHECK (decision_type IN ('AUTOMATIC', 'MANUAL', 'MEDICAL_REVIEW')),
    decision_reason TEXT,
    denial_reason_code VARCHAR(50),

    -- Authorization Details
    authorized_quantity INTEGER,
    authorized_amount NUMERIC(10,2),
    authorization_code VARCHAR(50),
    valid_from DATE,
    valid_until DATE,

    -- Timing (SLA tracking)
    requested_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    decided_at TIMESTAMP,
    processing_time_seconds INTEGER,

    -- Process Correlation
    process_instance_id VARCHAR(64),

    -- Audit
    decided_by VARCHAR(100),
    review_notes TEXT,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_authorizations_beneficiary ON operadora.authorization_requests(beneficiary_id);
CREATE INDEX idx_authorizations_request_number ON operadora.authorization_requests(request_number);
CREATE INDEX idx_authorizations_status ON operadora.authorization_requests(status);
CREATE INDEX idx_authorizations_requested_at ON operadora.authorization_requests(requested_at DESC);
CREATE INDEX idx_authorizations_process_instance ON operadora.authorization_requests(process_instance_id);
```

## 2. Process Variable Persistence Schema

### 2.1 Custom Process Variables Table

```sql
-- Extended Process Variables (beyond Camunda's standard storage)
CREATE TABLE operadora.process_variables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Process Correlation
    process_definition_key VARCHAR(64) NOT NULL,
    process_instance_id VARCHAR(64) NOT NULL,
    execution_id VARCHAR(64),
    task_id VARCHAR(64),

    -- Variable Details
    variable_name VARCHAR(255) NOT NULL,
    variable_type VARCHAR(50) NOT NULL,    -- 'STRING', 'INTEGER', 'BOOLEAN', 'JSON', 'DATE'

    -- Values (use appropriate column based on type)
    string_value TEXT,
    long_value BIGINT,
    double_value DOUBLE PRECISION,
    boolean_value BOOLEAN,
    date_value TIMESTAMP,
    json_value JSONB,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Composite unique constraint
    CONSTRAINT uq_process_variable UNIQUE (process_instance_id, variable_name)
);

CREATE INDEX idx_process_vars_instance ON operadora.process_variables(process_instance_id);
CREATE INDEX idx_process_vars_definition ON operadora.process_variables(process_definition_key);
CREATE INDEX idx_process_vars_name ON operadora.process_variables(variable_name);
CREATE INDEX idx_process_vars_json ON operadora.process_variables USING gin(json_value);
```

## 3. Audit & Compliance Schema

### 3.1 Audit Trail

```sql
-- Comprehensive Audit Log
CREATE TABLE audit.audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Entity Information
    entity_type VARCHAR(100) NOT NULL,     -- Table name
    entity_id VARCHAR(100) NOT NULL,

    -- Action
    action VARCHAR(20) NOT NULL
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'READ', 'EXECUTE')),

    -- Changes (for UPDATE actions)
    old_values JSONB,
    new_values JSONB,
    changed_fields JSONB,                  -- Array of field names

    -- Context
    user_id VARCHAR(100),
    user_name VARCHAR(200),
    user_role VARCHAR(50),

    -- Process Context
    process_instance_id VARCHAR(64),
    activity_id VARCHAR(100),

    -- Request Context
    ip_address INET,
    user_agent TEXT,
    session_id VARCHAR(100),

    -- Compliance
    gdpr_reason VARCHAR(200),              -- LGPD/GDPR justification

    -- Timestamp
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Indexes for performance
    CONSTRAINT chk_audit_values CHECK (
        (action = 'UPDATE' AND old_values IS NOT NULL AND new_values IS NOT NULL) OR
        (action != 'UPDATE')
    )
);

CREATE INDEX idx_audit_log_entity ON audit.audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_occurred_at ON audit.audit_log(occurred_at DESC);
CREATE INDEX idx_audit_log_user ON audit.audit_log(user_id);
CREATE INDEX idx_audit_log_process ON audit.audit_log(process_instance_id);

-- LGPD Consent Management
CREATE TABLE audit.consent_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id UUID NOT NULL REFERENCES operadora.beneficiaries(id),

    -- Consent Details
    consent_type VARCHAR(50) NOT NULL
        CHECK (consent_type IN (
            'DATA_PROCESSING', 'COMMUNICATION', 'MARKETING',
            'RESEARCH', 'THIRD_PARTY_SHARING'
        )),
    consent_given BOOLEAN NOT NULL,

    -- Legal Basis
    legal_basis VARCHAR(100),
    purpose TEXT NOT NULL,

    -- Period
    granted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,

    -- Evidence
    consent_mechanism VARCHAR(50),         -- 'DIGITAL_SIGNATURE', 'CHECKBOX', 'VERBAL', etc.
    evidence_document_url TEXT,

    -- Metadata
    ip_address INET,
    user_agent TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consent_beneficiary ON audit.consent_records(beneficiary_id);
CREATE INDEX idx_consent_type ON audit.consent_records(consent_type);
CREATE INDEX idx_consent_active ON audit.consent_records(beneficiary_id, consent_type)
    WHERE consent_given = TRUE AND revoked_at IS NULL;
```

## 4. Reporting Views Schema

### 4.1 Operational Dashboards

```sql
-- Active Care Journeys Summary
CREATE MATERIALIZED VIEW reporting.mv_active_care_journeys AS
SELECT
    b.id AS beneficiary_id,
    b.full_name,
    b.journey_state,
    rs.risk_level,
    rs.risk_score,
    cp.plan_type,
    cp.assigned_navigator_id,
    COUNT(i.id) AS total_interactions_30d,
    MAX(i.received_at) AS last_interaction_date,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - b.journey_state_updated_at))/86400 AS days_in_current_state
FROM operadora.beneficiaries b
LEFT JOIN operadora.risk_stratifications rs ON b.id = rs.beneficiary_id AND rs.is_current = TRUE
LEFT JOIN operadora.care_plans cp ON b.id = cp.beneficiary_id AND cp.status = 'ACTIVE'
LEFT JOIN operadora.interactions i ON b.id = i.beneficiary_id
    AND i.received_at > CURRENT_DATE - INTERVAL '30 days'
WHERE b.journey_state IN ('MONITORED', 'IN_CARE_JOURNEY', 'IN_CHRONIC_PROGRAM', 'IN_INTENSIVE_CARE')
GROUP BY b.id, b.full_name, b.journey_state, rs.risk_level, rs.risk_score,
         cp.plan_type, cp.assigned_navigator_id, b.journey_state_updated_at;

CREATE UNIQUE INDEX idx_mv_care_journeys_beneficiary ON reporting.mv_active_care_journeys(beneficiary_id);

-- Authorization Performance Metrics
CREATE MATERIALIZED VIEW reporting.mv_authorization_metrics AS
SELECT
    DATE_TRUNC('day', requested_at) AS request_date,
    status,
    decision_type,
    COUNT(*) AS total_requests,
    AVG(processing_time_seconds) AS avg_processing_time,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY processing_time_seconds) AS median_processing_time,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY processing_time_seconds) AS p95_processing_time,
    SUM(CASE WHEN processing_time_seconds <= 300 THEN 1 ELSE 0 END)::DECIMAL / COUNT(*) AS pct_within_5min
FROM operadora.authorization_requests
WHERE requested_at > CURRENT_DATE - INTERVAL '90 days'
GROUP BY DATE_TRUNC('day', requested_at), status, decision_type;

CREATE INDEX idx_mv_auth_metrics_date ON reporting.mv_authorization_metrics(request_date DESC);
```

## 5. Database Configuration

### 5.1 Connection Pool Settings (HikariCP)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### 5.2 Flyway Migration Configuration
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    schemas: camunda,operadora,audit,reporting
```

## Related Documents
- [01_PROJECT_STRUCTURE.md](./01_PROJECT_STRUCTURE.md)
- [02_DEPENDENCY_SPECIFICATION.md](./02_DEPENDENCY_SPECIFICATION.md)
- [04_KAFKA_ARCHITECTURE.md](./04_KAFKA_ARCHITECTURE.md)
