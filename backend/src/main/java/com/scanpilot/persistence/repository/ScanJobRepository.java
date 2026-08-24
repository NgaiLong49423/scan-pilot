package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.ScanJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScanJobRepository extends JpaRepository<ScanJobEntity, UUID> {

    List<ScanJobEntity> findByRepositoryIdOrderByStartedAtDesc(UUID repositoryId);

    List<ScanJobEntity> findByRepositoryIdAndStatus(UUID repositoryId, String status);

    List<ScanJobEntity> findByRepositoryIdAndStatusIn(UUID repositoryId, Collection<String> statuses);

    List<ScanJobEntity> findByStatusIn(Collection<String> statuses);

    Optional<ScanJobEntity> findTopByRepositoryIdAndBranchNameOrderByStartedAtDesc(UUID repositoryId, String branchName);

    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE ScanJobEntity j SET j.status = 'FAILED', j.stage = 'FAILED', " +
           "j.errorMessage = :errorMessage, j.completedAt = :now, j.updatedAt = :now " +
           "WHERE j.status IN ('QUEUED', 'RUNNING') AND j.heartbeatAt < :cutoff")
    int reconcileStaleJobsAtomic(@Param("cutoff") Instant cutoff,
                                 @Param("errorMessage") String errorMessage,
                                 @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE ScanJobEntity j SET j.heartbeatAt = :now, j.updatedAt = :now " +
           "WHERE j.workerInstanceId = :workerInstanceId AND j.status = 'QUEUED'")
    int updateHeartbeatForQueuedJobsByWorker(@Param("workerInstanceId") String workerInstanceId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE ScanJobEntity j SET j.heartbeatAt = :now, j.updatedAt = :now " +
           "WHERE j.id = :jobId AND j.status = 'RUNNING'")
    int updateHeartbeatForRunningJob(@Param("jobId") UUID jobId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE ScanJobEntity j SET j.status = :status, j.stage = :stage, j.errorMessage = :errorMessage, j.completedAt = :now, j.updatedAt = :now, j.heartbeatAt = :now WHERE j.id = :jobId")
    int updateJobStatusAndError(@Param("jobId") UUID jobId, @Param("status") String status, @Param("stage") String stage, @Param("errorMessage") String errorMessage, @Param("now") Instant now);
}
