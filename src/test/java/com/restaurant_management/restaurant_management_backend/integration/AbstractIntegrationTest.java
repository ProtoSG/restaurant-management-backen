package com.restaurant_management.restaurant_management_backend.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests.
 *
 * Uses the Singleton Container pattern: one PostgreSQL container starts when the
 * first test loads and stays alive for the entire test suite (JVM shutdown cleans up).
 * This avoids the Spring context cache/container lifecycle mismatch that happens
 * when @Testcontainers/@Container stops a container between test classes while
 * Spring's application context cache still holds a connection to the old port.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.flyway.baseline-on-migrate=false",
    "spring.flyway.baseline-version=0",
    "application.security.jwt.secret-key=dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy1vbmx5LW5vdC1yZWFsLTEyMzQ1Ng==",
    "admin.default.username=admin",
    "admin.default.password=admin123",
    // Fake keys — the voiceorder module's beans (AnthropicClient/OpenAIClient) construct
    // locally with no network call, so these just need to satisfy Spring, not be real.
    // No integration test exercises the voiceorder endpoints yet.
    "application.anthropic.api-key=sk-ant-fake-key-for-integration-tests-only-not-real",
    "application.openai.api-key=sk-fake-key-for-integration-tests-only-not-real"
})
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
