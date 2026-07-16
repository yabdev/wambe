package com.wambe.scanner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DeployedSecurityValidatorTest {

    @Test
    void acceptsRotatedSecret() {
        assertThatCode(() -> new DeployedSecurityValidator(rotatedSecret())
                        .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingBlankShortAndKnownDefaultSecrets() {
        assertInvalidSecret(null);
        assertInvalidSecret("");
        assertInvalidSecret("short");
        assertInvalidSecret("local-scanner-secret-change-me");
        assertInvalidSecret("test-scanner-secret-change-me");
    }

    @Test
    void permitsExplicitDevOnlyProfilesButRejectsMixedDeployedProfiles() {
        var local = new MockEnvironment();
        local.setActiveProfiles("local");
        assertThatCode(() -> new DeployedSecurityValidator(
                        local,
                        "local-scanner-secret-change-me",
                        true,
                        16_384)
                        .afterPropertiesSet())
                .doesNotThrowAnyException();

        var mixed = new MockEnvironment();
        mixed.setActiveProfiles("production", "test");
        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        mixed,
                        "test-scanner-secret-change-me",
                        true,
                        16_384)
                        .afterPropertiesSet())
                .hasMessage(DeployedSecurityValidator.INVALID_SCANNER_SECRET);
    }

    @Test
    void rejectsLocalDestinationBypassInDeployedProfile() {
        var staging = new MockEnvironment();
        staging.setActiveProfiles("staging");

        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        staging,
                        rotatedSecret(),
                        true,
                        16_384)
                        .afterPropertiesSet())
                .hasMessage(DeployedSecurityValidator.FORBIDDEN_LOCAL_DESTINATIONS);
    }

    @Test
    void rejectsDeployedEnvelopeLimitDrift() {
        var staging = new MockEnvironment();
        staging.setActiveProfiles("staging");

        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        staging,
                        rotatedSecret(),
                        false,
                        32_768)
                        .afterPropertiesSet())
                .hasMessage(DeployedSecurityValidator.INVALID_ENVELOPE_LIMIT);
    }

    @Test
    void rejectsMissingNonHttpsAndLocalDestinationOrigins() {
        var staging = new MockEnvironment();
        staging.setActiveProfiles("staging");
        String secret = rotatedSecret();

        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        staging,
                        secret,
                        false,
                        16_384,
                        "",
                        "https://api.staging.wambe.example")
                        .afterPropertiesSet())
                .hasMessage(DeployedSecurityValidator.INVALID_DESTINATION_CONFIG);
        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        staging,
                        secret,
                        false,
                        16_384,
                        "https://project.supabase.co",
                        "http://localhost:8080")
                        .afterPropertiesSet())
                .hasMessage(DeployedSecurityValidator.INVALID_DESTINATION_CONFIG);
    }

    private void assertInvalidSecret(String secret) {
        assertThatThrownBy(() -> new DeployedSecurityValidator(secret).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DeployedSecurityValidator.INVALID_SCANNER_SECRET);
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
