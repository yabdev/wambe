package com.wambe.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.wambe.api.observability.WambeMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class ScannerHmacFilterTest {

    private static final String SECRET = "test-secret";
    private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");
    private final WambeMetrics metrics = new WambeMetrics(new SimpleMeterRegistry());

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesFreshCorrectlySignedBodyAndPreservesIt() throws Exception {
        String timestamp = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).toString();
        String nonce = UUID.randomUUID().toString();
        byte[] body = "{\"mediaId\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        String digest = sha256(body);
        String signature = hmac(timestamp + "\n" + nonce + "\n" + digest);
        var request = request(body, timestamp, nonce, signature);
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();
        var filter = new ScannerHmacFilter(
                SECRET,
                Duration.ofMinutes(5),
                Clock.fixed(NOW, ZoneOffset.UTC));

        new RequestEnvelopeFilter(16_384, metrics).doFilter(request, response, (boundedRequest, boundedResponse) ->
                filter.doFilter(boundedRequest, boundedResponse, (filteredRequest, filteredResponse) -> {
                    invoked.set(true);
                    assertThat(filteredRequest.getInputStream().readAllBytes()).isEqualTo(body);
                    assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                            .extracting(Object::toString)
                            .contains("ROLE_SCANNER");
                }));

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsStaleCallbackBeforeControllerRuns() throws Exception {
        String stale = OffsetDateTime.ofInstant(NOW.minus(Duration.ofMinutes(6)), ZoneOffset.UTC).toString();
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String nonce = UUID.randomUUID().toString();
        var request = request(
                body,
                stale,
                nonce,
                hmac(stale + "\n" + nonce + "\n" + sha256(body)));
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();
        var filter = new ScannerHmacFilter(
                SECRET,
                Duration.ofMinutes(5),
                Clock.fixed(NOW, ZoneOffset.UTC));

        new RequestEnvelopeFilter(16_384, metrics).doFilter(request, response, (boundedRequest, boundedResponse) ->
                filter.doFilter(
                        boundedRequest,
                        boundedResponse,
                        (ignoredRequest, ignoredResponse) -> invoked.set(true)));

        assertThat(invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void failsClosedWhenEnvelopeFilterDidNotProvideBoundedBody() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        var request = request(body, OffsetDateTime.now(ZoneOffset.UTC).toString(), UUID.randomUUID().toString(), "ignored");
        var response = new MockHttpServletResponse();
        var filter = new ScannerHmacFilter(
                SECRET,
                Duration.ofMinutes(5),
                Clock.fixed(NOW, ZoneOffset.UTC));

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("Filter chain must not run");
        });

        assertThat(response.getStatus()).isEqualTo(413);
    }

    private MockHttpServletRequest request(
            byte[] body,
            String timestamp,
            String nonce,
            String signature) {
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/internal/scanner/callback");
        request.setServletPath("/api/v1/internal/scanner/callback");
        request.setContent(body);
        request.addHeader("Content-Length", body.length);
        request.addHeader("X-Wambe-Timestamp", timestamp);
        request.addHeader("X-Wambe-Nonce", nonce);
        request.addHeader("X-Wambe-Signature", signature);
        request.setAttribute(com.wambe.api.common.web.RequestIdFilter.ATTRIBUTE, UUID.randomUUID().toString());
        return request;
    }

    private String sha256(byte[] body) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
