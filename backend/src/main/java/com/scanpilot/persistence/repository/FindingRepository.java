package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.FindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FindingRepository extends JpaRepository<FindingEntity, UUID> {

    Optional<FindingEntity> findByRepositoryIdAndFingerprint(UUID repositoryId, String fingerprint);

    List<FindingEntity> findByRepositoryId(UUID repositoryId);

    List<FindingEntity> findByRepositoryIdAndLifecycle(UUID repositoryId, String lifecycle);

    List<FindingEntity> findByRepositoryIdAndSeverity(UUID repositoryId, String severity);

    List<FindingEntity> findByFingerprint(String fingerprint);

    long countByRepositoryIdAndLifecycle(UUID repositoryId, String lifecycle);
}
