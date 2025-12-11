# Dependency Specification - Maven POM Configuration

## Document Control
- **Version**: 1.0
- **Author**: System Architecture Designer
- **Date**: 2025-12-11
- **Status**: Draft

## Overview
This document specifies all Maven dependencies required for the Camunda 7 BPMN implementation, including versions, scopes, and configuration.

## Parent POM Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.austa</groupId>
    <artifactId>operadora-digital-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>AUSTA Operadora Digital - Parent POM</name>
    <description>Digital Health Operator Platform with Camunda 7 BPM</description>

    <properties>
        <!-- Java Version -->
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Spring Boot -->
        <spring-boot.version>3.2.1</spring-boot.version>

        <!-- Camunda -->
        <camunda.version>7.20.0</camunda.version>
        <camunda-spin.version>1.20.0</camunda-spin.version>
        <camunda-template-engines.version>2.2.0</camunda-template-engines.version>

        <!-- Database -->
        <postgresql.version>42.7.1</postgresql.version>
        <flyway.version>10.4.1</flyway.version>
        <hikaricp.version>5.1.0</hikaricp.version>

        <!-- Messaging -->
        <kafka.version>3.6.1</kafka.version>
        <spring-kafka.version>3.1.1</spring-kafka.version>

        <!-- Integration -->
        <apache-httpclient.version>5.3</apache-httpclient.version>
        <okhttp.version>4.12.0</okhttp.version>
        <jackson.version>2.16.1</jackson.version>

        <!-- Redis Cache -->
        <jedis.version>5.1.0</jedis.version>

        <!-- ML/AI -->
        <tensorflow.version>1.15.0</tensorflow.version>
        <dl4j.version>1.0.0-M2.1</dl4j.version>

        <!-- Monitoring -->
        <micrometer.version>1.12.2</micrometer.version>
        <prometheus.version>0.16.0</prometheus.version>

        <!-- Testing -->
        <junit-jupiter.version>5.10.1</junit-jupiter.version>
        <assertj.version>3.25.1</assertj.version>
        <mockito.version>5.8.0</mockito.version>
        <testcontainers.version>1.19.3</testcontainers.version>
        <camunda-bpm-assert.version>15.0.0</camunda-bpm-assert.version>

        <!-- Utilities -->
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <commons-lang3.version>3.14.0</commons-lang3.version>
        <guava.version>33.0.0-jre</guava.version>

        <!-- Plugin Versions -->
        <maven-compiler-plugin.version>3.12.1</maven-compiler-plugin.version>
        <maven-surefire-plugin.version>3.2.3</maven-surefire-plugin.version>
        <maven-failsafe-plugin.version>3.2.3</maven-failsafe-plugin.version>
        <jacoco-maven-plugin.version>0.8.11</jacoco-maven-plugin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Camunda BOM -->
            <dependency>
                <groupId>org.camunda.bpm</groupId>
                <artifactId>camunda-bom</artifactId>
                <version>${camunda.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>operadora-domain</module>
        <module>operadora-bpmn-delegates</module>
        <module>operadora-services</module>
        <module>operadora-integration</module>
        <module>operadora-messaging</module>
        <module>operadora-webapp</module>
    </modules>

</project>
```

## Main Application Module POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.austa</groupId>
        <artifactId>operadora-digital-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>operadora-webapp</artifactId>
    <packaging>jar</packaging>

    <name>AUSTA Operadora Digital - Web Application</name>

    <dependencies>
        <!-- ==================== CAMUNDA PLATFORM 7 ==================== -->

        <!-- Camunda Spring Boot Starter -->
        <dependency>
            <groupId>org.camunda.bpm.springboot</groupId>
            <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
            <version>${camunda.version}</version>
        </dependency>

        <!-- Camunda REST API -->
        <dependency>
            <groupId>org.camunda.bpm.springboot</groupId>
            <artifactId>camunda-bpm-spring-boot-starter-rest</artifactId>
            <version>${camunda.version}</version>
        </dependency>

        <!-- Camunda External Task Client -->
        <dependency>
            <groupId>org.camunda.bpm</groupId>
            <artifactId>camunda-external-task-client</artifactId>
        </dependency>

        <!-- Camunda Spin for JSON/XML -->
        <dependency>
            <groupId>org.camunda.spin</groupId>
            <artifactId>camunda-spin-dataformat-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.camunda.bpm</groupId>
            <artifactId>camunda-engine-plugin-spin</artifactId>
        </dependency>

        <!-- Camunda Template Engines (Freemarker) -->
        <dependency>
            <groupId>org.camunda.template-engines</groupId>
            <artifactId>camunda-template-engines-freemarker</artifactId>
            <version>${camunda-template-engines.version}</version>
        </dependency>

        <!-- Camunda DMN Engine -->
        <dependency>
            <groupId>org.camunda.bpm.dmn</groupId>
            <artifactId>camunda-engine-dmn</artifactId>
        </dependency>

        <!-- ==================== SPRING BOOT ==================== -->

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- ==================== DATABASE ==================== -->

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgresql.version}</version>
        </dependency>

        <!-- HikariCP Connection Pool -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>${hikaricp.version}</version>
        </dependency>

        <!-- Flyway Database Migration -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
            <version>${flyway.version}</version>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>${flyway.version}</version>
        </dependency>

        <!-- ==================== MESSAGING (KAFKA) ==================== -->

        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
            <version>${spring-kafka.version}</version>
        </dependency>

        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <version>${kafka.version}</version>
        </dependency>

        <!-- ==================== CACHING (REDIS) ==================== -->

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>${jedis.version}</version>
        </dependency>

        <!-- ==================== HTTP CLIENTS ==================== -->

        <!-- Apache HTTP Client for REST integrations -->
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
            <version>${apache-httpclient.version}</version>
        </dependency>

        <!-- OkHttp for WhatsApp Business API -->
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>${okhttp.version}</version>
        </dependency>

        <!-- ==================== JSON PROCESSING ==================== -->

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.module</groupId>
            <artifactId>jackson-module-parameter-names</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- ==================== MONITORING & OBSERVABILITY ==================== -->

        <!-- Micrometer for metrics -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
            <version>${micrometer.version}</version>
        </dependency>

        <!-- Prometheus Java Client -->
        <dependency>
            <groupId>io.prometheus</groupId>
            <artifactId>simpleclient</artifactId>
            <version>${prometheus.version}</version>
        </dependency>

        <!-- ==================== UTILITIES ==================== -->

        <!-- Lombok for boilerplate reduction -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- MapStruct for DTO mapping -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Apache Commons Lang3 -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>${commons-lang3.version}</version>
        </dependency>

        <!-- Google Guava -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>${guava.version}</version>
        </dependency>

        <!-- ==================== TESTING ==================== -->

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Camunda BPM Assert -->
        <dependency>
            <groupId>org.camunda.bpm.assert</groupId>
            <artifactId>camunda-bpm-assert</artifactId>
            <version>${camunda-bpm-assert.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- JUnit 5 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit-jupiter.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- AssertJ -->
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Mockito -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Testcontainers for integration tests -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>kafka</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- H2 for in-memory testing -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- ==================== INTERNAL MODULES ==================== -->

        <dependency>
            <groupId>com.austa</groupId>
            <artifactId>operadora-domain</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>com.austa</groupId>
            <artifactId>operadora-bpmn-delegates</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>com.austa</groupId>
            <artifactId>operadora-services</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>com.austa</groupId>
            <artifactId>operadora-integration</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>com.austa</groupId>
            <artifactId>operadora-messaging</artifactId>
            <version>${project.version}</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin.version}</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- Spring Boot Maven Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Maven Surefire Plugin (Unit Tests) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>${maven-surefire-plugin.version}</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                        <include>**/*Tests.java</include>
                    </includes>
                </configuration>
            </plugin>

            <!-- Maven Failsafe Plugin (Integration Tests) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>${maven-failsafe-plugin.version}</version>
                <configuration>
                    <includes>
                        <include>**/*IT.java</include>
                        <include>**/*IntegrationTest.java</include>
                    </includes>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- JaCoCo Code Coverage -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco-maven-plugin.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>check</id>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>PACKAGE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.80</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

        </plugins>
    </build>

</project>
```

## Dependency Version Management Strategy

### Version Selection Criteria
1. **Stability**: Prefer stable releases over beta/RC versions
2. **Security**: Regular security patch updates
3. **Compatibility**: Ensure Camunda 7 compatibility
4. **LTS Support**: Prefer LTS versions where available

### Update Schedule
- **Critical Security Updates**: Immediate
- **Minor Version Updates**: Monthly review
- **Major Version Updates**: Quarterly planning

## Key Dependency Justifications

### Camunda 7.20.0
- **Why**: Latest stable Camunda 7.x with Spring Boot 3 support
- **Alternatives**: Camunda 8 (cloud-native, but different architecture)
- **Decision**: Camunda 7 chosen for on-premise deployment control

### Spring Boot 3.2.1
- **Why**: Latest Spring Boot 3.x LTS with Java 17 support
- **Alternatives**: Spring Boot 2.7.x (older, pre-Java 17)
- **Decision**: Spring Boot 3 for modern features and long-term support

### PostgreSQL 42.7.1
- **Why**: Latest stable JDBC driver with performance improvements
- **Alternatives**: Oracle, MySQL (considered but PostgreSQL preferred for JSON support)
- **Decision**: PostgreSQL for JSONB support and open-source licensing

### Kafka 3.6.1
- **Why**: Production-ready event streaming with exactly-once semantics
- **Alternatives**: RabbitMQ, ActiveMQ (simpler but less scalable)
- **Decision**: Kafka for high-throughput event-driven architecture

## Gradle Alternative (Optional)

For teams preferring Gradle over Maven:

```groovy
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.1'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.austa'
version = '1.0.0-SNAPSHOT'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

ext {
    camundaVersion = '7.20.0'
    testcontainersVersion = '1.19.3'
}

dependencies {
    implementation platform("org.camunda.bpm:camunda-bom:${camundaVersion}")
    implementation 'org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-webapp'
    implementation 'org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-rest'

    // ... additional dependencies similar to Maven
}
```

## Related Documents
- [01_PROJECT_STRUCTURE.md](./01_PROJECT_STRUCTURE.md)
- [03_DATABASE_SCHEMA.md](./03_DATABASE_SCHEMA.md)
- [ADR-001: Build Tool Selection](./adr/ADR-001-build-tool-selection.md)
