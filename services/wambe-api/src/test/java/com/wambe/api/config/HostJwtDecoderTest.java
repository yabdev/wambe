package com.wambe.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.wambe.api.common.security.HostJwtClaimValidator;
import com.wambe.api.observability.WambeMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class HostJwtDecoderTest {

    private static final String ISSUER = "https://test.supabase.co/auth/v1";
    private static final String AUDIENCE = "authenticated";

    private HttpServer server;
    private RSAKey signingKey;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        signingKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID("host-test-key")
                .build();
        byte[] jwks = new JWKSet(signingKey.toPublicJWK())
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            exchange.getResponseBody().write(jwks);
            exchange.close();
        });
        server.start();

        String jwksUri = "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
        decoder = new SecurityConfig().hostJwtDecoder(
                jwksUri,
                ISSUER,
                AUDIENCE,
                new HostJwtClaimValidator(new WambeMetrics(new SimpleMeterRegistry())));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void acceptsSignedHostTokenWithRequiredClaims() throws Exception {
        var decoded = decoder.decode(token("authenticated", UUID.randomUUID().toString()));

        assertThat(decoded.getClaimAsString("role")).isEqualTo("authenticated");
    }

    @Test
    void rejectsSignedTokensOutsideHostBoundary() throws Exception {
        assertThatThrownBy(() -> decoder.decode(token("anon", UUID.randomUUID().toString())))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> decoder.decode(token("service_role", UUID.randomUUID().toString())))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> decoder.decode(token(null, UUID.randomUUID().toString())))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> decoder.decode(token("authenticated", "not-a-uuid")))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> decoder.decode(token("authenticated", null)))
                .isInstanceOf(RuntimeException.class);
    }

    private String token(String role, String subject) throws Exception {
        Instant now = Instant.now();
        var claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)));
        if (subject != null) {
            claimsBuilder.subject(subject);
        }
        if (role != null) {
            claimsBuilder.claim("role", role);
        }
        var claims = claimsBuilder.build();
        var jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(signingKey.getKeyID())
                        .build(),
                claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
}
