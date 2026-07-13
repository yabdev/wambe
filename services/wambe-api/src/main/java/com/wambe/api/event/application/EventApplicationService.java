package com.wambe.api.event.application;

import com.wambe.api.common.error.ApiException;
import com.wambe.api.common.idempotency.IdempotencyService;
import com.wambe.api.common.rls.RlsContext;
import com.wambe.api.event.api.EventMapper;
import com.wambe.api.event.domain.PublishValidator;
import com.wambe.api.event.persistence.EventEntity;
import com.wambe.api.event.persistence.EventRepository;
import com.wambe.api.generated.model.CreateEventRequest;
import com.wambe.api.generated.model.Event;
import com.wambe.api.generated.model.EventStatus;
import com.wambe.api.generated.model.EventWithSession;
import com.wambe.api.generated.model.ListEvents200Response;
import com.wambe.api.generated.model.PublishEvent200Response;
import com.wambe.api.generated.model.UpdateEventRequest;
import com.wambe.api.generated.model.Venue;
import com.wambe.api.generated.model.Visibility;
import com.wambe.api.media.persistence.MediaEntity;
import com.wambe.api.media.persistence.MediaRepository;
import com.wambe.api.session.persistence.CreationSessionEntity;
import com.wambe.api.session.persistence.CreationSessionRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventApplicationService {

    private final EventRepository events;
    private final MediaRepository media;
    private final CreationSessionRepository sessions;
    private final EventMapper mapper;
    private final PublishValidator publishValidator;
    private final RlsContext rls;
    private final IdempotencyService idempotency;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public EventApplicationService(
            EventRepository events,
            MediaRepository media,
            CreationSessionRepository sessions,
            EventMapper mapper,
            PublishValidator publishValidator,
            RlsContext rls,
            IdempotencyService idempotency,
            JdbcTemplate jdbcTemplate) {
        this(
                events,
                media,
                sessions,
                mapper,
                publishValidator,
                rls,
                idempotency,
                jdbcTemplate,
                Clock.systemUTC());
    }

    EventApplicationService(
            EventRepository events,
            MediaRepository media,
            CreationSessionRepository sessions,
            EventMapper mapper,
            PublishValidator publishValidator,
            RlsContext rls,
            IdempotencyService idempotency,
            JdbcTemplate jdbcTemplate,
            Clock clock) {
        this.events = events;
        this.media = media;
        this.sessions = sessions;
        this.mapper = mapper;
        this.publishValidator = publishValidator;
        this.rls = rls;
        this.idempotency = idempotency;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Transactional
    public EventWithSession create(UUID ownerId, UUID key, CreateEventRequest request) {
        rls.apply(ownerId);
        return idempotency.execute(
                ownerId,
                "POST /events",
                key,
                request,
                HttpStatus.CREATED,
                EventWithSession.class,
                () -> createOnce(ownerId, request));
    }

    @Transactional(readOnly = true)
    public Event get(UUID ownerId, UUID eventId) {
        rls.apply(ownerId);
        EventEntity event = owned(eventId, ownerId);
        return mapper.toApi(event, activeMedia(ownerId, eventId));
    }

    @Transactional(readOnly = true)
    public ListEvents200Response list(
            UUID ownerId,
            EventStatus status,
            String cursor,
            Integer requestedLimit) {
        rls.apply(ownerId);
        int limit = requestedLimit == null ? 20 : Math.max(1, Math.min(requestedLimit, 50));
        int offset = decodeCursor(cursor);
        int page = offset / limit;
        List<Event> items = events.listOwned(
                        ownerId,
                        status == null ? null : status.getValue(),
                        PageRequest.of(page, limit))
                .stream()
                .map(event -> mapper.toApi(event, activeMedia(ownerId, event.getId())))
                .toList();
        var response = new ListEvents200Response().items(items);
        if (items.size() == limit) {
            response.nextCursor(encodeCursor(offset + items.size()));
        }
        return response;
    }

    @Transactional
    public Event update(
            UUID ownerId,
            UUID eventId,
            long externalVersion,
            UUID key,
            UpdateEventRequest request) {
        rls.apply(ownerId);
        return idempotency.execute(
                ownerId,
                "PATCH /events/" + eventId,
                key,
                request,
                HttpStatus.OK,
                Event.class,
                () -> updateOnce(ownerId, eventId, externalVersion, request));
    }

    @Transactional
    public PublishEvent200Response publish(
            UUID ownerId,
            UUID eventId,
            long externalVersion,
            UUID key) {
        rls.apply(ownerId);
        return idempotency.execute(
                ownerId,
                "POST /events/" + eventId + "/publish",
                key,
                java.util.Map.of("eventId", eventId, "version", externalVersion),
                HttpStatus.OK,
                PublishEvent200Response.class,
                () -> publishOnce(ownerId, eventId, externalVersion));
    }

    @Transactional
    public Event unpublish(
            UUID ownerId,
            UUID eventId,
            long externalVersion,
            UUID key) {
        rls.apply(ownerId);
        return idempotency.execute(
                ownerId,
                "POST /events/" + eventId + "/unpublish",
                key,
                java.util.Map.of("eventId", eventId, "version", externalVersion),
                HttpStatus.OK,
                Event.class,
                () -> {
                    EventEntity event = events.lockOwned(eventId, ownerId).orElseThrow(ApiException::notFound);
                    verifyVersion(event, externalVersion);
                    if (!"published".equals(event.getStatus())) {
                        throw new ApiException(
                                HttpStatus.CONFLICT,
                                "INVALID_EVENT_STATE",
                                "Only a published event can be unpublished");
                    }
                    OffsetDateTime now = now();
                    event.unpublish(now);
                    audit(ownerId, eventId, "event_unpublished", "succeeded");
                    events.flush();
                    return mapper.toApi(event, activeMedia(ownerId, eventId));
                });
    }

    @Transactional
    public void delete(UUID ownerId, UUID eventId, UUID key) {
        rls.apply(ownerId);
        idempotency.execute(
                ownerId,
                "DELETE /events/" + eventId,
                key,
                java.util.Map.of("eventId", eventId),
                HttpStatus.NO_CONTENT,
                MutationResult.class,
                () -> {
                    EventEntity event = events.lockOwned(eventId, ownerId).orElseThrow(ApiException::notFound);
                    OffsetDateTime now = now();
                    event.softDelete(now);
                    activeMedia(ownerId, eventId).forEach(item -> item.softDelete(now));
                    audit(ownerId, eventId, "event_deleted", "succeeded");
                    return new MutationResult(true);
                });
    }

    private EventWithSession createOnce(UUID ownerId, CreateEventRequest request) {
        var existing = events.findByOwnerIdAndClientCreationKey(ownerId, request.getClientCreationKey());
        if (existing.isPresent()) {
            CreationSessionEntity session = sessions
                    .findFirstByOwnerIdAndEventIdOrderByOpenedAt(ownerId, existing.get().getId())
                    .orElseThrow();
            return new EventWithSession(
                    mapper.toApi(existing.get(), activeMedia(ownerId, existing.get().getId())),
                    session.getId());
        }
        OffsetDateTime now = now();
        EventEntity event = events.save(EventEntity.draft(ownerId, request.getClientCreationKey(), now));
        CreationSessionEntity session = sessions.save(CreationSessionEntity.open(
                ownerId,
                event.getId(),
                request.getEligibility().getValue(),
                request.getDeviceClass() == null ? null : request.getDeviceClass().getValue(),
                request.getNetworkQuality() == null ? null : request.getNetworkQuality().getValue(),
                now));
        events.flush();
        sessions.flush();
        recordProductEvent(ownerId, event.getId(), session.getId(), "event_creation_started", now, "{}");
        audit(ownerId, event.getId(), "event_created", "succeeded");
        return new EventWithSession(mapper.toApi(event, List.of()), session.getId());
    }

    private Event updateOnce(
            UUID ownerId,
            UUID eventId,
            long externalVersion,
            UpdateEventRequest request) {
        EventEntity event = events.lockOwned(eventId, ownerId).orElseThrow(ApiException::notFound);
        verifyVersion(event, externalVersion);

        Venue venue = valueOrCurrentVenue(request.getVenue(), event);
        String eventType = request.getEventType().isPresent()
                ? enumValue(request.getEventType().orElse(null))
                : event.getEventType();
        String title = valueOrCurrent(request.getTitle(), event.getTitle());
        OffsetDateTime startsAt = valueOrCurrent(request.getStartsAt(), event.getStartsAt());
        String visibility = request.getVisibility().isPresent()
                ? enumValue(request.getVisibility().orElse(null))
                : event.getVisibility();
        String notes = valueOrCurrent(request.getDressCodeNotes(), event.getDressCodeNotes());
        OffsetDateTime now = now();
        event.update(
                eventType,
                title,
                startsAt,
                request.getTimezone() == null ? event.getTimezone() : request.getTimezone(),
                visibility,
                venue == null ? null : nullableValue(venue.getName()),
                venue == null ? null : venue.getDisplayAddress(),
                venue == null ? null : nullableValue(venue.getPlaceId()),
                venue == null ? null : nullableValue(venue.getLatitude()),
                venue == null ? null : nullableValue(venue.getLongitude()),
                venue != null && Boolean.TRUE.equals(venue.getConfirmed()),
                notes,
                now);
        events.flush();
        audit(ownerId, eventId, "draft_saved", "succeeded");
        return mapper.toApi(event, activeMedia(ownerId, eventId));
    }

    private PublishEvent200Response publishOnce(UUID ownerId, UUID eventId, long externalVersion) {
        EventEntity event = events.lockOwned(eventId, ownerId).orElseThrow(ApiException::notFound);
        verifyVersion(event, externalVersion);
        OffsetDateTime now = now();
        boolean hasNonCleanMedia = media.existsByOwnerIdAndEventIdAndStorageStatusIn(
                ownerId,
                eventId,
                List.of("quarantine", "scanning", "rejected"));
        publishValidator.validate(event, hasNonCleanMedia, now);
        event.publish(event.getSlug() == null ? slug() : event.getSlug(), now);
        CreationSessionEntity session = sessions
                .findFirstByOwnerIdAndEventIdOrderByOpenedAt(ownerId, eventId)
                .orElseThrow();
        session.markFirstPublished(now);
        recordProductEvent(ownerId, eventId, session.getId(), "event_publish_succeeded", now,
                "{\"visibility\":\"" + event.getVisibility() + "\"}");
        audit(ownerId, eventId, "event_published", "succeeded");
        events.flush();
        Event responseEvent = mapper.toApi(event, activeMedia(ownerId, eventId));
        var response = new PublishEvent200Response(
                responseEvent,
                mapper.canonicalUrl(event),
                mapper.isShareEligible(event));
        if (!mapper.isShareEligible(event)) {
            response.shareBlockedReason("GUEST_ACCESS_ENFORCEMENT_REQUIRED");
        }
        return response;
    }

    private EventEntity owned(UUID eventId, UUID ownerId) {
        return events.findByIdAndOwnerIdAndStatusNot(eventId, ownerId, "deleted")
                .orElseThrow(ApiException::notFound);
    }

    private List<MediaEntity> activeMedia(UUID ownerId, UUID eventId) {
        return media.findAllByOwnerIdAndEventIdAndStorageStatusNotOrderByCreatedAt(
                ownerId,
                eventId,
                "deleted");
    }

    private void verifyVersion(EventEntity event, long externalVersion) {
        if (event.getVersion() + 1 != externalVersion) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EVENT_VERSION_CONFLICT",
                    "The event was updated elsewhere; reload the current version");
        }
    }

    private Venue valueOrCurrentVenue(JsonNullable<Venue> value, EventEntity event) {
        if (value.isPresent()) {
            return value.orElse(null);
        }
        if (event.getVenueDisplayAddress() == null) {
            return null;
        }
        return new Venue()
                .name(event.getVenueName())
                .displayAddress(event.getVenueDisplayAddress())
                .placeId(event.getVenuePlaceId())
                .latitude(event.getVenueLatitude())
                .longitude(event.getVenueLongitude())
                .confirmed(event.getVenueConfirmedAt() != null);
    }

    private <T> T valueOrCurrent(JsonNullable<T> value, T current) {
        return value.isPresent() ? value.orElse(null) : current;
    }

    private <T> T nullableValue(JsonNullable<T> value) {
        return value == null || !value.isPresent() ? null : value.orElse(null);
    }

    private String enumValue(Object value) {
        return value == null ? null : value.toString();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private String slug() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 22);
    }

    private int decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.US_ASCII));
        } catch (RuntimeException exception) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_CURSOR",
                    "The pagination cursor is invalid");
        }
    }

    private String encodeCursor(int offset) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                Integer.toString(offset).getBytes(StandardCharsets.US_ASCII));
    }

    private void recordProductEvent(
            UUID ownerId,
            UUID eventId,
            UUID sessionId,
            String name,
            OffsetDateTime occurredAt,
            String properties) {
        jdbcTemplate.update("""
                insert into product_events (
                    owner_id, event_id, creation_session_id, name, schema_version,
                    occurred_at, received_at, allowed_properties, consent_category
                ) values (?, ?, ?, ?, '1.0', ?, ?, cast(? as jsonb), 'essential')
                """,
                ownerId,
                eventId,
                sessionId,
                name,
                occurredAt,
                now(),
                properties);
    }

    private void audit(UUID ownerId, UUID resourceId, String action, String outcome) {
        jdbcTemplate.update("""
                insert into audit_log (
                    owner_id, actor, action, resource_type, resource_id, outcome
                ) values (?, 'host', ?, 'event', ?, ?)
                """,
                ownerId,
                action,
                resourceId,
                outcome);
    }

    private record MutationResult(boolean completed) {
    }
}
