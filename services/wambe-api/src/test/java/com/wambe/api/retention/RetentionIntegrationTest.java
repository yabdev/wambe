package com.wambe.api.retention;

import static org.assertj.core.api.Assertions.assertThat;

import com.wambe.api.PostgresIntegrationTest;
import com.wambe.api.event.application.EventApplicationService;
import com.wambe.api.generated.model.CreateEventRequest;
import com.wambe.api.integration.storage.LocalObjectStorage;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class RetentionIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private EventApplicationService events;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InternalJobService retention;

    @Autowired
    private LocalObjectStorage storage;

    @Test
    @Transactional
    void removesAbandonedDraftMediaAndStoredObjectAfterThirtyDays() throws Exception {
        UUID ownerId = UUID.randomUUID();
        var created = events.create(
                ownerId,
                UUID.randomUUID(),
                new CreateEventRequest(
                        UUID.randomUUID(),
                        CreateEventRequest.EligibilityEnum.ELIGIBLE_FIRST_TIME));
        UUID eventId = created.getEvent().getId();
        UUID mediaId = UUID.randomUUID();
        OffsetDateTime runAt = OffsetDateTime.now(ZoneOffset.UTC);

        String quarantinePath = "quarantine/" + mediaId + "/invite.png";
        jdbc.update("""
                insert into event_media (
                    id, event_id, owner_id, role, filename, claimed_mime_type,
                    size_bytes, quarantine_path, storage_status
                ) values (?, ?, ?, 'invitation', 'invite.png', 'image/png',
                    1024, ?, 'quarantine')
                """,
                mediaId,
                eventId,
                ownerId,
                quarantinePath);
        var storedObject = storage.quarantinePath(quarantinePath);
        Files.createDirectories(storedObject.getParent());
        Files.write(storedObject, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});
        jdbc.update(
                "update events set last_saved_at = ? where id = ?",
                runAt.minusDays(31),
                eventId);

        var storagePaths = jdbc.queryForList(
                "select quarantine_path from retention_storage_paths(?)",
                String.class,
                runAt);
        assertThat(storagePaths).containsExactly(quarantinePath);

        retention.runRetention();

        assertThat(Files.exists(storedObject)).isFalse();
        assertThat(jdbc.queryForObject(
                "select count(*) from events where id = ?",
                Long.class,
                eventId)).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from event_media where id = ?",
                Long.class,
                mediaId)).isZero();
    }
}
