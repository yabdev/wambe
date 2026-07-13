package com.wambe.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wambe")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("db/test-init.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "wambe_api");
        registry.add("spring.datasource.password", () -> "wambe_test");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "1");
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("wambe.storage.local-root", () -> "target/test-storage");
        registry.add("wambe.internal-jobs.local-key", () -> "test-jobs-key");
    }
}
