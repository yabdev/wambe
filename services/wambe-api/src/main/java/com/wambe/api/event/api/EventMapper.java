package com.wambe.api.event.api;

import com.wambe.api.event.persistence.EventEntity;
import com.wambe.api.generated.model.Event;
import com.wambe.api.generated.model.EventStatus;
import com.wambe.api.generated.model.Media;
import com.wambe.api.generated.model.MediaRole;
import com.wambe.api.generated.model.MediaStatus;
import com.wambe.api.generated.model.Venue;
import com.wambe.api.generated.model.Visibility;
import com.wambe.api.integration.storage.ObjectStoragePort;
import com.wambe.api.media.persistence.MediaEntity;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    private final ObjectStoragePort storage;
    private final String publicBaseUrl;

    public EventMapper(
            ObjectStoragePort storage,
            @Value("${wambe.public-base-url}") String publicBaseUrl) {
        this.storage = storage;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    public Event toApi(EventEntity entity, List<MediaEntity> media) {
        Event response = new Event()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .status(EventStatus.fromValue(entity.getStatus()))
                .version(Math.toIntExact(entity.getVersion() + 1))
                .timezone(entity.getTimezone())
                .shareEligible(isShareEligible(entity))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .media(media.stream().map(this::toApi).toList());
        response.eventType(entity.getEventType());
        response.title(entity.getTitle());
        response.startsAt(entity.getStartsAt());
        response.visibility(entity.getVisibility() == null
                ? null
                : Visibility.fromValue(entity.getVisibility()));
        response.dressCodeNotes(entity.getDressCodeNotes());
        response.slug(entity.getSlug());
        response.publishedAt(entity.getPublishedAt());
        response.lastSavedAt(entity.getLastSavedAt());
        if (entity.getSlug() != null) {
            response.canonicalUrl(URI.create(publicBaseUrl + "/e/" + entity.getSlug()));
        }
        if (!isShareEligible(entity) && "published".equals(entity.getStatus())) {
            response.shareBlockedReason("GUEST_ACCESS_ENFORCEMENT_REQUIRED");
        }
        if (entity.getVenueDisplayAddress() != null) {
            response.venue(new Venue()
                    .name(entity.getVenueName())
                    .displayAddress(entity.getVenueDisplayAddress())
                    .placeId(entity.getVenuePlaceId())
                    .latitude(entity.getVenueLatitude())
                    .longitude(entity.getVenueLongitude())
                    .confirmed(entity.getVenueConfirmedAt() != null));
        }
        return response;
    }

    public Media toApi(MediaEntity entity) {
        Media response = new Media()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .role(MediaRole.fromValue(entity.getRole()))
                .filename(entity.getFilename())
                .claimedMimeType(entity.getClaimedMimeType())
                .sizeBytes(Math.toIntExact(entity.getSizeBytes()))
                .status(MediaStatus.fromValue(entity.getStorageStatus()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());
        response.detectedMimeType(entity.getDetectedMimeType());
        response.rejectionCode(entity.getRejectionCode());
        if (entity.getPreviewPath() != null && "active".equals(entity.getStorageStatus())) {
            response.previewUrl(storage.signedGetActive(
                    entity.getPreviewPath(), Duration.ofMinutes(10)));
        }
        return response;
    }

    public URI canonicalUrl(EventEntity event) {
        return URI.create(publicBaseUrl + "/e/" + event.getSlug());
    }

    public boolean isShareEligible(EventEntity event) {
        return "published".equals(event.getStatus())
                && ("public".equals(event.getVisibility()) || "private_link".equals(event.getVisibility()));
    }
}
