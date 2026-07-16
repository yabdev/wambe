package com.wambe.api.common.security;

import com.wambe.api.common.web.RequestIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ScannerHmacFilter extends OncePerRequestFilter {

    public static final String BODY_DIGEST_ATTRIBUTE =
            ScannerHmacFilter.class.getName() + ".bodyDigest";

    private final byte[] secret;
    private final Duration tolerance;
    private final Clock clock;

    @Autowired
    public ScannerHmacFilter(
            @Value("${wambe.scanner.hmac-secret}") String secret,
            @Value("${wambe.scanner.callback-tolerance}") Duration tolerance) {
        this(secret, tolerance, Clock.systemUTC());
    }

    ScannerHmacFilter(String secret, Duration tolerance, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tolerance = tolerance;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !RequestEnvelopeFilter.isCallbackRequest(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            byte[] body = RequestEnvelopeFilter.body(request);
            if (body == null) {
                RequestEnvelopeFilter.reject(request, response);
                return;
            }
            var cached = new CachedBodyRequest(request, body);
            String timestamp = requiredHeader(request, "X-Wambe-Timestamp");
            String nonce = requiredHeader(request, "X-Wambe-Nonce");
            String suppliedSignature = requiredHeader(request, "X-Wambe-Signature");
            OffsetDateTime requestTime = OffsetDateTime.parse(timestamp);
            if (Duration.between(requestTime.toInstant(), clock.instant()).abs().compareTo(tolerance) > 0) {
                unauthorized(response, request);
                return;
            }

            String bodyDigest = sha256(cached.body());
            String payload = timestamp + "\n" + nonce + "\n" + bodyDigest;
            String expected = hmac(payload);
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    suppliedSignature.toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
                unauthorized(response, request);
                return;
            }

            request.setAttribute(BODY_DIGEST_ATTRIBUTE, bodyDigest);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    "media-scanner",
                    null,
                    java.util.List.of(new SimpleGrantedAuthority("ROLE_SCANNER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(cached, response);
        } catch (RuntimeException exception) {
            unauthorized(response, request);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing header");
        }
        return value;
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void unauthorized(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        response.getWriter().write(
                "{\"error\":{\"code\":\"INVALID_SCANNER_SIGNATURE\","
                        + "\"message\":\"Authentication is invalid\","
                        + "\"requestId\":\"" + requestId + "\"}}");
    }
}
