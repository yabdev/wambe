package com.wambe.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DispatchHmacFilterTest {

    private static final String SECRET = "scanner-test-secret";
    private final ScannerMetrics metrics = new ScannerMetrics(new SimpleMeterRegistry());

    @Test
    void authenticatesBoundedSignedBodyAndPreservesIt() throws Exception {
        byte[] body = "{\"jobId\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        String nonce = UUID.randomUUID().toString();
        var request = request(body);
        request.addHeader("X-Wambe-Timestamp", timestamp);
        request.addHeader("X-Wambe-Nonce", nonce);
        request.addHeader(
                "X-Wambe-Signature",
                hmac(timestamp + "\n" + nonce + "\n" + DispatchHmacFilter.sha256(body)));
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        var hmacFilter = new DispatchHmacFilter(SECRET, Duration.ofMinutes(5));
        new RequestEnvelopeFilter(16_384, metrics).doFilter(request, response, (boundedRequest, boundedResponse) ->
                hmacFilter.doFilter(boundedRequest, boundedResponse, (filteredRequest, ignoredResponse) -> {
                    invoked.set(true);
                    assertThat(filteredRequest.getInputStream().readAllBytes()).isEqualTo(body);
                }));

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void validSizeWithInvalidSignatureRemainsUnauthorized() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        var request = request(body);
        request.addHeader("X-Wambe-Timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        request.addHeader("X-Wambe-Nonce", UUID.randomUUID().toString());
        request.addHeader("X-Wambe-Signature", "invalid");
        var response = new MockHttpServletResponse();

        var hmacFilter = new DispatchHmacFilter(SECRET, Duration.ofMinutes(5));
        new RequestEnvelopeFilter(16_384, metrics).doFilter(request, response, (boundedRequest, boundedResponse) ->
                hmacFilter.doFilter(boundedRequest, boundedResponse, (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("Filter chain must not run");
                }));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void failsClosedWhenEnvelopeFilterDidNotProvideBoundedBody() throws Exception {
        var request = request("{}".getBytes(StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();

        new DispatchHmacFilter(SECRET, Duration.ofMinutes(5))
                .doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("Filter chain must not run");
                });

        assertThat(response.getStatus()).isEqualTo(413);
    }

    private MockHttpServletRequest request(byte[] body) {
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/scan");
        request.setServletPath("/scan");
        request.setContent(body);
        request.addHeader("Content-Length", body.length);
        return request;
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
