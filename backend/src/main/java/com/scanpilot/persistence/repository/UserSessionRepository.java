package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, String> {

    Optional<UserSessionEntity> findBySessionId(String sessionId);

    List<UserSessionEntity> findByUserId(UUID userId);

    void deleteBySessionId(String sessionId);

    void deleteByExpiresAtBefore(Instant now);
}
