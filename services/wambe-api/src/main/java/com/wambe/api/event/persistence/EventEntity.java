package com.wambe.api.event.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    private UUID clientCreationKey;
    private String eventType;
    private String title;
    private OffsetDateTime startsAt;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private String status;

    private String visibility;
    private String venueName;
    private String venueDisplayAddress;
    private String venuePlaceId;
    private BigDecimal venueLatitude;
    private BigDecimal venueLongitude;
    private OffsetDateTime venueConfirmedAt;

    @Column(columnDefinition = "text")
    private String dressCodeNotes;

    private String slug;

    @Version
    private long version;

    private OffsetDateTime publishedAt;
    private OffsetDateTime unpublishedAt;
    private OffsetDateTime deletedAt;
    private OffsetDateTime lastSavedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected EventEntity() {
    }

    public static EventEntity draft(UUID ownerId, UUID clientCreationKey, OffsetDateTime now) {
        var event = new EventEntity();
        event.id = UUID.randomUUID();
        event.ownerId = ownerId;
        event.clientCreationKey = clientCreationKey;
        event.timezone = "Africa/Lagos";
        event.status = "draft";
        event.createdAt = now;
        event.updatedAt = now;
        event.lastSavedAt = now;
        return event;
    }

    public void update(
            String eventType,
            String title,
            OffsetDateTime startsAt,
            String timezone,
            String visibility,
            String venueName,
            String venueDisplayAddress,
            String venuePlaceId,
            BigDecimal venueLatitude,
            BigDecimal venueLongitude,
            boolean venueConfirmed,
            String dressCodeNotes,
            OffsetDateTime now) {
        this.eventType = eventType;
        this.title = title;
        this.startsAt = startsAt;
        this.timezone = timezone == null ? "Africa/Lagos" : timezone;
        this.visibility = visibility;
        this.venueName = venueName;
        this.venueDisplayAddress = venueDisplayAddress;
        this.venuePlaceId = venuePlaceId;
        this.venueLatitude = venueLatitude;
        this.venueLongitude = venueLongitude;
        this.venueConfirmedAt = venueConfirmed ? now : null;
        this.dressCodeNotes = dressCodeNotes;
        this.lastSavedAt = now;
        this.updatedAt = now;
    }

    public void publish(String assignedSlug, OffsetDateTime now) {
        if (slug == null) {
            slug = assignedSlug;
            publishedAt = now;
        }
        status = "published";
        unpublishedAt = null;
        updatedAt = now;
    }

    public void unpublish(OffsetDateTime now) {
        status = "unpublished";
        unpublishedAt = now;
        updatedAt = now;
    }

    public void softDelete(OffsetDateTime now) {
        status = "deleted";
        deletedAt = now;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getClientCreationKey() {
        return clientCreationKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getStatus() {
        return status;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getVenueName() {
        return venueName;
    }

    public String getVenueDisplayAddress() {
        return venueDisplayAddress;
    }

    public String getVenuePlaceId() {
        return venuePlaceId;
    }

    public BigDecimal getVenueLatitude() {
        return venueLatitude;
    }

    public BigDecimal getVenueLongitude() {
        return venueLongitude;
    }

    public OffsetDateTime getVenueConfirmedAt() {
        return venueConfirmedAt;
    }

    public String getDressCodeNotes() {
        return dressCodeNotes;
    }

    public String getSlug() {
        return slug;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public OffsetDateTime getUnpublishedAt() {
        return unpublishedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public OffsetDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
