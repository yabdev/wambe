package com.wambe.scanner;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DeployedSecurityValidator implements InitializingBean {

    static final String INVALID_SCANNER_SECRET = "WAMBE_DEPLOYED_SCANNER_SECRET_INVALID";
    static final String FORBIDDEN_LOCAL_DESTINATIONS = "WAMBE_DEPLOYED_LOCAL_DESTINATIONS_FORBIDDEN";
    static final String INVALID_ENVELOPE_LIMIT = "WAMBE_DEPLOYED_ENVELOPE_LIMIT_INVALID";
    static final String INVALID_DESTINATION_CONFIG = "WAMBE_DEPLOYED_DESTINATION_CONFIG_INVALID";
    private static final int MIN_SECRET_LENGTH = 32;
    private static final int APPROVED_ENVELOPE_LIMIT = 16_384;
    private static final Set<String> KNOWN_DEVELOPMENT_SECRETS = Set.of(
            "local-scanner-secret-change-me",
            "test-scanner-secret-change-me");

    private final String scannerHmacSecret;
    private final boolean validationRequired;
    private final boolean allowLocalDestinations;
    private final int requestEnvelopeMaxBytes;
    private final String supabaseUrl;
    private final String apiBaseUrl;

    @Autowired
    public DeployedSecurityValidator(
            Environment environment,
            @Value("${wambe.scanner.hmac-secret:}") String scannerHmacSecret,
            @Value("${wambe.scanner.destinations.allow-local:false}") boolean allowLocalDestinations,
            @Value("${wambe.security.request-envelope.max-bytes}") int requestEnvelopeMaxBytes,
            @Value("${wambe.scanner.destinations.supabase-url:}") String supabaseUrl,
            @Value("${wambe.scanner.destinations.api-base-url:}") String apiBaseUrl) {
        this(
                requiresValidation(environment),
                scannerHmacSecret,
                allowLocalDestinations,
                requestEnvelopeMaxBytes,
                supabaseUrl,
                apiBaseUrl);
    }

    DeployedSecurityValidator(String scannerHmacSecret) {
        this(
                true,
                scannerHmacSecret,
                false,
                APPROVED_ENVELOPE_LIMIT,
                "https://project.supabase.co",
                "https://api.staging.wambe.example");
    }

    DeployedSecurityValidator(
            Environment environment,
            String scannerHmacSecret,
            boolean allowLocalDestinations,
            int requestEnvelopeMaxBytes) {
        this(
                requiresValidation(environment),
                scannerHmacSecret,
                allowLocalDestinations,
                requestEnvelopeMaxBytes,
                "https://project.supabase.co",
                "https://api.staging.wambe.example");
    }

    private DeployedSecurityValidator(
            boolean validationRequired,
            String scannerHmacSecret,
            boolean allowLocalDestinations,
            int requestEnvelopeMaxBytes,
            String supabaseUrl,
            String apiBaseUrl) {
        this.validationRequired = validationRequired;
        this.scannerHmacSecret = scannerHmacSecret;
        this.allowLocalDestinations = allowLocalDestinations;
        this.requestEnvelopeMaxBytes = requestEnvelopeMaxBytes;
        this.supabaseUrl = supabaseUrl;
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public void afterPropertiesSet() {
        if (!validationRequired) {
            return;
        }
        if (scannerHmacSecret == null
                || scannerHmacSecret.isBlank()
                || scannerHmacSecret.length() < MIN_SECRET_LENGTH
                || KNOWN_DEVELOPMENT_SECRETS.contains(scannerHmacSecret)) {
            throw new IllegalStateException(INVALID_SCANNER_SECRET);
        }
        if (allowLocalDestinations) {
            throw new IllegalStateException(FORBIDDEN_LOCAL_DESTINATIONS);
        }
        if (requestEnvelopeMaxBytes != APPROVED_ENVELOPE_LIMIT) {
            throw new IllegalStateException(INVALID_ENVELOPE_LIMIT);
        }
        if (!isExactHttpsOrigin(supabaseUrl) || !isExactHttpsOrigin(apiBaseUrl)) {
            throw new IllegalStateException(INVALID_DESTINATION_CONFIG);
        }
    }

    private static boolean requiresValidation(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0
                || Arrays.stream(profiles)
                        .anyMatch(profile -> !"local".equals(profile) && !"test".equals(profile));
    }

    private boolean isExactHttpsOrigin(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            String path = uri.getRawPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && (path == null || path.isEmpty() || "/".equals(path))
                    && !isLocalOrIpLiteral(host);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isLocalOrIpLiteral(String host) {
        String normalized = host.toLowerCase();
        return "localhost".equals(normalized)
                || normalized.endsWith(".local")
                || normalized.endsWith(".internal")
                || normalized.indexOf(':') >= 0
                || normalized.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    }
}
