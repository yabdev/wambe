package com.wambe.scanner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScannerDestinationPolicyTest {

    private static final String SUPABASE = "https://project.supabase.co";
    private static final String API = "https://api.staging.wambe.example";
    private final ScannerMetrics metrics = new ScannerMetrics(new SimpleMeterRegistry());

    private final ScannerDestinationPolicy policy =
            new ScannerDestinationPolicy(SUPABASE, API, false, metrics);

    @Test
    void acceptsExactHttpsStorageAndCallbackDestinations() {
        assertThatCode(() -> policy.validate(request(
                        "https://project.supabase.co/storage/v1/object/sign/quarantine/file?token=read",
                        "https://project.supabase.co:443/storage/v1/object/sign/quarantine/preview?token=write",
                        API + "/api/v1/internal/scanner/callback")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsHostSchemePortUserInfoAndPathBypasses() {
        assertRejected("storage_scheme", request(
                "http://project.supabase.co/storage/v1/object/sign/file",
                validPreview(),
                validCallback()));
        assertRejected("storage_host", request(
                "https://project.supabase.co.evil.example/storage/v1/object/sign/file",
                validPreview(),
                validCallback()));
        assertRejected("storage_uri", request(
                "https://user@project.supabase.co/storage/v1/object/sign/file",
                validPreview(),
                validCallback()));
        assertRejected("storage_uri", request(
                "https://project.supabase.co:444/storage/v1/object/sign/file",
                validPreview(),
                validCallback()));
        assertRejected("storage_path", request(
                "https://project.supabase.co/auth/v1/token",
                validPreview(),
                validCallback()));
    }

    @Test
    void rejectsCallbackOriginPathQueryAndFragment() {
        assertRejected("callback_origin", request(
                validRead(),
                validPreview(),
                "https://evil.example/api/v1/internal/scanner/callback"));
        assertRejected("callback_path", request(
                validRead(),
                validPreview(),
                API + "/api/v1/internal/scanner/other"));
        assertRejected("callback_path", request(
                validRead(),
                validPreview(),
                validCallback() + "?next=evil"));
        assertRejected("callback_path", request(
                validRead(),
                validPreview(),
                validCallback() + "#fragment"));
    }

    @Test
    void rejectsIpLiteralSupabaseConfiguration() {
        assertThatThrownBy(() ->
                        new ScannerDestinationPolicy("https://127.0.0.1", API, false, metrics))
                .isInstanceOf(ScannerDestinationRejectedException.class)
                .extracting(exception -> ((ScannerDestinationRejectedException) exception).reason())
                .isEqualTo("invalid_supabase_origin");
    }

    @Test
    void explicitLocalModeAllowsOnlyExactApiDevStorage() {
        var localPolicy = new ScannerDestinationPolicy("", "http://api:8080", true, metrics);

        assertThatCode(() -> localPolicy.validate(request(
                        "http://api:8080/dev-storage/quarantine/file",
                        "http://api:8080/dev-storage/quarantine/preview",
                        "http://api:8080/api/v1/internal/scanner/callback")))
                .doesNotThrowAnyException();
        assertRejected(localPolicy, "storage_scheme", request(
                "http://localhost:8080/dev-storage/quarantine/file",
                "http://api:8080/dev-storage/quarantine/preview",
                "http://api:8080/api/v1/internal/scanner/callback"));
    }

    private void assertRejected(String reason, ScanRequest request) {
        assertRejected(policy, reason, request);
    }

    private void assertRejected(
            ScannerDestinationPolicy target,
            String reason,
            ScanRequest request) {
        assertThatThrownBy(() -> target.validate(request))
                .isInstanceOf(ScannerDestinationRejectedException.class)
                .extracting(exception -> ((ScannerDestinationRejectedException) exception).reason())
                .isEqualTo(reason);
    }

    private ScanRequest request(String readUrl, String previewUrl, String callbackUrl) {
        return new ScanRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                URI.create(readUrl),
                URI.create(previewUrl),
                "owner/event/media/preview",
                URI.create(callbackUrl),
                "image/png");
    }

    private String validRead() {
        return SUPABASE + "/storage/v1/object/sign/quarantine/file?token=read";
    }

    private String validPreview() {
        return SUPABASE + "/storage/v1/object/sign/quarantine/preview?token=write";
    }

    private String validCallback() {
        return API + "/api/v1/internal/scanner/callback";
    }
}
