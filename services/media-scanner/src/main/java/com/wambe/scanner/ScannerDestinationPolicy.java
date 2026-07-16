package com.wambe.scanner;

import java.net.URI;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("deployedSecurityValidator")
public class ScannerDestinationPolicy {

    private static final String CALLBACK_PATH = "/api/v1/internal/scanner/callback";
    private static final String SUPABASE_OBJECT_PATH = "/storage/v1/object/";
    private static final String LOCAL_STORAGE_PATH = "/dev-storage/";

    private final Origin supabaseOrigin;
    private final Origin apiOrigin;
    private final boolean allowLocal;
    private final ScannerMetrics metrics;

    public ScannerDestinationPolicy(
            @Value("${wambe.scanner.destinations.supabase-url:}") String supabaseUrl,
            @Value("${wambe.scanner.destinations.api-base-url:}") String apiBaseUrl,
            @Value("${wambe.scanner.destinations.allow-local:false}") boolean allowLocal,
            ScannerMetrics metrics) {
        this.allowLocal = allowLocal;
        this.metrics = metrics;
        this.apiOrigin = trustedOrigin(apiBaseUrl, allowLocal, "invalid_api_origin");
        this.supabaseOrigin = allowLocal && (supabaseUrl == null || supabaseUrl.isBlank())
                ? null
                : trustedOrigin(supabaseUrl, false, "invalid_supabase_origin");
    }

    public void validate(ScanRequest request) {
        validateStorage(request.readUrl());
        validateStorage(request.previewWriteUrl());
        validateCallback(request.callbackUrl());
    }

    private void validateStorage(URI destination) {
        requireNetworkUri(destination, "storage_uri");
        if (allowLocal && apiOrigin.matches(destination)) {
            if (!destination.getRawPath().startsWith(LOCAL_STORAGE_PATH)
                    || destination.getRawQuery() != null
                    || destination.getRawFragment() != null) {
                reject("storage_path");
            }
            return;
        }
        if (!"https".equalsIgnoreCase(destination.getScheme())) {
            reject("storage_scheme");
        }
        if (!usesDefaultPort(destination)) {
            reject("storage_uri");
        }
        if (supabaseOrigin == null || !supabaseOrigin.matches(destination)) {
            reject("storage_host");
        }
        if (!destination.getRawPath().startsWith(SUPABASE_OBJECT_PATH)
                || destination.getRawFragment() != null) {
            reject("storage_path");
        }
    }

    private void validateCallback(URI destination) {
        requireNetworkUri(destination, "callback_uri");
        if (!apiOrigin.matches(destination)) {
            reject("callback_origin");
        }
        if (!CALLBACK_PATH.equals(destination.getRawPath())
                || destination.getRawQuery() != null
                || destination.getRawFragment() != null) {
            reject("callback_path");
        }
    }

    private Origin trustedOrigin(String value, boolean httpAllowed, String reason) {
        try {
            URI uri = URI.create(value);
            requireNetworkUri(uri, reason);
            boolean validScheme = "https".equalsIgnoreCase(uri.getScheme())
                    || (httpAllowed && "http".equalsIgnoreCase(uri.getScheme()));
            String path = uri.getRawPath();
            if (!validScheme
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || !(path == null || path.isEmpty() || "/".equals(path))
                    || (!httpAllowed && !usesDefaultPort(uri))
                    || (!httpAllowed && isIpLiteral(uri.getHost()))) {
                reject(reason);
            }
            return Origin.from(uri);
        } catch (IllegalArgumentException | NullPointerException exception) {
            reject(reason);
            throw new IllegalStateException("unreachable");
        }
    }

    private void requireNetworkUri(URI uri, String reason) {
        if (uri == null
                || uri.isOpaque()
                || uri.getScheme() == null
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getPort() < -1
                || uri.getPort() == 0) {
            reject(reason);
        }
    }

    private boolean usesDefaultPort(URI uri) {
        return uri.getPort() == -1
                || ("https".equalsIgnoreCase(uri.getScheme()) && uri.getPort() == 443)
                || ("http".equalsIgnoreCase(uri.getScheme()) && uri.getPort() == 80);
    }

    private boolean isIpLiteral(String host) {
        return host.indexOf(':') >= 0 || host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    }

    private void reject(String reason) {
        metrics.destinationRejected(reason);
        throw new ScannerDestinationRejectedException(reason);
    }

    private record Origin(String scheme, String host, int port) {

        static Origin from(URI uri) {
            return new Origin(
                    uri.getScheme().toLowerCase(Locale.ROOT),
                    uri.getHost().toLowerCase(Locale.ROOT),
                    effectivePort(uri));
        }

        boolean matches(URI uri) {
            return scheme.equalsIgnoreCase(uri.getScheme())
                    && host.equalsIgnoreCase(uri.getHost())
                    && port == effectivePort(uri);
        }

        private static int effectivePort(URI uri) {
            if (uri.getPort() != -1) {
                return uri.getPort();
            }
            return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }
    }
}
