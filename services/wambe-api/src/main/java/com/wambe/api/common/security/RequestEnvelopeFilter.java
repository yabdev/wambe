package com.wambe.api.common.security;

import com.wambe.api.observability.WambeMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.wambe.api.common.web.RequestIdFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestEnvelopeFilter extends OncePerRequestFilter {

    public static final String BODY_ATTRIBUTE = RequestEnvelopeFilter.class.getName() + ".body";
    private static final String CALLBACK_PATH = "/api/v1/internal/scanner/callback";

    private final int maxBytes;
    private final WambeMetrics metrics;

    public RequestEnvelopeFilter(
            @Value("${wambe.security.request-envelope.max-bytes:16384}") int maxBytes,
            WambeMetrics metrics) {
        if (maxBytes < 1) {
            throw new IllegalArgumentException("Request envelope limit must be positive");
        }
        this.maxBytes = maxBytes;
        this.metrics = metrics;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isCallbackRequest(request);
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
            reject(request, response);
        }
    }

    public static boolean isCallbackRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && CALLBACK_PATH.equals(pathWithinApplication(request));
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

    static void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("application/json");
        String requestId = safeRequestId(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        String suffix = requestId == null ? "" : ",\"requestId\":\"" + requestId + "\"";
        response.getWriter().write("{\"error\":{\"code\":\"REQUEST_ENVELOPE_REJECTED\","
                + "\"message\":\"Request body is not accepted\"" + suffix + "}}");
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

    private static String safeRequestId(Object value) {
        if (!(value instanceof String requestId)) {
            return null;
        }
        try {
            return UUID.fromString(requestId).toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static final class RejectedEnvelope extends RuntimeException {
    }
}
