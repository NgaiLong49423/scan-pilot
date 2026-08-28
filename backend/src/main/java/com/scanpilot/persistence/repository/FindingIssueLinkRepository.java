package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.FindingIssueLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FindingIssueLinkRepository extends JpaRepository<FindingIssueLinkEntity, UUID> {

    Optional<FindingIssueLinkEntity> findByFindingId(UUID findingId);

    java.util.List<FindingIssueLinkEntity> findByFindingIdIn(java.util.Collection<UUID> findingIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FindingIssueLinkEntity f SET f.state = :newState, f.updatedAt = :now WHERE f.findingId = :findingId AND f.state = :oldState")
    int updateStateConditional(
        @Param("findingId") UUID findingId,
        @Param("oldState") String oldState,
        @Param("newState") String newState,
        @Param("now") Instant now
    );
}
