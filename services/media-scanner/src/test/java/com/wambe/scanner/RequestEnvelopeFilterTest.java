package com.wambe.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestEnvelopeFilterTest {

    private static final int LIMIT = 16;
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ScannerMetrics metrics = new ScannerMetrics(registry);

    @Test
    void acceptsFixedBodyAtLimit() throws Exception {
        byte[] body = new byte[LIMIT];
        var request = request(body);
        request.addHeader("Content-Length", body.length);
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(RequestEnvelopeFilter.body(request)).isEqualTo(body);
    }

    @Test
    void rejectsDeclaredOversizeBeforeNextFilter() throws Exception {
        var request = request(new byte[] {1});
        request.addHeader("Content-Length", LIMIT + 1);
        var response = new MockHttpServletResponse();

        new RequestEnvelopeFilter(LIMIT, metrics).doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("Filter chain must not run");
        });

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(registry.get("wambe.security.envelope.rejected")
                        .tag("service", "media_scanner")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void rejectsMissingFixedLength() throws Exception {
        var response = new MockHttpServletResponse();

        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(request(new byte[] {1}), response, (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("Filter chain must not run");
                });

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void boundsChunkedBodies() throws Exception {
        var accepted = request(new byte[LIMIT]);
        accepted.addHeader("Transfer-Encoding", "chunked");
        var acceptedResponse = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();
        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(accepted, acceptedResponse, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        var rejected = request(new byte[LIMIT + 1]);
        rejected.addHeader("Transfer-Encoding", "chunked");
        var rejectedResponse = new MockHttpServletResponse();
        new RequestEnvelopeFilter(LIMIT, metrics)
                .doFilter(rejected, rejectedResponse, (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("Filter chain must not run");
                });

        assertThat(invoked).isTrue();
        assertThat(rejectedResponse.getStatus()).isEqualTo(413);
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
    void protectsScanBehindServletContextPath() throws Exception {
        var request = request(new byte[0]);
        request.setContextPath("/scanner");
        request.setRequestURI("/scanner/scan");
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
    void rejectsMalformedAndAmbiguousLengthHeaders() throws Exception {
        var malformed = request(new byte[] {1});
        malformed.addHeader("Content-Length", "not-a-number");
        assertEnvelopeRejected(malformed);

        var ambiguous = request(new byte[] {1});
        ambiguous.addHeader("Content-Length", "1");
        ambiguous.addHeader("Transfer-Encoding", "chunked");
        assertEnvelopeRejected(ambiguous);
    }

    private MockHttpServletRequest request(byte[] body) {
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/scan");
        request.setServletPath("/scan");
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
        request.setRequestURI("/scan");
        request.setServletPath("/scan");
        return request;
    }
}
