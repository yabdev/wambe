package com.wambe.api.session.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreationSessionRepository extends JpaRepository<CreationSessionEntity, UUID> {

    Optional<CreationSessionEntity> findFirstByOwnerIdAndEventIdOrderByOpenedAt(
            UUID ownerId,
            UUID eventId);

    Optional<CreationSessionEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
