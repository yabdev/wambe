package com.wambe.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wambe.api.integration.scanner.ScannerIdentityTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("staging")
class DeployedDevStorageSecurityIntegrationTest {

    private static final String SCANNER_URL =
            "https://wambe-scanner-staging-a1b2c3-uc.a.run.app";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScannerIdentityTokenProvider scannerIdentityTokenProvider;

    @DynamicPropertySource
    static void deployedProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresIntegrationTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "wambe_api");
        registry.add("spring.datasource.password", () -> "wambe_test");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "1");
        registry.add("spring.flyway.user", PostgresIntegrationTest.POSTGRES::getUsername);
        registry.add("spring.flyway.password", PostgresIntegrationTest.POSTGRES::getPassword);
        registry.add("wambe.storage.type", () -> "supabase");
        registry.add("wambe.scanner.hmac-secret", DeployedDevStorageSecurityIntegrationTest::rotatedSecret);
        registry.add("wambe.scanner.url", () -> SCANNER_URL);
        registry.add("wambe.scanner.audience", () -> SCANNER_URL);
    }

    @Test
    void deployedContextDeniesTheLocalStorageRoute() throws Exception {
        mockMvc.perform(get("/dev-storage/not-a-real-object"))
                .andExpect(status().isUnauthorized());
    }

    private static String rotatedSecret() {
        return String.join(
                "-",
                "wambe",
                "test",
                "rotated",
                "scanner",
                "secret",
                "not",
                "a",
                "credential");
    }
}
