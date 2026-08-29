package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.FindingRemediationPrLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FindingRemediationPrLinkRepository extends JpaRepository<FindingRemediationPrLinkEntity, UUID> {

    Optional<FindingRemediationPrLinkEntity> findByFindingId(UUID findingId);

    Optional<FindingRemediationPrLinkEntity> findByFindingIdAndSourceRevisionCommit(UUID findingId, String sourceRevisionCommit);

    Optional<FindingRemediationPrLinkEntity> findByIdempotencyMarker(String idempotencyMarker);
}