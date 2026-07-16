package com.wambe.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.wambe.api.common.web.RequestIdFilter;
import com.wambe.api.observability.WambeMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestEnvelopeFilterTest {

    private static final int LIMIT = 16;
    private final WambeMetrics metrics = new WambeMetrics(new SimpleMeterRegistry());

    @Test
    void acceptsFixedBodyAtLimitAndExposesOnlyBoundedBytes() throws Exception {
        byte[] body = new byte[LIMIT];
        var request = request(body);
        request.addHeader("Content-Length", body.length);
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        new RequestEnvelopeFilter(LIMIT, metrics).doFilter(request, response, (filtered, ignored) -> {
            invoked.set(true);
            assertThat(RequestEnvelopeFilter.body((MockHttpServletRequest) filtered))
                    .isEqualTo(body);
        });

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsDeclaredOversizeWithoutInvokingNextFilter() throws Exception {
        var request = request("attacker-payload".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Content-Length", LIMIT + 1);
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(RequestIdFilter.ATTRIBUTE, requestId);
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).doesNotContain("attacker-payload");
        assertThat(response.getContentAsString()).contains(requestId);
    }

    @Test
    void rejectsMissingFixedBodyLength() throws Exception {
        var request = request(new byte[] {1});
        var response = new MockHttpServletResponse();

        new RequestEnvelopeFilter(LIMIT, metrics).doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("Filter chain must not run");
        });

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void acceptsBoundedChunkedBody() throws Exception {
        byte[] body = new byte[LIMIT];
        var request = request(body);
        request.addHeader("Transfer-Encoding", "chunked");
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsChunkedBodyAboveLimit() throws Exception {
        byte[] body = new byte[LIMIT + 1];
        var request = request(body);
        request.addHeader("Transfer-Encoding", "chunked");
        var response = new MockHttpServletResponse();

        new RequestEnvelopeFilter(LIMIT, metrics).doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("Filter chain must not run");
        });

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void neverReadsMoreThanLimitPlusOneFromUnboundedChunkedStream() throws Exception {
        var reads = new AtomicInteger();
        var request = streamingRequest(reads);
        request.addHeader("Transfer-Encoding", "chunked");
        var response = new MockHttpServletResponse();

        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("Filter chain must not run");
                });

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(reads.get()).isLessThanOrEqualTo(LIMIT + 1);
    }

    @Test
    void protectsCallbackBehindServletContextPath() throws Exception {
        var request = request(new byte[0]);
        request.setContextPath("/wambe");
        request.setRequestURI("/wambe/api/v1/internal/scanner/callback");
        request.addHeader("Content-Length", 0);
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(invoked).isTrue();
    }

    @Test
    void enforcesApprovedSixteenKibibyteBoundary() throws Exception {
        byte[] atLimit = new byte[16_384];
        var accepted = request(atLimit);
        accepted.addHeader("Content-Length", atLimit.length);
        var acceptedResponse = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();
        new RequestEnvelopeFilter(16_384, metrics)
                .doFilter(accepted, acceptedResponse, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        var rejected = request(new byte[1]);
        rejected.addHeader("Content-Length", 16_385);
        var rejectedResponse = new MockHttpServletResponse();
        new RequestEnvelopeFilter(16_384, metrics)
                .doFilter(rejected, rejectedResponse, (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("Filter chain must not run");
                });

        assertThat(invoked).isTrue();
        assertThat(rejectedResponse.getStatus()).isEqualTo(413);
    }

    @Test
    void rejectsMalformedConflictingAndAmbiguousLengthHeaders() throws Exception {
        var malformed = request(new byte[] {1});
        malformed.addHeader("Content-Length", "not-a-number");
        assertEnvelopeRejected(malformed);

        var conflicting = request(new byte[] {1});
        conflicting.addHeader("Content-Length", "1");
        conflicting.addHeader("Content-Length", "2");
        assertEnvelopeRejected(conflicting);

        var ambiguous = request(new byte[] {1});
        ambiguous.addHeader("Content-Length", "1");
        ambiguous.addHeader("Transfer-Encoding", "chunked");
        assertEnvelopeRejected(ambiguous);
    }

    private MockHttpServletRequest request(byte[] body) {
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/internal/scanner/callback");
        request.setServletPath("/api/v1/internal/scanner/callback");
        request.setContent(body);
        return request;
    }

    private void assertEnvelopeRejected(MockHttpServletRequest request) throws Exception {
        var response = new MockHttpServletResponse();
        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("Filter chain must not run");
                });
        assertThat(response.getStatus()).isEqualTo(413);
    }

    private MockHttpServletRequest streamingRequest(AtomicInteger reads) {
        var request = new MockHttpServletRequest() {
            @Override
            public ServletInputStream getInputStream() {
                return new ServletInputStream() {
                    @Override
                    public int read() {
                        reads.incrementAndGet();
                        return 'x';
                    }

                    @Override
                    public boolean isFinished() {
                        return false;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {
                    }
                };
            }
        };
        request.setMethod("POST");
        request.setRequestURI("/api/v1/internal/scanner/callback");
        request.setServletPath("/api/v1/internal/scanner/callback");
        return request;
    }
}
