package com.wambe.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ScannerIdentityTokenConfigurationTest {

    @Test
    void bypassesGoogleIdentityOnlyForExplicitDevOnlyProfiles() {
        assertThat(isDevOnly("local")).isTrue();
        assertThat(isDevOnly("test")).isTrue();
        assertThat(isDevOnly("local", "test")).isTrue();

        assertThat(isDevOnly()).isFalse();
        assertThat(isDevOnly("staging")).isFalse();
        assertThat(isDevOnly("production", "test")).isFalse();
        assertThat(isDevOnly("test", "unexpected")).isFalse();
    }

    private boolean isDevOnly(String... profiles) {
        var environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return ScannerIdentityTokenConfiguration.isExplicitDevOnly(environment);
    }
}
