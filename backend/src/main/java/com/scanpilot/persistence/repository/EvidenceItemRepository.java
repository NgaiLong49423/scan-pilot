package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.EvidenceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceItemRepository extends JpaRepository<EvidenceItemEntity, UUID> {

    List<EvidenceItemEntity> findByFindingId(UUID findingId);

    List<EvidenceItemEntity> findByFindingIdOrderByCreatedAtAsc(UUID findingId);
}
