package com.wambe.api.config;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DeployedSecurityValidator implements InitializingBean {

    static final String INVALID_SCANNER_SECRET = "WAMBE_DEPLOYED_SCANNER_SECRET_INVALID";
    static final String FORBIDDEN_JOB_KEY = "WAMBE_DEPLOYED_INTERNAL_JOB_KEY_FORBIDDEN";
    static final String INVALID_SCANNER_IDENTITY_CONFIG = "WAMBE_DEPLOYED_SCANNER_IDENTITY_INVALID";
    static final String INVALID_ENVELOPE_LIMIT = "WAMBE_DEPLOYED_ENVELOPE_LIMIT_INVALID";
    static final String INVALID_STORAGE = "WAMBE_DEPLOYED_STORAGE_INVALID";
    static final String FORBIDDEN_PROFILE = "WAMBE_DEPLOYED_PROFILE_FORBIDDEN";
    private static final int MIN_SECRET_LENGTH = 32;
    private static final int APPROVED_ENVELOPE_LIMIT = 16_384;
    private static final Set<String> KNOWN_DEVELOPMENT_SECRETS = Set.of(
            "local-scanner-secret-change-me",
            "test-scanner-secret-change-me");
    private static final Set<String> FORBIDDEN_DEPLOYED_PROFILES =
            Set.of("local", "test", "development");

    private final List<String> activeProfiles;
    private final String scannerHmacSecret;
    private final String internalJobKey;
    private final String scannerUrl;
    private final String scannerAudience;
    private final String storageType;
    private final boolean validationRequired;
    private final int requestEnvelopeMaxBytes;

    @Autowired
    public DeployedSecurityValidator(
            Environment environment,
            @Value("${wambe.scanner.hmac-secret:}") String scannerHmacSecret,
            @Value("${wambe.internal-jobs.local-key:}") String internalJobKey,
            @Value("${wambe.scanner.url:}") String scannerUrl,
            @Value("${wambe.scanner.audience:}") String scannerAudience,
            @Value("${wambe.security.request-envelope.max-bytes}") int requestEnvelopeMaxBytes,
            @Value("${wambe.storage.type:local}") String storageType) {
        this(
                environment.getActiveProfiles(),
                scannerHmacSecret,
                internalJobKey,
                scannerUrl,
                scannerAudience,
                requestEnvelopeMaxBytes,
                storageType);
    }

    DeployedSecurityValidator(
            String scannerHmacSecret,
            String internalJobKey,
            String scannerUrl,
            String scannerAudience) {
        this(
                new String[0],
                scannerHmacSecret,
                internalJobKey,
                scannerUrl,
                scannerAudience,
                APPROVED_ENVELOPE_LIMIT,
                "supabase");
    }

    private DeployedSecurityValidator(
            String[] activeProfiles,
            String scannerHmacSecret,
            String internalJobKey,
            String scannerUrl,
            String scannerAudience,
            int requestEnvelopeMaxBytes,
            String storageType) {
        this.activeProfiles = List.copyOf(Arrays.asList(activeProfiles));
        this.validationRequired = requiresValidation(activeProfiles);
        this.scannerHmacSecret = scannerHmacSecret;
        this.internalJobKey = internalJobKey;
        this.scannerUrl = scannerUrl;
        this.scannerAudience = scannerAudience;
        this.requestEnvelopeMaxBytes = requestEnvelopeMaxBytes;
        this.storageType = storageType;
    }

    @Override
    public void afterPropertiesSet() {
        if (!validationRequired) {
            return;
        }
        if (activeProfiles.stream().anyMatch(FORBIDDEN_DEPLOYED_PROFILES::contains)) {
            throw new IllegalStateException(FORBIDDEN_PROFILE);
        }
        if (!"supabase".equals(storageType)) {
            throw new IllegalStateException(INVALID_STORAGE);
        }
        if (scannerHmacSecret == null
                || scannerHmacSecret.isBlank()
                || scannerHmacSecret.length() < MIN_SECRET_LENGTH
                || KNOWN_DEVELOPMENT_SECRETS.contains(scannerHmacSecret)) {
            throw new IllegalStateException(INVALID_SCANNER_SECRET);
        }
        if (internalJobKey != null && !internalJobKey.isBlank()) {
            throw new IllegalStateException(FORBIDDEN_JOB_KEY);
        }
        if (!normalizedCloudRunUrl(scannerUrl).equals(normalizedCloudRunUrl(scannerAudience))) {
            throw new IllegalStateException(INVALID_SCANNER_IDENTITY_CONFIG);
        }
        if (requestEnvelopeMaxBytes != APPROVED_ENVELOPE_LIMIT) {
            throw new IllegalStateException(INVALID_ENVELOPE_LIMIT);
        }
    }

    private static boolean requiresValidation(String[] profiles) {
        return profiles.length == 0
                || Arrays.stream(profiles)
                        .anyMatch(profile -> !"local".equals(profile) && !"test".equals(profile));
    }

    private String normalizedCloudRunUrl(String value) {
        try {
            if (value == null || value.isBlank() || !value.equals(value.trim())) {
                throw new IllegalArgumentException();
            }
            URI uri = URI.create(value);
            String path = uri.getRawPath();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getPort() != -1
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || !(path == null || path.isEmpty() || "/".equals(path))) {
                throw new IllegalArgumentException();
            }
            return "https://" + uri.getHost().toLowerCase();
        } catch (RuntimeException exception) {
            throw new IllegalStateException(INVALID_SCANNER_IDENTITY_CONFIG);
        }
    }
}
