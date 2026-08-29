package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.InstallationStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallationStateRepository extends JpaRepository<InstallationStateEntity, UUID> {

    Optional<InstallationStateEntity> findByStateHash(String stateHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE InstallationStateEntity s
        SET s.status = 'CONSUMED', s.consumedAt = :now
        WHERE s.stateHash = :stateHash
          AND s.status = 'ACTIVE'
          AND s.expiresAt > :now
          AND s.userId = :userId
          AND s.sessionId = :sessionId
    """)
    int consumeState(
            @Param("stateHash") String stateHash,
            @Param("userId") UUID userId,
            @Param("sessionId") String sessionId,
            @Param("now") Instant now
    );
}
