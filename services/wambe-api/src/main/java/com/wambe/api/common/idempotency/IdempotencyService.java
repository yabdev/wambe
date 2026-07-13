package com.wambe.api.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wambe.api.common.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T execute(
            UUID ownerId,
            String route,
            UUID key,
            Object request,
            HttpStatus responseStatus,
            Class<T> responseType,
            Supplier<T> operation) {
        String requestHash = hash(request);
        int inserted = jdbcTemplate.update("""
                insert into idempotency_keys (
                    owner_id, route, idempotency_key, request_hash, created_at, expires_at
                ) values (?, ?, ?, ?, ?, ?)
                on conflict do nothing
                """,
                ownerId,
                route,
                key,
                requestHash,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(24));
        if (inserted == 0) {
            return replay(ownerId, route, key, requestHash, responseType);
        }

        T response = operation.get();
        jdbcTemplate.update("""
                update idempotency_keys
                   set response_status = ?, response_body = cast(? as jsonb)
                 where owner_id = ? and route = ? and idempotency_key = ?
                """,
                responseStatus.value(),
                json(response),
                ownerId,
                route,
                key);
        return response;
    }

    private <T> T replay(
            UUID ownerId,
            String route,
            UUID key,
            String requestHash,
            Class<T> responseType) {
        StoredOperation stored = jdbcTemplate.queryForObject("""
                select request_hash, response_body::text
                  from idempotency_keys
                 where owner_id = ? and route = ? and idempotency_key = ?
                """,
                (resultSet, row) -> new StoredOperation(
                        resultSet.getString("request_hash"),
                        resultSet.getString("response_body")),
                ownerId,
                route,
                key);
        if (stored == null || !stored.requestHash().equals(requestHash)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_CONFLICT",
                    "The idempotency key was already used for a different request");
        }
        if (stored.responseBody() == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_IN_PROGRESS",
                    "The original request is still in progress");
        }
        try {
            return objectMapper.readValue(stored.responseBody(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored idempotency response is invalid", exception);
        }
    }

    private String hash(Object request) {
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash request", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not store idempotency response", exception);
        }
    }

    private record StoredOperation(String requestHash, String responseBody) {
    }
}
