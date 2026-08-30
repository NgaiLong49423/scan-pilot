package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.ScanEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScanEventRepository extends JpaRepository<ScanEventEntity, UUID>, ScanEventRepositoryCustom {

    List<ScanEventEntity> findByScanJobIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
        UUID scanJobId,
        long sequenceNumber,
        Pageable pageable
    );

    List<ScanEventEntity> findByScanJobIdOrderBySequenceNumberAsc(UUID scanJobId);
}
