package com.wambe.api.event.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wambe.api.common.error.ApiException;
import com.wambe.api.event.persistence.EventEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublishValidatorTest {

    private final PublishValidator validator = new PublishValidator();
    private final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    void acceptsLeanPublicationWithoutOptionalMedia() {
        EventEntity event = validEvent();

        assertThatCode(() -> validator.validate(event, false, now))
                .doesNotThrowAnyException();
    }

    @Test
    void reportsRequiredVenueAndMediaProblemsTogether() {
        EventEntity event = EventEntity.draft(UUID.randomUUID(), UUID.randomUUID(), now);

        assertThatThrownBy(() -> validator.validate(event, true, now))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    org.assertj.core.api.Assertions.assertThat(api.fieldErrors())
                            .containsKeys("eventType", "title", "startsAt", "venue", "visibility", "media");
                });
    }

    private EventEntity validEvent() {
        EventEntity event = EventEntity.draft(UUID.randomUUID(), UUID.randomUUID(), now);
        event.update(
                "wedding",
                "Ada and Tunde",
                now.plusDays(10),
                "Africa/Lagos",
                "public",
                "Celebration Hall",
                "12 Marina Road, Lagos",
                "google-place-id",
                BigDecimal.valueOf(6.455),
                BigDecimal.valueOf(3.394),
                true,
                null,
                now);
        return event;
    }
}
