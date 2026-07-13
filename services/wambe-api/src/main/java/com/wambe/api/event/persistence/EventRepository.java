package com.wambe.api.event.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    Optional<EventEntity> findByIdAndOwnerIdAndStatusNot(UUID id, UUID ownerId, String excludedStatus);

    Optional<EventEntity> findByOwnerIdAndClientCreationKey(UUID ownerId, UUID clientCreationKey);

    @Query("""
            select event from EventEntity event
            where event.ownerId = :ownerId
              and event.status <> 'deleted'
              and (:status is null or event.status = :status)
            order by event.updatedAt desc, event.id desc
            """)
    List<EventEntity> listOwned(
            @Param("ownerId") UUID ownerId,
            @Param("status") String status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event from EventEntity event
            where event.id = :id and event.ownerId = :ownerId and event.status <> 'deleted'
            """)
    Optional<EventEntity> lockOwned(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
