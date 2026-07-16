package com.wambe.scanner;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DispatchHmacFilter extends OncePerRequestFilter {

    private final byte[] secret;
    private final Duration tolerance;

    public DispatchHmacFilter(
            @Value("${wambe.scanner.hmac-secret}") String secret,
            @Value("${wambe.scanner.request-tolerance}") Duration tolerance) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tolerance = tolerance;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !RequestEnvelopeFilter.isScanRequest(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            byte[] body = RequestEnvelopeFilter.body(request);
            if (body == null) {
                RequestEnvelopeFilter.reject(response);
                return;
            }
            var cached = new CachedRequest(request, body);
            String timestamp = required(request, "X-Wambe-Timestamp");
            String nonce = required(request, "X-Wambe-Nonce");
            String supplied = required(request, "X-Wambe-Signature").toLowerCase();
            Instant requestTime = OffsetDateTime.parse(timestamp).toInstant();
            if (Duration.between(requestTime, Instant.now()).abs().compareTo(tolerance) > 0) {
                reject(response);
                return;
            }
            String digest = sha256(cached.body);
            String expected = hmac(timestamp + "\n" + nonce + "\n" + digest);
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    supplied.getBytes(StandardCharsets.US_ASCII))) {
                reject(response);
                return;
            }
            filterChain.doFilter(cached, response);
        } catch (RuntimeException exception) {
            reject(response);
        }
    }

    static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String required(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing signature header");
        }
        return value;
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"invalid_signature\"}");
    }

    private static final class CachedRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }
    }
}
