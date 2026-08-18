package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.MonitoredBranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitoredBranchRepository extends JpaRepository<MonitoredBranchEntity, UUID> {

    List<MonitoredBranchEntity> findByRepositoryId(UUID repositoryId);

    Optional<MonitoredBranchEntity> findByRepositoryIdAndBranchName(UUID repositoryId, String branchName);

    List<MonitoredBranchEntity> findByRepositoryIdAndIsActiveTrue(UUID repositoryId);

    long countByRepositoryIdAndIsActiveTrue(UUID repositoryId);
}
