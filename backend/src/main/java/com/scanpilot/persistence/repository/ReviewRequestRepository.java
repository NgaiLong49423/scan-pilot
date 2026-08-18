package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.ReviewRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRequestRepository extends JpaRepository<ReviewRequestEntity, UUID> {

    List<ReviewRequestEntity> findByRepositoryId(UUID repositoryId);

    List<ReviewRequestEntity> findByFindingId(UUID findingId);

    List<ReviewRequestEntity> findByRepositoryIdAndStatus(UUID repositoryId, String status);
}
