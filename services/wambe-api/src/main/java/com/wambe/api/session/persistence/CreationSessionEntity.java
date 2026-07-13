package com.wambe.api.session.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "creation_sessions")
public class CreationSessionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private OffsetDateTime openedAt;

    private OffsetDateTime firstPublishedAt;

    @Column(nullable = false)
    private String eligibility;

    @Column(nullable = false)
    private String deviceClass;

    @Column(nullable = false)
    private String networkQuality;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String milestoneSummary;

    protected CreationSessionEntity() {
    }

    public static CreationSessionEntity open(
            UUID ownerId,
            UUID eventId,
            String eligibility,
            String deviceClass,
            String networkQuality,
            OffsetDateTime now) {
        var session = new CreationSessionEntity();
        session.id = UUID.randomUUID();
        session.ownerId = ownerId;
        session.eventId = eventId;
        session.eligibility = eligibility;
        session.deviceClass = deviceClass == null ? "unknown" : deviceClass;
        session.networkQuality = networkQuality == null ? "unknown" : networkQuality;
        session.openedAt = now;
        session.milestoneSummary = "{}";
        return session;
    }

    public void markFirstPublished(OffsetDateTime publishedAt) {
        if (firstPublishedAt == null) {
            firstPublishedAt = publishedAt;
        }
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

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getFirstPublishedAt() {
        return firstPublishedAt;
    }

    public String getEligibility() {
        return eligibility;
    }
}
