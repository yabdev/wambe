package com.wambe.api.scanner.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scan_jobs")
public class ScanJobEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID mediaId;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private OffsetDateTime nextAttemptAt;

    private OffsetDateTime leaseExpiresAt;
    private String lastErrorCode;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected ScanJobEntity() {
    }

    public static ScanJobEntity pending(UUID ownerId, UUID mediaId, OffsetDateTime now) {
        var job = new ScanJobEntity();
        job.id = UUID.randomUUID();
        job.ownerId = ownerId;
        job.mediaId = mediaId;
        job.status = "pending";
        job.nextAttemptAt = now;
        job.createdAt = now;
        job.updatedAt = now;
        return job;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMediaId() {
        return mediaId;
    }
}
