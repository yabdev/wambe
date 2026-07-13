package com.wambe.api.auth;

import com.wambe.api.common.error.ApiException;
import com.wambe.api.common.idempotency.IdempotencyService;
import com.wambe.api.common.rls.RlsContext;
import com.wambe.api.integration.supabase.IdentityAdminPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityLinkService {

    private final IdentityAdminPort identityAdmin;
    private final RlsContext rls;
    private final IdempotencyService idempotency;
    private final JdbcTemplate jdbcTemplate;

    public IdentityLinkService(
            IdentityAdminPort identityAdmin,
            RlsContext rls,
            IdempotencyService idempotency,
            JdbcTemplate jdbcTemplate) {
        this.identityAdmin = identityAdmin;
        this.rls = rls;
        this.idempotency = idempotency;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void confirm(UUID ownerId, UUID key, String provider) {
        requireRecentAuthentication();
        rls.apply(ownerId);
        idempotency.execute(
                ownerId,
                "POST /auth/link-identity",
                key,
                java.util.Map.of("provider", provider),
                HttpStatus.NO_CONTENT,
                MutationResult.class,
                () -> {
                    var identity = identityAdmin.verifyLinkedIdentity(ownerId, provider);
                    jdbcTemplate.update("""
                            insert into identity_link_audit (
                                host_id, provider, provider_subject_hash,
                                verified_email_proof_at, actor, result
                            ) values (?, ?, ?, ?, 'host', 'linked')
                            """,
                            ownerId,
                            provider,
                            sha256(identity.providerSubject()),
                            identity.verifiedAt());
                    return new MutationResult(true);
                });
    }

    private void requireRecentAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)
                || jwt.getToken().getIssuedAt() == null
                || jwt.getToken().getIssuedAt().isBefore(
                        OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10).toInstant())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "RECENT_AUTHENTICATION_REQUIRED",
                    "Sign in again before linking another method");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record MutationResult(boolean completed) {
    }
}
