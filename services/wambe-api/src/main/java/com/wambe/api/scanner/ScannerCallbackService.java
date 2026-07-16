package com.wambe.api.scanner;

import com.wambe.api.common.error.ApiException;
import com.wambe.api.common.rls.RlsContext;
import com.wambe.api.generated.model.ScannerCallbackRequest;
import com.wambe.api.integration.storage.ObjectStoragePort;
import com.wambe.api.media.persistence.MediaEntity;
import com.wambe.api.media.persistence.MediaRepository;
import com.wambe.api.observability.WambeMetrics;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScannerCallbackService {

    private final JdbcTemplate jdbcTemplate;
    private final MediaRepository media;
    private final ObjectStoragePort storage;
    private final RlsContext rls;
    private final WambeMetrics metrics;

    public ScannerCallbackService(
            JdbcTemplate jdbcTemplate,
            MediaRepository media,
            ObjectStoragePort storage,
            RlsContext rls,
            WambeMetrics metrics) {
        this.jdbcTemplate = jdbcTemplate;
        this.media = media;
        this.storage = storage;
        this.rls = rls;
        this.metrics = metrics;
    }

    @Transactional
    public void accept(
            OffsetDateTime requestTimestamp,
            UUID nonce,
            String bodyDigest,
            ScannerCallbackRequest request) {
        UUID ownerId = ownerFor(request.getMediaId());
        rls.apply(ownerId);
        int inserted = jdbcTemplate.update("""
                insert into scanner_callback_nonces (
                    nonce, media_id, owner_id, request_timestamp,
                    body_digest, consumed_at, expires_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                on conflict do nothing
                """,
                nonce,
                request.getMediaId(),
                ownerId,
                requestTimestamp,
                bodyDigest,
                now(),
                now().plusHours(24));
        if (inserted == 0) {
            String existingDigest = jdbcTemplate.queryForObject(
                    "select body_digest from scanner_callback_nonces where nonce = ?",
                    String.class,
                    nonce);
            if (bodyDigest.equals(existingDigest)) {
                return;
            }
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SCANNER_NONCE_REPLAY",
                    "The scanner callback nonce was already consumed");
        }

        MediaEntity entity = media.findByIdAndOwnerId(request.getMediaId(), ownerId)
                .orElseThrow(ApiException::notFound);
        validatePreviewPath(entity, request);
        if (isTerminal(entity)) {
            ensureMatchingTerminalResult(entity, request);
            return;
        }
        String result = request.getResult().getValue();
        if ("clean".equals(result)) {
            activate(entity, request);
        } else {
            String rejection = request.getRejectionCode().isPresent()
                    ? request.getRejectionCode().get().getValue()
                    : "scan_failed";
            entity.reject(request.getDetectedMimeType().getValue(), rejection, now());
            storage.delete(entity.getQuarantinePath());
        }
        metrics.scanResult(result);
        jdbcTemplate.update("""
                update scan_jobs
                   set status = 'completed', lease_expires_at = null, updated_at = ?
                 where media_id = ? and owner_id = ?
                """,
                now(),
                entity.getId(),
                ownerId);
    }

    private void validatePreviewPath(MediaEntity entity, ScannerCallbackRequest request) {
        String suppliedPath = request.getPreviewQuarantinePath();
        String expectedPath = entity.getQuarantinePath() + ".preview";
        if (suppliedPath != null && !expectedPath.equals(suppliedPath)) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_PREVIEW_PATH",
                    "The preview path does not belong to this media object");
        }
    }

    private boolean isTerminal(MediaEntity entity) {
        return "active".equals(entity.getStorageStatus())
                || "rejected".equals(entity.getStorageStatus());
    }

    private void ensureMatchingTerminalResult(
            MediaEntity entity,
            ScannerCallbackRequest request) {
        boolean matches;
        if ("active".equals(entity.getStorageStatus())) {
            String expectedPreviewPath = request.getPreviewQuarantinePath() == null
                    ? null
                    : entity.getOwnerId() + "/" + entity.getEventId() + "/"
                            + entity.getId() + "/preview";
            matches = request.getResult() == ScannerCallbackRequest.ResultEnum.CLEAN
                    && Objects.equals(
                            entity.getDetectedMimeType(),
                            request.getDetectedMimeType().getValue())
                    && Objects.equals(entity.getObjectSha256(), request.getObjectSha256())
                    && Objects.equals(entity.getPreviewPath(), expectedPreviewPath)
                    && Objects.equals(entity.getPreviewSha256(), request.getPreviewSha256());
        } else {
            String rejection = request.getRejectionCode().isPresent()
                    ? request.getRejectionCode().get().getValue()
                    : "scan_failed";
            matches = request.getResult() != ScannerCallbackRequest.ResultEnum.CLEAN
                    && Objects.equals(
                            entity.getDetectedMimeType(),
                            request.getDetectedMimeType().getValue())
                    && Objects.equals(entity.getRejectionCode(), rejection);
        }
        if (!matches) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SCANNER_TERMINAL_STATE_CONFLICT",
                    "The media object already has a different terminal scan result");
        }
    }

    private void activate(MediaEntity entity, ScannerCallbackRequest request) {
        String activePath = entity.getOwnerId() + "/" + entity.getEventId() + "/"
                + entity.getId() + "/original";
        storage.promote(entity.getQuarantinePath(), activePath);
        String previewPath = null;
        if (request.getPreviewQuarantinePath() != null) {
            previewPath = entity.getOwnerId() + "/" + entity.getEventId() + "/"
                    + entity.getId() + "/preview";
            storage.promote(request.getPreviewQuarantinePath(), previewPath);
        }
        entity.activate(
                request.getDetectedMimeType().getValue(),
                activePath,
                previewPath,
                request.getObjectSha256(),
                request.getPreviewSha256(),
                now());
    }

    private UUID ownerFor(UUID mediaId) {
        List<UUID> owners = jdbcTemplate.query(
                "select scanner_media_owner(?)",
                (resultSet, rowNum) -> resultSet.getObject(1, UUID.class),
                mediaId);
        if (owners.isEmpty() || owners.getFirst() == null) {
            throw ApiException.notFound();
        }
        return owners.getFirst();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
