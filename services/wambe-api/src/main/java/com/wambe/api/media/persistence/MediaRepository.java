package com.wambe.api.media.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {

    List<MediaEntity> findAllByOwnerIdAndEventIdAndStorageStatusNotOrderByCreatedAt(
            UUID ownerId,
            UUID eventId,
            String excludedStatus);

    Optional<MediaEntity> findByIdAndOwnerIdAndEventId(UUID id, UUID ownerId, UUID eventId);

    Optional<MediaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndEventIdAndStorageStatusIn(
            UUID ownerId,
            UUID eventId,
            List<String> statuses);
}
