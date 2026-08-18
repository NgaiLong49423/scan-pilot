package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.CoverageRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoverageRecordRepository extends JpaRepository<CoverageRecordEntity, UUID> {

    Optional<CoverageRecordEntity> findByScanJobId(UUID scanJobId);

    List<CoverageRecordEntity> findByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);
}
