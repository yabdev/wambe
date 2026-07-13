package com.wambe.api.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wambe.api.common.error.ApiException;
import com.wambe.api.common.idempotency.IdempotencyService;
import com.wambe.api.common.rls.RlsContext;
import com.wambe.api.generated.model.WambeProductEventV1;
import com.wambe.api.session.persistence.CreationSessionRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreationMilestoneService {

    private final CreationSessionRepository sessions;
    private final RlsContext rls;
    private final IdempotencyService idempotency;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CreationMilestoneService(
            CreationSessionRepository sessions,
            RlsContext rls,
            IdempotencyService idempotency,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.rls = rls;
        this.idempotency = idempotency;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(
            UUID ownerId,
            UUID sessionId,
            UUID key,
            WambeProductEventV1 request) {
        rls.apply(ownerId);
        idempotency.execute(
                ownerId,
                "POST /creation-sessions/" + sessionId + "/events",
                key,
                request,
                HttpStatus.ACCEPTED,
                MutationResult.class,
                () -> {
                    var session = sessions.findByIdAndOwnerId(sessionId, ownerId)
                            .orElseThrow(ApiException::notFound);
                    if (!sessionId.equals(request.getCreationSessionId())
                            || !session.getEventId().equals(request.getEventId())) {
                        throw new ApiException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "SESSION_EVENT_MISMATCH",
                                "The milestone does not belong to this creation session");
                    }
                    jdbcTemplate.update("""
                            insert into product_events (
                                owner_id, event_id, creation_session_id, name, schema_version,
                                occurred_at, received_at, allowed_properties, consent_category
                            ) values (?, ?, ?, ?, '1.0', ?, ?, cast(? as jsonb), 'analytics')
                            """,
                            ownerId,
                            request.getEventId(),
                            sessionId,
                            request.getName().getValue(),
                            request.getOccurredAt(),
                            OffsetDateTime.now(ZoneOffset.UTC),
                            json(request.getProperties()));
                    return new MutationResult(true);
                });
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize milestone properties", exception);
        }
    }

    private record MutationResult(boolean completed) {
    }
}
