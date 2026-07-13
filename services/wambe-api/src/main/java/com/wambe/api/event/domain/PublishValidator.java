package com.wambe.api.event.domain;

import com.wambe.api.common.error.ApiException;
import com.wambe.api.event.persistence.EventEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PublishValidator {

    private static final Set<String> VISIBILITIES =
            Set.of("public", "private_link", "invite_only", "hidden_location");

    public void validate(EventEntity event, boolean hasNonCleanMedia, OffsetDateTime now) {
        var errors = new LinkedHashMap<String, List<String>>();
        required(errors, "eventType", event.getEventType());
        required(errors, "title", event.getTitle());
        if (event.getStartsAt() == null || !event.getStartsAt().isAfter(now)) {
            errors.put("startsAt", List.of("Choose a future event date and time"));
        }
        required(errors, "venue.displayAddress", event.getVenueDisplayAddress());
        if (event.getVenueConfirmedAt() == null
                || !validLatitude(event.getVenueLatitude())
                || !validLongitude(event.getVenueLongitude())) {
            errors.put("venue", List.of("Confirm the venue and map pin"));
        }
        if (event.getVisibility() == null || !VISIBILITIES.contains(event.getVisibility())) {
            errors.put("visibility", List.of("Choose who can see this event"));
        }
        if (hasNonCleanMedia) {
            errors.put("media", List.of("Wait for attached media to pass safety checks or remove it"));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PUBLICATION_VALIDATION_FAILED",
                    "Resolve the highlighted fields before publishing",
                    errors);
        }
    }

    private void required(
            LinkedHashMap<String, List<String>> errors,
            String field,
            String value) {
        if (value == null || value.isBlank()) {
            errors.put(field, List.of("This field is required"));
        }
    }

    private boolean validLatitude(BigDecimal value) {
        return value != null
                && value.compareTo(BigDecimal.valueOf(-90)) >= 0
                && value.compareTo(BigDecimal.valueOf(90)) <= 0;
    }

    private boolean validLongitude(BigDecimal value) {
        return value != null
                && value.compareTo(BigDecimal.valueOf(-180)) >= 0
                && value.compareTo(BigDecimal.valueOf(180)) <= 0;
    }
}
