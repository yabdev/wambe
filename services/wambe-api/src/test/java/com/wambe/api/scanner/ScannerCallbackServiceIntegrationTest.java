package com.wambe.api.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wambe.api.common.error.ApiException;
import com.wambe.api.PostgresIntegrationTest;
import com.wambe.api.generated.model.ScannerCallbackRequest;
import com.wambe.api.integration.storage.LocalObjectStorage;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class ScannerCallbackServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ScannerCallbackService callbacks;

    @Autowired
    private LocalObjectStorage storage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void promotesCleanMediaAndTreatsIdenticalNonceReplayAsSuccess() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        String objectPath = ownerId + "/" + eventId + "/" + mediaId + "/original";
        transactions.executeWithoutResult(status -> {
            setOwner(ownerId);
            jdbcTemplate.update("""
                    insert into events (id, owner_id, status, timezone, last_saved_at)
                    values (?, ?, 'draft', 'Africa/Lagos', now())
                    """,
                    eventId,
                    ownerId);
            jdbcTemplate.update("""
                    insert into event_media (
                        id, event_id, owner_id, role, filename, claimed_mime_type,
                        detected_mime_type, size_bytes, quarantine_path, storage_status
                    ) values (?, ?, ?, 'invitation', 'invite.png', 'image/png',
                              null, 4, ?, 'scanning')
                    """,
                    mediaId,
                    eventId,
                    ownerId,
                    objectPath);
            jdbcTemplate.update("""
                    insert into scan_jobs (media_id, owner_id, status)
                    values (?, ?, 'leased')
                    """,
                    mediaId,
                    ownerId);
        });
        var quarantine = storage.quarantinePath(objectPath);
        Files.createDirectories(quarantine.getParent());
        Files.write(quarantine, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});

        var request = new ScannerCallbackRequest(
                mediaId,
                ScannerCallbackRequest.ResultEnum.CLEAN,
                ScannerCallbackRequest.DetectedMimeTypeEnum.IMAGE_PNG,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        UUID nonce = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);

        callbacks.accept(timestamp, nonce, "digest", request);
        assertThatCode(() -> callbacks.accept(timestamp, nonce, "digest", request))
                .doesNotThrowAnyException();
        assertThatCode(() -> callbacks.accept(
                        timestamp, UUID.randomUUID(), "fresh-digest", request))
                .doesNotThrowAnyException();

        var conflictingRequest = new ScannerCallbackRequest(
                        mediaId,
                        ScannerCallbackRequest.ResultEnum.REJECTED,
                        ScannerCallbackRequest.DetectedMimeTypeEnum.IMAGE_PNG,
                        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .rejectionCode(ScannerCallbackRequest.RejectionCodeEnum.MALWARE_DETECTED);
        assertThatThrownBy(() -> callbacks.accept(
                        timestamp, UUID.randomUUID(), "conflicting-digest", conflictingRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("different terminal scan result");

        String status = transactions.execute(transaction -> {
            setOwner(ownerId);
            return jdbcTemplate.queryForObject(
                    "select storage_status from event_media where id = ?",
                    String.class,
                    mediaId);
        });
        assertThat(status).isEqualTo("active");
        assertThat(Files.exists(quarantine)).isFalse();
    }

    private void setOwner(UUID ownerId) {
        jdbcTemplate.queryForObject(
                "select set_config('app.current_user_id', ?, true)",
                String.class,
                ownerId.toString());
    }
}
