package br.com.austa.experiencia.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers configuration for integration tests.
 *
 * Provides PostgreSQL, Kafka, and Redis containers with automatic Spring property configuration.
 * Containers are shared across all tests in the same JVM to improve performance.
 *
 * @see org.testcontainers.junit.jupiter.Testcontainers
 * @see org.springframework.test.context.DynamicPropertySource
 */
@TestConfiguration
public class TestContainersConfig {

    /**
     * PostgreSQL container for database integration tests.
     * Using Postgres 15 with test database and credentials.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("experiencia_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true); // Reuse container across test runs

    /**
     * Kafka container for message streaming tests.
     * Using Confluent Platform 7.4.0 for Kafka compatibility.
     */
    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
        .withReuse(true);

    /**
     * Redis container for caching and session management tests.
     * Using Redis 7 Alpine for minimal footprint.
     */
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(true);

    /**
     * Dynamically configures Spring application properties based on container runtime values.
     * This ensures test application context connects to the correct TestContainer instances.
     *
     * @param registry Spring's dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL datasource configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA/Hibernate configuration for testing
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.dialect",
            () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Kafka bootstrap servers configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");

        // Kafka producer configuration for tests
        registry.add("spring.kafka.producer.key-serializer",
            () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
            () -> "org.springframework.kafka.support.serializer.JsonSerializer");

        // Kafka consumer configuration for tests
        registry.add("spring.kafka.consumer.key-deserializer",
            () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer",
            () -> "org.springframework.kafka.support.serializer.JsonDeserializer");

        // Redis configuration
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379).toString());
        registry.add("spring.redis.timeout", () -> "2000");

        // Redis cache configuration
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.cache.redis.time-to-live", () -> "600000"); // 10 minutes

        // Redis session configuration (if using Spring Session)
        registry.add("spring.session.store-type", () -> "redis");
        registry.add("spring.session.redis.flush-mode", () -> "immediate");
    }

    /**
     * Provides static access to PostgreSQL container for manual queries in tests.
     *
     * @return the PostgreSQL container instance
     */
    public static PostgreSQLContainer<?> getPostgresContainer() {
        return postgres;
    }

    /**
     * Provides static access to Kafka container for manual topic management in tests.
     *
     * @return the Kafka container instance
     */
    public static KafkaContainer getKafkaContainer() {
        return kafka;
    }

    /**
     * Provides static access to Redis container for manual cache operations in tests.
     *
     * @return the Redis container instance
     */
    public static GenericContainer<?> getRedisContainer() {
        return redis;
    }

    /**
     * Utility method to check if all containers are running and healthy.
     * Useful for troubleshooting test failures.
     *
     * @return true if all containers are running
     */
    public static boolean areAllContainersHealthy() {
        return postgres.isRunning() &&
               kafka.isRunning() &&
               redis.isRunning();
    }

    /**
     * Utility method to get container health status report.
     * Useful for debugging test environment issues.
     *
     * @return formatted string with container status
     */
    public static String getContainerHealthStatus() {
        return String.format(
            "TestContainers Health Status:\n" +
            "  PostgreSQL: %s (Port: %d)\n" +
            "  Kafka: %s (Bootstrap: %s)\n" +
            "  Redis: %s (Port: %d)",
            postgres.isRunning() ? "RUNNING" : "STOPPED",
            postgres.getMappedPort(5432),
            kafka.isRunning() ? "RUNNING" : "STOPPED",
            kafka.getBootstrapServers(),
            redis.isRunning() ? "RUNNING" : "STOPPED",
            redis.getMappedPort(6379)
        );
    }
}
