package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.FindingLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface FindingLocationRepository extends JpaRepository<FindingLocationEntity, UUID> {

    List<FindingLocationEntity> findByFindingId(UUID findingId);

    List<FindingLocationEntity> findByFindingIdAndIsCurrentHeadTrue(UUID findingId);

    @Modifying
    @Transactional
    void deleteByFindingId(UUID findingId);
}
