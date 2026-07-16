package com.wambe.api.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DeployedSecurityValidatorTest {

    private static final String SCANNER_URL =
            "https://wambe-scanner-staging-a1b2c3-uc.a.run.app";

    @Test
    void acceptsRotatedSecretAndEmptyLocalJobKey() {
        assertThatCode(() -> validator(rotatedSecret(), "", SCANNER_URL, SCANNER_URL)
                        .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsNoProfileAsDeployedWhenStorageIsSupabase() {
        var noProfile = new MockEnvironment();

        assertThatCode(() -> new DeployedSecurityValidator(
                        noProfile,
                        rotatedSecret(),
                        "",
                        SCANNER_URL,
                        SCANNER_URL,
                        16_384,
                        "supabase")
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
    void rejectsAnyDeployedLocalJobKey() {
        assertThatThrownBy(() ->
                        validator(rotatedSecret(), "still-present", SCANNER_URL, SCANNER_URL)
                                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DeployedSecurityValidator.FORBIDDEN_JOB_KEY);
    }

    @Test
    void rejectsNonHttpsMissingOrMismatchedScannerAudience() {
        assertInvalidIdentity("http://scanner.example", "http://scanner.example");
        assertInvalidIdentity(SCANNER_URL, "");
        assertInvalidIdentity(SCANNER_URL, "https://other-scanner.a.run.app");
        assertInvalidIdentity(SCANNER_URL + "/scan", SCANNER_URL + "/scan");
    }

    @Test
    void permitsExplicitDevOnlyProfilesButCannotBeBypassedByMixedProfiles() {
        var testEnvironment = new MockEnvironment();
        testEnvironment.setActiveProfiles("test");
        assertThatCode(() -> new DeployedSecurityValidator(
                        testEnvironment,
                        "test-scanner-secret-change-me",
                        "test-job-key",
                        "http://localhost:8081",
                        "",
                        16_384,
                        "local")
                        .afterPropertiesSet())
                .doesNotThrowAnyException();

        var mixedEnvironment = new MockEnvironment();
        mixedEnvironment.setActiveProfiles("staging", "test");
        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        mixedEnvironment,
                        rotatedSecret(),
                        "",
                        SCANNER_URL,
                        SCANNER_URL,
                        16_384,
                        "supabase")
                        .afterPropertiesSet())
                .hasMessage(DeployedSecurityValidator.FORBIDDEN_PROFILE);
    }

    @Test
    void rejectsDevelopmentProfileInADeployedContext() {
        var development = new MockEnvironment();
        development.setActiveProfiles("development");

        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        development,
                        rotatedSecret(),
                        "",
                        SCANNER_URL,
                        SCANNER_URL,
                        16_384,
                        "supabase")
                        .afterPropertiesSet())
                .hasMessage(DeployedSecurityValidator.FORBIDDEN_PROFILE);
    }

    @Test
    void rejectsLocalOrMissingStorageInADeployedContext() {
        var staging = new MockEnvironment();
        staging.setActiveProfiles("staging");

        assertInvalidStorage(new MockEnvironment(), "local");
        assertInvalidStorage(staging, "local");
        assertInvalidStorage(staging, "");
    }

    @Test
    void rejectsDeployedEnvelopeLimitDrift() {
        var staging = new MockEnvironment();
        staging.setActiveProfiles("staging");

        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        staging,
                        rotatedSecret(),
                        "",
                        SCANNER_URL,
                        SCANNER_URL,
                        32_768,
                        "supabase")
                        .afterPropertiesSet())
                .hasMessage(DeployedSecurityValidator.INVALID_ENVELOPE_LIMIT);
    }

    private void assertInvalidSecret(String secret) {
        assertThatThrownBy(() -> validator(secret, "", SCANNER_URL, SCANNER_URL)
                        .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DeployedSecurityValidator.INVALID_SCANNER_SECRET);
    }

    private void assertInvalidIdentity(String scannerUrl, String audience) {
        assertThatThrownBy(() -> validator(rotatedSecret(), "", scannerUrl, audience)
                        .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DeployedSecurityValidator.INVALID_SCANNER_IDENTITY_CONFIG);
    }

    private void assertInvalidStorage(MockEnvironment environment, String storageType) {
        assertThatThrownBy(() -> new DeployedSecurityValidator(
                        environment,
                        rotatedSecret(),
                        "",
                        SCANNER_URL,
                        SCANNER_URL,
                        16_384,
                        storageType)
                        .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DeployedSecurityValidator.INVALID_STORAGE);
    }

    private DeployedSecurityValidator validator(
            String secret,
            String jobKey,
            String scannerUrl,
            String audience) {
        return new DeployedSecurityValidator(secret, jobKey, scannerUrl, audience);
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
