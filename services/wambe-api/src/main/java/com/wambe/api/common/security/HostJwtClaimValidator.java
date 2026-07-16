package com.wambe.api.common.security;

import com.wambe.api.observability.WambeMetrics;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class HostJwtClaimValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_HOST_CLAIMS =
            new OAuth2Error("invalid_token", "Host token claims are invalid", null);
    private final WambeMetrics metrics;

    public HostJwtClaimValidator(WambeMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object role = jwt.getClaims().get("role");
        if (!"authenticated".equals(role)) {
            metrics.jwtRejected(role == null ? "missing_role" : "wrong_role");
            return OAuth2TokenValidatorResult.failure(INVALID_HOST_CLAIMS);
        }
        try {
            UUID.fromString(jwt.getSubject());
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException | NullPointerException exception) {
            metrics.jwtRejected("invalid_subject");
            return OAuth2TokenValidatorResult.failure(INVALID_HOST_CLAIMS);
        }
    }
}
