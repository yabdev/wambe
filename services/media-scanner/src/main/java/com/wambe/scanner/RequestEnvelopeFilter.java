package com.wambe.scanner;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestEnvelopeFilter extends OncePerRequestFilter {

    public static final String BODY_ATTRIBUTE = RequestEnvelopeFilter.class.getName() + ".body";

    private final int maxBytes;
    private final ScannerMetrics metrics;

    public RequestEnvelopeFilter(
            @Value("${wambe.security.request-envelope.max-bytes:16384}") int maxBytes,
            ScannerMetrics metrics) {
        if (maxBytes < 1) {
            throw new IllegalArgumentException("Request envelope limit must be positive");
        }
        this.maxBytes = maxBytes;
        this.metrics = metrics;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isScanRequest(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            request.setAttribute(BODY_ATTRIBUTE, readBoundedBody(request));
            filterChain.doFilter(request, response);
        } catch (RejectedEnvelope exception) {
            metrics.envelopeRejected();
            reject(response);
        }
    }

    public static boolean isScanRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/scan".equals(pathWithinApplication(request));
    }

    public static byte[] body(HttpServletRequest request) {
        Object value = request.getAttribute(BODY_ATTRIBUTE);
        return value instanceof byte[] bytes ? bytes.clone() : null;
    }

    private byte[] readBoundedBody(HttpServletRequest request) throws IOException {
        String transferEncoding = request.getHeader("Transfer-Encoding");
        boolean chunked = transferEncoding != null
                && "chunked".equalsIgnoreCase(transferEncoding.trim());
        if (transferEncoding != null && !chunked) {
            throw new RejectedEnvelope();
        }

        List<String> contentLengths = contentLengthValues(request);
        if (chunked && !contentLengths.isEmpty()) {
            throw new RejectedEnvelope();
        }

        Long declaredLength = null;
        if (!chunked) {
            declaredLength = parseContentLength(contentLengths);
            if (declaredLength == null
                    || declaredLength < 0
                    || declaredLength > maxBytes) {
                throw new RejectedEnvelope();
            }
        }

        byte[] body = request.getInputStream().readNBytes(maxBytes + 1);
        if (body.length > maxBytes
                || (declaredLength != null && body.length != declaredLength)) {
            throw new RejectedEnvelope();
        }
        return body;
    }

    private List<String> contentLengthValues(HttpServletRequest request) {
        List<String> values = new ArrayList<>();
        Collections.list(request.getHeaders("Content-Length")).forEach(header -> {
            for (String value : header.split(",")) {
                if (!value.isBlank()) {
                    values.add(value.trim());
                }
            }
        });
        return values;
    }

    private Long parseContentLength(List<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        Long parsed = null;
        try {
            for (String value : values) {
                long current = Long.parseLong(value);
                if (parsed != null && parsed != current) {
                    throw new RejectedEnvelope();
                }
                parsed = current;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new RejectedEnvelope();
        }
    }

    static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"request_envelope_rejected\"}");
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath != null
                        && !contextPath.isEmpty()
                        && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
    }

    private static final class RejectedEnvelope extends RuntimeException {
    }
}
