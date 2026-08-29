package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositoryRepository extends JpaRepository<RepositoryEntity, UUID> {

    Optional<RepositoryEntity> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);

    List<RepositoryEntity> findByUserId(UUID userId);

    Optional<RepositoryEntity> findByFullName(String fullName);

    Optional<RepositoryEntity> findByUserIdAndFullName(UUID userId, String fullName);

    List<RepositoryEntity> findByGithubRepoIdAndInstallationIdAndStatus(Long githubRepoId, Long installationId, String status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM RepositoryEntity r WHERE r.id = :id")
    Optional<RepositoryEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
