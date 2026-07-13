package com.wambe.api.scanner.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanJobRepository extends JpaRepository<ScanJobEntity, UUID> {

    Optional<ScanJobEntity> findByMediaId(UUID mediaId);
}
