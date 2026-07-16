package com.wambe.api.integration.scanner;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import java.io.IOException;
import java.util.Optional;

public class GoogleScannerIdentityTokenProvider implements ScannerIdentityTokenProvider {

    static final String IDENTITY_UNAVAILABLE = "WAMBE_SCANNER_IDENTITY_UNAVAILABLE";

    private final IdTokenCredentials credentials;

    public GoogleScannerIdentityTokenProvider(String audience) throws IOException {
        if (audience == null || audience.isBlank()) {
            throw new IllegalStateException(IDENTITY_UNAVAILABLE);
        }
        GoogleCredentials applicationDefault = GoogleCredentials.getApplicationDefault();
        if (!(applicationDefault instanceof IdTokenProvider tokenProvider)) {
            throw new IllegalStateException(IDENTITY_UNAVAILABLE);
        }
        this.credentials = IdTokenCredentials.newBuilder()
                .setIdTokenProvider(tokenProvider)
                .setTargetAudience(audience)
                .build();
    }

    @Override
    public synchronized Optional<String> token() {
        try {
            credentials.refreshIfExpired();
            AccessToken accessToken = credentials.getAccessToken();
            if (accessToken == null || accessToken.getTokenValue().isBlank()) {
                throw new IllegalStateException(IDENTITY_UNAVAILABLE);
            }
            return Optional.of(accessToken.getTokenValue());
        } catch (IOException exception) {
            throw new IllegalStateException(IDENTITY_UNAVAILABLE, exception);
        }
    }
}
