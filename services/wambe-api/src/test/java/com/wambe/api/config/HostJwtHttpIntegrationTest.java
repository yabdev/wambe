package com.wambe.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.wambe.api.PostgresIntegrationTest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class HostJwtHttpIntegrationTest extends PostgresIntegrationTest {

    private static final String ISSUER = "https://qa.supabase.test/auth/v1";
    private static final String AUDIENCE = "authenticated";
    private static final String ALLOWED_ORIGIN = "https://staging.wambe.test";
    private static final RSAKey SIGNING_KEY = signingKey();
    private static final HttpServer JWKS_SERVER = startJwksServer();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    @SuppressWarnings("unused")
    static void hostSecurityProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
        registry.add("wambe.auth.issuer", () -> ISSUER);
        registry.add("wambe.auth.audience", () -> AUDIENCE);
        registry.add("wambe.cors.allowed-origins", () -> ALLOWED_ORIGIN);
    }

    @AfterAll
    @SuppressWarnings("unused")
    static void stopJwksServer() {
        JWKS_SERVER.stop(0);
    }

    @Test
    void authenticatedHostJwtReachesHostRoute() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token("authenticated", UUID.randomUUID().toString())))
                .andExpect(status().isOk());
    }

    @Test
    void nonHostJwtClaimsFailAtHttpBoundary() throws Exception {
        assertUnauthorized(token("anon", UUID.randomUUID().toString()));
        assertUnauthorized(token("service_role", UUID.randomUUID().toString()));
        assertUnauthorized(token(null, UUID.randomUUID().toString()));
        assertUnauthorized(token("authenticated", "not-a-uuid"));
        assertUnauthorized(token("authenticated", null));
    }

    @Test
    void corsAllowsOnlyTheConfiguredExactOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/events")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));

        mockMvc.perform(options("/api/v1/events")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private static String token(String role, String subject) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)));
        if (subject != null) {
            claims.subject(subject);
        }
        if (role != null) {
            claims.claim("role", role);
        }
        var jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(SIGNING_KEY.getKeyID())
                        .build(),
                claims.build());
        jwt.sign(new RSASSASigner(SIGNING_KEY));
        return jwt.serialize();
    }

    private static RSAKey signingKey() {
        try {
            var pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID("qa-host-key")
                    .build();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not create QA signing key", exception);
        }
    }

    private static HttpServer startJwksServer() {
        try {
            byte[] jwks = new JWKSet(SIGNING_KEY.toPublicJWK())
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
            var server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jwks.length);
                try (var responseBody = exchange.getResponseBody()) {
                    responseBody.write(jwks);
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start QA JWKS server", exception);
        }
    }
}
