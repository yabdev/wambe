package com.wambe.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wambe.api.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hostManagementRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicMetadataDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/public/events/not-a-real-slug/metadata"))
                .andExpect(status().isNotFound());
    }

    @Test
    void localStorageRouteIsAvailableWithoutAuthenticationInTestProfile() throws Exception {
        mockMvc.perform(get("/dev-storage/not-a-real-object"))
                .andExpect(status().isNotFound());
    }

    @Test
    void internalJobsRejectMissingCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/internal/jobs/retention"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scannerRejectsInvalidSignature() throws Exception {
        mockMvc.perform(post("/api/v1/internal/scanner/callback")
                        .header("Content-Length", 2)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scannerRejectsOversizedEnvelopeBeforeAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/internal/scanner/callback")
                        .header("Content-Length", 16_385)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isPayloadTooLarge());
    }
}
