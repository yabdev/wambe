package com.wambe.api.media.application;

import com.wambe.api.common.error.ApiException;
import com.wambe.api.common.idempotency.IdempotencyService;
import com.wambe.api.common.rls.RlsContext;
import com.wambe.api.event.api.EventMapper;
import com.wambe.api.event.persistence.EventRepository;
import com.wambe.api.generated.model.CreateMediaIntent201Response;
import com.wambe.api.generated.model.CreateMediaIntentRequest;
import com.wambe.api.generated.model.ListMedia200Response;
import com.wambe.api.generated.model.Media;
import com.wambe.api.integration.storage.ObjectStoragePort;
import com.wambe.api.media.persistence.MediaEntity;
import com.wambe.api.media.persistence.MediaRepository;
import com.wambe.api.scanner.persistence.ScanJobEntity;
import com.wambe.api.scanner.persistence.ScanJobRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaApplicationService {

    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private final EventRepository events;
    private final MediaRepository media;
    private final ScanJobRepository scanJobs;
    private final ObjectStoragePort storage;
    private final EventMapper mapper;
    private final RlsContext rls;
    private final IdempotencyService idempotency;

    public MediaApplicationService(
            EventRepository events,
            MediaRepository media,
            ScanJobRepository scanJobs,
            ObjectStoragePort storage,
            EventMapper mapper,
            RlsContext rls,
            IdempotencyService idempotency) {
        this.events = events;
        this.media = media;
        this.scanJobs = scanJobs;
        this.storage = storage;
        this.mapper = mapper;
        this.rls = rls;
        this.idempotency = idempotency;
    }

    @Transactional
    public CreateMediaIntent201Response createIntent(
            UUID ownerId,
            UUID eventId,
            UUID key,
            CreateMediaIntentRequest request) {
        rls.apply(ownerId);
        requireEvent(ownerId, eventId);
        return idempotency.execute(
                ownerId,
                "POST /events/" + eventId + "/media/intents",
                key,
                request,
                HttpStatus.CREATED,
                CreateMediaIntent201Response.class,
                () -> {
                    String mime = request.getClaimedMimeType().getValue();
                    if (!ALLOWED_MIME_TYPES.contains(mime)) {
                        throw new ApiException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "UNSUPPORTED_MEDIA_TYPE",
                                "Use JPG, PNG, WebP, or PDF");
                    }
                    OffsetDateTime now = now();
                    MediaEntity entity = media.save(MediaEntity.quarantine(
                            ownerId,
                            eventId,
                            request.getRole().getValue(),
                            request.getFilename(),
                            mime,
                            request.getSizeBytes(),
                            now));
                    Duration ttl = Duration.ofMinutes(10);
                    return new CreateMediaIntent201Response(
                            mapper.toApi(entity),
                            storage.signedPut(entity.getQuarantinePath(), ttl, entity.getSizeBytes()),
                            now.plus(ttl));
                });
    }

    @Transactional
    public Media complete(UUID ownerId, UUID eventId, UUID mediaId, UUID key) {
        rls.apply(ownerId);
        requireEvent(ownerId, eventId);
        return idempotency.execute(
                ownerId,
                "POST /events/" + eventId + "/media/" + mediaId + "/complete",
                key,
                java.util.Map.of("mediaId", mediaId),
                HttpStatus.ACCEPTED,
                Media.class,
                () -> {
                    MediaEntity entity = ownedMedia(ownerId, eventId, mediaId);
                    if ("scanning".equals(entity.getStorageStatus())
                            || "active".equals(entity.getStorageStatus())) {
                        return mapper.toApi(entity);
                    }
                    if (!storage.exists(entity.getQuarantinePath(), entity.getSizeBytes())) {
                        throw new ApiException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "UPLOAD_NOT_FOUND",
                                "The uploaded file could not be verified");
                    }
                    OffsetDateTime now = now();
                    entity.markScanning(now);
                    if (scanJobs.findByMediaId(mediaId).isEmpty()) {
                        scanJobs.save(ScanJobEntity.pending(ownerId, mediaId, now));
                    }
                    return mapper.toApi(entity);
                });
    }

    @Transactional(readOnly = true)
    public ListMedia200Response list(UUID ownerId, UUID eventId) {
        rls.apply(ownerId);
        requireEvent(ownerId, eventId);
        List<Media> items = media
                .findAllByOwnerIdAndEventIdAndStorageStatusNotOrderByCreatedAt(
                        ownerId,
                        eventId,
                        "deleted")
                .stream()
                .map(mapper::toApi)
                .toList();
        return new ListMedia200Response(items);
    }

    @Transactional
    public void delete(UUID ownerId, UUID eventId, UUID mediaId, UUID key) {
        rls.apply(ownerId);
        requireEvent(ownerId, eventId);
        idempotency.execute(
                ownerId,
                "DELETE /events/" + eventId + "/media/" + mediaId,
                key,
                java.util.Map.of("mediaId", mediaId),
                HttpStatus.NO_CONTENT,
                MutationResult.class,
                () -> {
                    MediaEntity entity = ownedMedia(ownerId, eventId, mediaId);
                    entity.softDelete(now());
                    storage.delete(entity.getQuarantinePath());
                    if (entity.getActivePath() != null) {
                        storage.delete(entity.getActivePath());
                    }
                    return new MutationResult(true);
                });
    }

    private void requireEvent(UUID ownerId, UUID eventId) {
        events.findByIdAndOwnerIdAndStatusNot(eventId, ownerId, "deleted")
                .orElseThrow(ApiException::notFound);
    }

    private MediaEntity ownedMedia(UUID ownerId, UUID eventId, UUID mediaId) {
        return media.findByIdAndOwnerIdAndEventId(mediaId, ownerId, eventId)
                .orElseThrow(ApiException::notFound);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private record MutationResult(boolean completed) {
    }
}
