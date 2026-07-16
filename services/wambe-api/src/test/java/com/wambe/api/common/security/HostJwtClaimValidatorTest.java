package com.wambe.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.wambe.api.observability.WambeMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class HostJwtClaimValidatorTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final HostJwtClaimValidator validator =
            new HostJwtClaimValidator(new WambeMetrics(registry));

    @Test
    void acceptsAuthenticatedRoleWithUuidSubject() {
        assertThat(validator.validate(jwt("authenticated", UUID.randomUUID().toString())).hasErrors())
                .isFalse();
    }

    @Test
    void rejectsMissingAndNonHostRoles() {
        assertThat(validator.validate(jwt(null, UUID.randomUUID().toString())).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwt("anon", UUID.randomUUID().toString())).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwt("service_role", UUID.randomUUID().toString())).hasErrors())
                .isTrue();
        assertThat(registry.get("wambe.security.jwt.rejected").counters())
                .hasSize(2);
    }

    @Test
    void rejectsMalformedSubject() {
        assertThat(validator.validate(jwt("authenticated", "not-a-uuid")).hasErrors())
                .isTrue();
        assertThat(registry.get("wambe.security.jwt.rejected")
                        .tag("reason", "invalid_subject")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    private Jwt jwt(String role, String subject) {
        var builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.build();
    }
}
