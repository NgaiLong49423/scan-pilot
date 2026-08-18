package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.ScanJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScanJobRepository extends JpaRepository<ScanJobEntity, UUID> {

    List<ScanJobEntity> findByRepositoryIdOrderByStartedAtDesc(UUID repositoryId);

    List<ScanJobEntity> findByRepositoryIdAndStatus(UUID repositoryId, String status);

    Optional<ScanJobEntity> findTopByRepositoryIdAndBranchNameOrderByStartedAtDesc(UUID repositoryId, String branchName);
}
