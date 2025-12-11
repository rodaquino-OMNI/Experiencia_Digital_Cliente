# Deployment Strategy - Docker & Kubernetes Configuration

## Document Control
- **Version**: 1.0
- **Author**: System Architecture Designer
- **Date**: 2025-12-11
- **Status**: Draft

## Overview
This document defines the deployment strategy for the Camunda 7 BPM platform, including Docker containerization, Kubernetes orchestration, and environment-specific configurations.

## Deployment Architecture

### High-Level Architecture
```
┌─────────────────────────────────────────────────────────────────────────┐
│                          KUBERNETES CLUSTER                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────┐     ┌──────────────────┐     ┌─────────────────┐│
│  │   Ingress        │────►│  Service Mesh    │────►│  Application    ││
│  │   (NGINX)        │     │   (Istio)        │     │  Namespace      ││
│  └──────────────────┘     └──────────────────┘     └────────┬────────┘│
│                                                               │         │
│  ┌───────────────────────────────────────────────────────────┼────────┐│
│  │ Application Namespace                                     │        ││
│  │                                                           │        ││
│  │  ┌─────────────────────────────────────────────────────┐ │        ││
│  │  │ Camunda BPM Deployment (3 replicas)                 │ │        ││
│  │  │ - Spring Boot Application                           │ │        ││
│  │  │ - Embedded Tomcat                                   │ │        ││
│  │  │ - Camunda Cockpit, Tasklist, Admin                  │ │        ││
│  │  └─────────────────────────────────────────────────────┘ │        ││
│  │                           │                               │        ││
│  │  ┌─────────────────────────────────────────────────────┐ │        ││
│  │  │ External Task Workers (Horizontal Autoscale)        │ │        ││
│  │  │ - Async service task execution                      │ │        ││
│  │  └─────────────────────────────────────────────────────┘ │        ││
│  │                           │                               │        ││
│  └───────────────────────────┼───────────────────────────────┘        ││
│                              │                                         │
│  ┌───────────────────────────┼───────────────────────────────────────┐│
│  │ Data Layer Namespace      │                                       ││
│  │                           │                                       ││
│  │  ┌───────────────────┐    │    ┌────────────────┐   ┌──────────┐││
│  │  │  PostgreSQL       │◄───┴───►│  Redis Cache   │   │  Kafka   │││
│  │  │  StatefulSet      │          │  StatefulSet   │   │  Cluster │││
│  │  │  (3 replicas)     │          │  (3 replicas)  │   │(3 brokers│││
│  │  └───────────────────┘          └────────────────┘   └──────────┘││
│  └───────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │ Monitoring Namespace                                              │ │
│  │  - Prometheus     - Grafana     - Jaeger     - ELK Stack         │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Docker Configuration

### Multi-Stage Dockerfile
```dockerfile
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom files first for dependency caching
COPY pom.xml .
COPY operadora-domain/pom.xml operadora-domain/
COPY operadora-bpmn-delegates/pom.xml operadora-bpmn-delegates/
COPY operadora-services/pom.xml operadora-services/
COPY operadora-integration/pom.xml operadora-integration/
COPY operadora-messaging/pom.xml operadora-messaging/
COPY operadora-webapp/pom.xml operadora-webapp/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY operadora-domain/src operadora-domain/src
COPY operadora-bpmn-delegates/src operadora-bpmn-delegates/src
COPY operadora-services/src operadora-services/src
COPY operadora-integration/src operadora-integration/src
COPY operadora-messaging/src operadora-messaging/src
COPY operadora-webapp/src operadora-webapp/src

# Build application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Install utilities
RUN apk add --no-cache curl bash

# Create non-root user
RUN addgroup -g 1000 camunda && \
    adduser -D -u 1000 -G camunda camunda

WORKDIR /app

# Copy JAR from build stage
COPY --from=build --chown=camunda:camunda \
    /app/operadora-webapp/target/operadora-digital-*.jar \
    /app/application.jar

# Health check script
COPY --chown=camunda:camunda docker/healthcheck.sh /app/
RUN chmod +x /app/healthcheck.sh

# Switch to non-root user
USER camunda

# Expose ports
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heap-dump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Application entrypoint
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/application.jar"]
```

### Health Check Script (`docker/healthcheck.sh`)
```bash
#!/bin/bash
set -e

# Check if application is responding
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "Health check passed"
    exit 0
else
    echo "Health check failed with HTTP code: $HTTP_CODE"
    exit 1
fi
```

### Docker Compose (Local Development)
```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: operadora-postgres
    environment:
      POSTGRES_DB: operadora
      POSTGRES_USER: operadora_user
      POSTGRES_PASSWORD: operadora_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./db/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U operadora_user -d operadora"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: operadora-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Kafka (with KRaft - no Zookeeper)
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: operadora-kafka
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092'
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://kafka:29092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:9092'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'
    volumes:
      - kafka-data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 10s
      retries: 5

  # Camunda Application
  camunda-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: operadora-camunda
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      # Spring Profile
      SPRING_PROFILES_ACTIVE: dev

      # Database
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/operadora
      SPRING_DATASOURCE_USERNAME: operadora_user
      SPRING_DATASOURCE_PASSWORD: operadora_pass

      # Redis
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379

      # Kafka
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092

      # Camunda
      CAMUNDA_BPM_ADMIN_USER_ID: admin
      CAMUNDA_BPM_ADMIN_USER_PASSWORD: admin

      # JVM Options
      JAVA_OPTS: >-
        -Xms512m
        -Xmx1024m
        -XX:+UseG1GC
    volumes:
      - app-logs:/app/logs
    healthcheck:
      test: ["CMD", "/app/healthcheck.sh"]
      interval: 30s
      timeout: 10s
      start_period: 60s
      retries: 3

  # Prometheus (Metrics)
  prometheus:
    image: prom/prometheus:latest
    container_name: operadora-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  # Grafana (Dashboards)
  grafana:
    image: grafana/grafana:latest
    container_name: operadora-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources

volumes:
  postgres-data:
  redis-data:
  kafka-data:
  app-logs:
  prometheus-data:
  grafana-data:
```

## Kubernetes Configuration

### Namespace Definition
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: operadora-digital
  labels:
    name: operadora-digital
    environment: production
```

### ConfigMap for Application Configuration
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: camunda-config
  namespace: operadora-digital
data:
  application.yml: |
    spring:
      application:
        name: operadora-digital
      profiles:
        active: ${SPRING_PROFILES_ACTIVE:prod}

    camunda:
      bpm:
        admin-user:
          id: ${CAMUNDA_ADMIN_USER:admin}
          password: ${CAMUNDA_ADMIN_PASSWORD}
        job-execution:
          enabled: true
          deployment-aware: true
        history-level: full
        history-time-to-live: 365

    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      metrics:
        export:
          prometheus:
            enabled: true
```

### Secret for Sensitive Data
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: camunda-secrets
  namespace: operadora-digital
type: Opaque
stringData:
  database-url: jdbc:postgresql://postgres-service:5432/operadora
  database-username: operadora_user
  database-password: CHANGE_ME_IN_PRODUCTION
  kafka-bootstrap-servers: kafka-service:9092
  redis-password: CHANGE_ME_IN_PRODUCTION
```

### Deployment for Camunda Application
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: camunda-app
  namespace: operadora-digital
  labels:
    app: camunda-app
    version: v1.0.0
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: camunda-app
  template:
    metadata:
      labels:
        app: camunda-app
        version: v1.0.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: camunda-app-sa

      # Init container to wait for database
      initContainers:
      - name: wait-for-postgres
        image: busybox:1.36
        command: ['sh', '-c',
          'until nc -z postgres-service 5432; do echo waiting for postgres; sleep 2; done;']

      containers:
      - name: camunda-app
        image: operadora-digital:1.0.0
        imagePullPolicy: Always

        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        - name: management
          containerPort: 8081
          protocol: TCP

        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: camunda-secrets
              key: database-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: camunda-secrets
              key: database-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: camunda-secrets
              key: database-password
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          valueFrom:
            secretKeyRef:
              name: camunda-secrets
              key: kafka-bootstrap-servers

        # Resource limits
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"

        # Liveness probe
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 90
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3

        # Readiness probe
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3

        # Startup probe (for slow starts)
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 12

        # Volume mounts
        volumeMounts:
        - name: config
          mountPath: /app/config
        - name: logs
          mountPath: /app/logs

      volumes:
      - name: config
        configMap:
          name: camunda-config
      - name: logs
        emptyDir: {}

      # Affinity rules
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - camunda-app
              topologyKey: kubernetes.io/hostname
```

### Service Definition
```yaml
apiVersion: v1
kind: Service
metadata:
  name: camunda-service
  namespace: operadora-digital
  labels:
    app: camunda-app
spec:
  type: ClusterIP
  selector:
    app: camunda-app
  ports:
  - name: http
    port: 80
    targetPort: 8080
    protocol: TCP
  - name: management
    port: 8081
    targetPort: 8081
    protocol: TCP
  sessionAffinity: ClientIP
```

### Horizontal Pod Autoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: camunda-app-hpa
  namespace: operadora-digital
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: camunda-app
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 25
        periodSeconds: 60
```

### Ingress Configuration
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: camunda-ingress
  namespace: operadora-digital
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "30"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
spec:
  tls:
  - hosts:
    - camunda.operadora.austa.com.br
    secretName: camunda-tls
  rules:
  - host: camunda.operadora.austa.com.br
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: camunda-service
            port:
              number: 80
```

## Environment-Specific Configurations

### Development Environment
- **Replicas**: 1
- **Resources**: Minimal (512Mi RAM, 0.5 CPU)
- **Database**: Single instance
- **Cache**: Optional
- **Monitoring**: Basic

### Staging Environment
- **Replicas**: 2
- **Resources**: Medium (1Gi RAM, 1 CPU)
- **Database**: Master-replica setup
- **Cache**: Redis cluster (3 nodes)
- **Monitoring**: Full stack

### Production Environment
- **Replicas**: 3-10 (autoscaling)
- **Resources**: High (2Gi RAM, 2 CPU)
- **Database**: High availability (3+ nodes)
- **Cache**: Redis cluster (3+ nodes)
- **Monitoring**: Full stack + alerting

## CI/CD Integration

### GitLab CI Pipeline (`.gitlab-ci.yml`)
```yaml
stages:
  - build
  - test
  - package
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  DOCKER_REGISTRY: registry.operadora.austa.com.br
  IMAGE_NAME: operadora-digital

build:
  stage: build
  image: maven:3.9.6-eclipse-temurin-17
  script:
    - mvn clean compile
  cache:
    paths:
      - .m2/repository
  artifacts:
    paths:
      - target/
    expire_in: 1 hour

unit-test:
  stage: test
  image: maven:3.9.6-eclipse-temurin-17
  script:
    - mvn test
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml
      coverage_report:
        coverage_format: jacoco
        path: target/site/jacoco/jacoco.xml

integration-test:
  stage: test
  image: maven:3.9.6-eclipse-temurin-17
  services:
    - postgres:16-alpine
    - redis:7-alpine
  variables:
    POSTGRES_DB: operadora_test
    POSTGRES_USER: test_user
    POSTGRES_PASSWORD: test_pass
  script:
    - mvn verify -P integration-tests
  artifacts:
    reports:
      junit: target/failsafe-reports/TEST-*.xml

docker-build:
  stage: package
  image: docker:24
  services:
    - docker:24-dind
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $DOCKER_REGISTRY
    - docker build -t $DOCKER_REGISTRY/$IMAGE_NAME:$CI_COMMIT_SHA .
    - docker tag $DOCKER_REGISTRY/$IMAGE_NAME:$CI_COMMIT_SHA $DOCKER_REGISTRY/$IMAGE_NAME:latest
    - docker push $DOCKER_REGISTRY/$IMAGE_NAME:$CI_COMMIT_SHA
    - docker push $DOCKER_REGISTRY/$IMAGE_NAME:latest
  only:
    - main
    - develop

deploy-staging:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context staging
    - kubectl set image deployment/camunda-app camunda-app=$DOCKER_REGISTRY/$IMAGE_NAME:$CI_COMMIT_SHA -n operadora-digital-staging
    - kubectl rollout status deployment/camunda-app -n operadora-digital-staging
  environment:
    name: staging
    url: https://camunda-staging.operadora.austa.com.br
  only:
    - develop

deploy-production:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context production
    - kubectl set image deployment/camunda-app camunda-app=$DOCKER_REGISTRY/$IMAGE_NAME:$CI_COMMIT_SHA -n operadora-digital
    - kubectl rollout status deployment/camunda-app -n operadora-digital
  environment:
    name: production
    url: https://camunda.operadora.austa.com.br
  only:
    - main
  when: manual
```

## Related Documents
- [01_PROJECT_STRUCTURE.md](./01_PROJECT_STRUCTURE.md)
- [02_DEPENDENCY_SPECIFICATION.md](./02_DEPENDENCY_SPECIFICATION.md)
- [06_INTEGRATION_ARCHITECTURE.md](./06_INTEGRATION_ARCHITECTURE.md)
