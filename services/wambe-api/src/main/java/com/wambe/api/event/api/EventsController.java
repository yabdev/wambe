package com.wambe.api.event.api;

import com.wambe.api.common.security.CurrentOwner;
import com.wambe.api.event.application.EventApplicationService;
import com.wambe.api.generated.api.EventsApi;
import com.wambe.api.generated.model.CreateEventRequest;
import com.wambe.api.generated.model.CreateMediaIntent201Response;
import com.wambe.api.generated.model.CreateMediaIntentRequest;
import com.wambe.api.generated.model.Event;
import com.wambe.api.generated.model.EventStatus;
import com.wambe.api.generated.model.EventWithSession;
import com.wambe.api.generated.model.ListEvents200Response;
import com.wambe.api.generated.model.ListMedia200Response;
import com.wambe.api.generated.model.Media;
import com.wambe.api.generated.model.PublishEvent200Response;
import com.wambe.api.generated.model.UpdateEventRequest;
import com.wambe.api.media.application.MediaApplicationService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventsController implements EventsApi {

    private final CurrentOwner currentOwner;
    private final EventApplicationService events;
    private final MediaApplicationService media;

    public EventsController(
            CurrentOwner currentOwner,
            EventApplicationService events,
            MediaApplicationService media) {
        this.currentOwner = currentOwner;
        this.events = events;
        this.media = media;
    }

    @Override
    public ResponseEntity<Media> _completeMediaUpload(
            UUID eventId,
            UUID mediaId,
            UUID idempotencyKey) {
        return ResponseEntity.accepted()
                .body(media.complete(currentOwner.requireId(), eventId, mediaId, idempotencyKey));
    }

    @Override
    public ResponseEntity<EventWithSession> _createEvent(
            UUID idempotencyKey,
            CreateEventRequest createEventRequest) {
        return ResponseEntity.status(201)
                .body(events.create(currentOwner.requireId(), idempotencyKey, createEventRequest));
    }

    @Override
    public ResponseEntity<CreateMediaIntent201Response> _createMediaIntent(
            UUID eventId,
            UUID idempotencyKey,
            CreateMediaIntentRequest createMediaIntentRequest) {
        return ResponseEntity.status(201)
                .body(media.createIntent(
                        currentOwner.requireId(),
                        eventId,
                        idempotencyKey,
                        createMediaIntentRequest));
    }

    @Override
    public ResponseEntity<Void> _deleteEvent(UUID idempotencyKey, UUID eventId) {
        events.delete(currentOwner.requireId(), eventId, idempotencyKey);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> _deleteMedia(UUID eventId, UUID mediaId, UUID idempotencyKey) {
        media.delete(currentOwner.requireId(), eventId, mediaId, idempotencyKey);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Event> _getEvent(UUID eventId) {
        return ResponseEntity.ok(events.get(currentOwner.requireId(), eventId));
    }

    @Override
    public ResponseEntity<ListEvents200Response> _listEvents(
            EventStatus status,
            String cursor,
            Integer limit) {
        return ResponseEntity.ok(events.list(currentOwner.requireId(), status, cursor, limit));
    }

    @Override
    public ResponseEntity<ListMedia200Response> _listMedia(UUID eventId) {
        return ResponseEntity.ok(media.list(currentOwner.requireId(), eventId));
    }

    @Override
    public ResponseEntity<PublishEvent200Response> _publishEvent(
            UUID eventId,
            UUID idempotencyKey,
            String ifMatch) {
        return ResponseEntity.ok(events.publish(
                currentOwner.requireId(),
                eventId,
                Long.parseLong(ifMatch),
                idempotencyKey));
    }

    @Override
    public ResponseEntity<Event> _unpublishEvent(
            UUID eventId,
            UUID idempotencyKey,
            String ifMatch) {
        return ResponseEntity.ok(events.unpublish(
                currentOwner.requireId(),
                eventId,
                Long.parseLong(ifMatch),
                idempotencyKey));
    }

    @Override
    public ResponseEntity<Event> _updateEvent(
            UUID idempotencyKey,
            String ifMatch,
            UUID eventId,
            UpdateEventRequest updateEventRequest) {
        return ResponseEntity.ok(events.update(
                currentOwner.requireId(),
                eventId,
                Long.parseLong(ifMatch),
                idempotencyKey,
                updateEventRequest));
    }
}
