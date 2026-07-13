package com.wambe.api.media.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_media")
public class MediaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String claimedMimeType;

    private String detectedMimeType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String quarantinePath;

    private String activePath;
    private String previewPath;

    @Column(nullable = false)
    private String storageStatus;

    private String rejectionCode;
    private String objectSha256;
    private String previewSha256;
    private OffsetDateTime deletedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected MediaEntity() {
    }

    public static MediaEntity quarantine(
            UUID ownerId,
            UUID eventId,
            String role,
            String filename,
            String claimedMimeType,
            long sizeBytes,
            OffsetDateTime now) {
        var media = new MediaEntity();
        media.id = UUID.randomUUID();
        media.ownerId = ownerId;
        media.eventId = eventId;
        media.role = role;
        media.filename = filename;
        media.claimedMimeType = claimedMimeType;
        media.sizeBytes = sizeBytes;
        media.quarantinePath = ownerId + "/" + eventId + "/" + media.id + "/original";
        media.storageStatus = "quarantine";
        media.createdAt = now;
        media.updatedAt = now;
        return media;
    }

    public void markScanning(OffsetDateTime now) {
        storageStatus = "scanning";
        updatedAt = now;
    }

    public void activate(
            String detectedMimeType,
            String activePath,
            String previewPath,
            String objectSha256,
            String previewSha256,
            OffsetDateTime now) {
        this.detectedMimeType = detectedMimeType;
        this.activePath = activePath;
        this.previewPath = previewPath;
        this.objectSha256 = objectSha256;
        this.previewSha256 = previewSha256;
        this.storageStatus = "active";
        this.rejectionCode = null;
        this.updatedAt = now;
    }

    public void reject(String detectedMimeType, String rejectionCode, OffsetDateTime now) {
        this.detectedMimeType = detectedMimeType;
        this.storageStatus = "rejected";
        this.rejectionCode = rejectionCode;
        this.updatedAt = now;
    }

    public void softDelete(OffsetDateTime now) {
        storageStatus = "deleted";
        deletedAt = now;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getRole() {
        return role;
    }

    public String getFilename() {
        return filename;
    }

    public String getClaimedMimeType() {
        return claimedMimeType;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getQuarantinePath() {
        return quarantinePath;
    }

    public String getActivePath() {
        return activePath;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public String getStorageStatus() {
        return storageStatus;
    }

    public String getRejectionCode() {
        return rejectionCode;
    }

    public String getObjectSha256() {
        return objectSha256;
    }

    public String getPreviewSha256() {
        return previewSha256;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
