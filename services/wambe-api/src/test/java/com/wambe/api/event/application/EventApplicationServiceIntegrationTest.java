package com.wambe.api.event.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.wambe.api.PostgresIntegrationTest;
import com.wambe.api.generated.model.CreateEventRequest;
import com.wambe.api.generated.model.UpdateEventRequest;
import com.wambe.api.generated.model.Venue;
import com.wambe.api.generated.model.Visibility;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventApplicationServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private EventApplicationService service;

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
    }
}
