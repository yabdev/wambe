package com.wambe.api.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wambe.api.PostgresIntegrationTest;
import com.wambe.api.common.error.ApiException;
import com.wambe.api.common.rls.RlsContext;
import com.wambe.api.generated.model.CreateEventRequest;
import com.wambe.api.generated.model.Event;
import com.wambe.api.generated.model.UpdateEventRequest;
import com.wambe.api.generated.model.Venue;
import com.wambe.api.generated.model.Visibility;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class EventApplicationServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private EventApplicationService service;

    @Autowired
    private PublicMetadataService publicMetadata;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RlsContext rls;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void createsUpdatesPublishesAndSafelyReplaysRequests() {
        UUID ownerId = UUID.randomUUID();
        UUID creationKey = UUID.randomUUID();
        UUID createIdempotencyKey = UUID.randomUUID();
        CreateEventRequest createRequest = new CreateEventRequest(
                creationKey,
                CreateEventRequest.EligibilityEnum.ELIGIBLE_FIRST_TIME);

        var created = service.create(ownerId, createIdempotencyKey, createRequest);
        var createReplay = service.create(ownerId, createIdempotencyKey, createRequest);

        assertThat(createReplay.getEvent().getId()).isEqualTo(created.getEvent().getId());
        assertThat(created.getEvent().getVersion()).isEqualTo(1);

        var update = new UpdateEventRequest()
                .eventType(UpdateEventRequest.EventTypeEnum.WEDDING)
                .title("Ada and Tunde")
                .startsAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30))
                .timezone("Africa/Lagos")
                .venue(new Venue("12 Marina Road, Lagos", true)
                        .name("Celebration Hall")
                        .placeId("google-place-id")
                        .latitude(BigDecimal.valueOf(6.455))
                        .longitude(BigDecimal.valueOf(3.394)))
                .visibility(Visibility.PUBLIC);

        var updated = service.update(
                ownerId,
                created.getEvent().getId(),
                created.getEvent().getVersion(),
                UUID.randomUUID(),
                update);
        assertThat(updated.getVersion()).isEqualTo(2);

        UUID publishKey = UUID.randomUUID();
        var published = service.publish(
                ownerId,
                updated.getId(),
                updated.getVersion(),
                publishKey);
        var replay = service.publish(
                ownerId,
                updated.getId(),
                updated.getVersion(),
                publishKey);

        assertThat(published.getEvent().getStatus().getValue()).isEqualTo("published");
        assertThat(published.getCanonicalUrl()).isEqualTo(replay.getCanonicalUrl());
        assertThat(published.getEvent().getSlug().orElseThrow()).hasSize(22);
        assertThat(published.getShareEligible()).isTrue();
        transactions.executeWithoutResult(status -> {
            rls.apply(ownerId);
            OffsetDateTime openedAt = jdbc.queryForObject(
                    "select opened_at from creation_sessions where id = ?",
                    OffsetDateTime.class,
                    created.getCreationSessionId());
            OffsetDateTime firstPublishedAt = jdbc.queryForObject(
                    "select first_published_at from creation_sessions where id = ?",
                    OffsetDateTime.class,
                    created.getCreationSessionId());
            assertThat(firstPublishedAt).isAfterOrEqualTo(openedAt);
            assertThat(jdbc.queryForList(
                            "select name from product_events where creation_session_id = ? order by occurred_at",
                            String.class,
                            created.getCreationSessionId()))
                    .contains("event_creation_started", "event_publish_succeeded");
        });
    }

    @ParameterizedTest
    @EnumSource(
            value = CreateEventRequest.EligibilityEnum.class,
            names = {"RESUMED_DRAFT", "STAFF_ASSISTED"})
    void preservesIneligibleKpiSessionClassification(CreateEventRequest.EligibilityEnum eligibility) {
        var created = service.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new CreateEventRequest(UUID.randomUUID(), eligibility));

        transactions.executeWithoutResult(status -> {
            rls.apply(created.getEvent().getOwnerId());
            assertThat(jdbc.queryForObject(
                            "select eligibility from creation_sessions where id = ?",
                            String.class,
                            created.getCreationSessionId()))
                    .isEqualTo(eligibility.getValue());
            assertThat(jdbc.queryForObject(
                            "select first_published_at from creation_sessions where id = ?",
                            OffsetDateTime.class,
                            created.getCreationSessionId()))
                    .isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(Visibility.class)
    void persistsEveryVisibilityAndBlocksProtectedSharing(Visibility visibility) {
        UUID ownerId = UUID.randomUUID();
        Event ready = createReadyEvent(ownerId, visibility);

        var published = service.publish(ownerId, ready.getId(), ready.getVersion(), UUID.randomUUID());
        boolean shareEligible = visibility == Visibility.PUBLIC || visibility == Visibility.PRIVATE_LINK;

        assertThat(published.getEvent().getVisibility().orElseThrow()).isEqualTo(visibility);
        assertThat(published.getShareEligible()).isEqualTo(shareEligible);
        if (shareEligible) {
            assertThat(publicMetadata.get(published.getEvent().getSlug().orElseThrow()).getVisibility().getValue())
                    .isEqualTo(visibility.getValue());
        } else {
            assertThat(published.getShareBlockedReason().orElseThrow())
                    .isEqualTo("GUEST_ACCESS_ENFORCEMENT_REQUIRED");
            assertNotFound(() -> publicMetadata.get(published.getEvent().getSlug().orElseThrow()));
        }
    }

    @Test
    void keepsStableUrlAcrossUpdateUnpublishAndRepublishThenDeletesSafely() {
        UUID ownerId = UUID.randomUUID();
        Event ready = createReadyEvent(ownerId, Visibility.PUBLIC);
        var firstPublish = service.publish(ownerId, ready.getId(), ready.getVersion(), UUID.randomUUID());
        String slug = firstPublish.getEvent().getSlug().orElseThrow();
        String canonicalUrl = firstPublish.getCanonicalUrl().toString();
        assertThat(publicMetadata.get(slug).getTitle()).isEqualTo("Ada and Tunde");

        Event updated = service.update(
                ownerId,
                ready.getId(),
                firstPublish.getEvent().getVersion(),
                UUID.randomUUID(),
                new UpdateEventRequest().title("Ada and Tunde — updated"));
        assertThat(updated.getSlug().orElseThrow()).isEqualTo(slug);
        assertThat(updated.getCanonicalUrl().orElseThrow().toString()).isEqualTo(canonicalUrl);

        Event unpublished = service.unpublish(
                ownerId,
                ready.getId(),
                updated.getVersion(),
                UUID.randomUUID());
        assertThat(unpublished.getStatus().getValue()).isEqualTo("unpublished");
        assertThat(unpublished.getSlug().orElseThrow()).isEqualTo(slug);
        assertNotFound(() -> publicMetadata.get(slug));

        var republished = service.publish(
                ownerId,
                ready.getId(),
                unpublished.getVersion(),
                UUID.randomUUID());
        assertThat(republished.getCanonicalUrl().toString()).isEqualTo(canonicalUrl);

        service.delete(ownerId, ready.getId(), UUID.randomUUID());
        assertNotFound(() -> service.get(ownerId, ready.getId()));
        assertThatThrownBy(() -> publicMetadata.get(slug))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.GONE);
                    assertThat(exception.code()).isEqualTo("EVENT_DELETED");
                });
    }

    @Test
    void deniesCrossOwnerReadsAndMutationsWithoutLeakingTheEvent() {
        UUID ownerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        Event ready = createReadyEvent(ownerId, Visibility.PUBLIC);

        assertNotFound(() -> service.get(otherOwnerId, ready.getId()));
        assertNotFound(() -> service.update(
                otherOwnerId,
                ready.getId(),
                ready.getVersion(),
                UUID.randomUUID(),
                new UpdateEventRequest().title("Stolen title")));
        assertNotFound(() -> service.publish(
                otherOwnerId,
                ready.getId(),
                ready.getVersion(),
                UUID.randomUUID()));
        assertNotFound(() -> service.delete(otherOwnerId, ready.getId(), UUID.randomUUID()));

        assertThat(service.get(ownerId, ready.getId()).getTitle().orElseThrow()).isEqualTo("Ada and Tunde");
    }

    @Test
    void rejectsAnIdempotencyKeyReusedForDifferentCreateData() {
        UUID ownerId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        service.create(
                ownerId,
                idempotencyKey,
                new CreateEventRequest(
                        UUID.randomUUID(),
                        CreateEventRequest.EligibilityEnum.ELIGIBLE_FIRST_TIME));

        assertThatThrownBy(() -> service.create(
                ownerId,
                idempotencyKey,
                new CreateEventRequest(
                        UUID.randomUUID(),
                        CreateEventRequest.EligibilityEnum.RESUMED_DRAFT)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT");
                });
    }

    private Event createReadyEvent(UUID ownerId, Visibility visibility) {
        var created = service.create(
                ownerId,
                UUID.randomUUID(),
                new CreateEventRequest(
                        UUID.randomUUID(),
                        CreateEventRequest.EligibilityEnum.ELIGIBLE_FIRST_TIME));
        return service.update(
                ownerId,
                created.getEvent().getId(),
                created.getEvent().getVersion(),
                UUID.randomUUID(),
                new UpdateEventRequest()
                        .eventType(UpdateEventRequest.EventTypeEnum.WEDDING)
                        .title("Ada and Tunde")
                        .startsAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30))
                        .timezone("Africa/Lagos")
                        .venue(new Venue("12 Marina Road, Lagos", true)
                                .name("Celebration Hall")
                                .placeId("google-place-id")
                                .latitude(BigDecimal.valueOf(6.455))
                                .longitude(BigDecimal.valueOf(3.394)))
                        .visibility(visibility));
    }

    private void assertNotFound(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.code()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(exception.getMessage()).isEqualTo("Resource not found");
                });
    }
}
