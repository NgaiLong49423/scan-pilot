package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.ScanCheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScanCheckpointRepository extends JpaRepository<ScanCheckpointEntity, UUID> {

    Optional<ScanCheckpointEntity> findTopByRepositoryIdAndBranchNameOrderByCreatedAtDesc(UUID repositoryId, String branchName);

    List<ScanCheckpointEntity> findByRepositoryId(UUID repositoryId);
}
