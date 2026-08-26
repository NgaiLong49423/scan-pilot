package com.scanpilot.scanner.dispatcher;

import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanEventRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.scanner.config.ScanWorkerInstance;
import com.scanpilot.scanner.pipeline.ScanPipelineService;
import com.scanpilot.scanner.telemetry.ScanEventPayload;
import com.scanpilot.scanner.telemetry.TelemetryPayloadSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/**
 * Dispatches scan jobs asynchronously to a bounded thread pool task executor (FR-002, NFR-001, Issue #52).
 * Handles duplicate active scan prevention at PostgreSQL row lock level and capacity overload protection.
 * Ensures tasks are only submitted to the executor after the enclosing transaction commits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanJobDispatcher {

    public static final String CAPACITY_EXCEEDED_MESSAGE = "Scan capacity exceeded, please retry later";
    private static final List<String> ACTIVE_STATUSES = List.of("QUEUED", "RUNNING");

    private final ScanPipelineService scanPipelineService;
    private final ScanJobRepository scanJobRepository;
    private final ScanEventRepository scanEventRepository;
    private final RepositoryRepository repositoryRepository;
    private final ScanWorkerInstance scanWorkerInstance;
    private final TelemetryPayloadSerializer telemetryPayloadSerializer;

    @Qualifier("scanTaskExecutor")
    private final ThreadPoolTaskExecutor scanTaskExecutor;

    /**
     * Dispatches a scan job for the given repository and branch.
     * Prevents duplicate concurrent scans on the same repository via DB-level pessimistic locking.
     * Enqueues the scan job in DB and registers post-commit asynchronous execution on bounded worker pool.
     *
     * @param repo       the target repository entity
     * @param branchName the target branch name
     * @return the newly queued ScanJobEntity or the already active ScanJobEntity
     */
    @Transactional
    public ScanJobEntity dispatch(RepositoryEntity repo, String branchName) {
        if (repo == null || repo.getId() == null) {
            throw new IllegalArgumentException("Repository and repository ID are required for scan dispatch");
        }

        String branch = (branchName != null && !branchName.isBlank()) ? branchName.trim() : "main";

        // 1. Pessimistic lock at DB level on repository row to prevent race conditions across threads/instances
        repositoryRepository.findByIdForUpdate(repo.getId());

        // 2. Duplicate Prevention: check if repository has an active job (QUEUED or RUNNING)
        List<ScanJobEntity> activeJobs = scanJobRepository.findByRepositoryIdAndStatusIn(repo.getId(), ACTIVE_STATUSES);
        if (!activeJobs.isEmpty()) {
            ScanJobEntity activeJob = activeJobs.get(0);
            log.info("Duplicate scan trigger prevented for repository {}. Returning active job {} in status {}",
                    repo.getId(), activeJob.getId(), activeJob.getStatus());
            return activeJob;
        }

        Instant now = Instant.now();
        // 3. Create and persist ScanJobEntity in QUEUED state with worker instance ID and initial heartbeat
        ScanJobEntity scanJob = ScanJobEntity.builder()
                .repositoryId(repo.getId())
                .branchName(branch)
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("QUEUED")
                .stage("QUEUED")
                .workerInstanceId(scanWorkerInstance.getInstanceId())
                .createdAt(now)
                .updatedAt(now)
                .heartbeatAt(now)
                .startedAt(null)
                .completedAt(null)
                .build();

        ScanJobEntity savedJob = scanJobRepository.saveAndFlush(scanJob);
        UUID jobId = savedJob.getId();
        UUID repoId = repo.getId();

        // Emit QUEUED stage started milestone event within dispatch transaction (AC-02, maxLimit = 95)
        emitEvent(jobId, "QUEUED", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("QUEUED"), 95L);

        // 4. Submit task to bounded executor strictly AFTER database transaction commits
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitTaskToExecutor(jobId, repoId);
                }
            });
        } else {
            submitTaskToExecutor(jobId, repoId);
        }

        log.info("Dispatched scan job {} for repository {} on branch {} (status=QUEUED, stage=QUEUED, workerInstanceId={})",
                savedJob.getId(), repo.getId(), branch, scanWorkerInstance.getInstanceId());
        return savedJob;
    }

    private void submitTaskToExecutor(UUID jobId, UUID repoId) {
        try {
            scanTaskExecutor.execute(() -> {
                try {
                    scanPipelineService.executeScanJob(jobId);
                } catch (Exception e) {
                    log.error("Unhandled exception during async scan job execution for jobId={}: errorType={} message={}",
                            jobId, e.getClass().getSimpleName(), scanPipelineService.sanitizeErrorMessage(e.getMessage()));
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("Scan executor capacity exceeded after commit for jobId={} repositoryId={}: errorType={}",
                    jobId, repoId, e.getClass().getSimpleName());
            emitEvent(jobId, "FAILED", "SCAN_FAILED", "JOB_FAILED", new ScanEventPayload.JobFailedPayload("DISPATCH_CAPACITY_EXCEEDED"), 100L);
            scanJobRepository.updateJobStatusAndError(jobId, "FAILED", "FAILED", CAPACITY_EXCEEDED_MESSAGE, Instant.now());
        }
    }

    public void emitEvent(UUID jobId, String stage, String eventType, String messageCode, ScanEventPayload payload, long maxLimit) {
        if (scanEventRepository == null || jobId == null) {
            return;
        }
        try {
            String payloadJson = telemetryPayloadSerializer != null ? telemetryPayloadSerializer.serialize(payload) : null;
            if (payload != null && payloadJson == null) {
                log.debug("Event {} ({}) suppressed due to invalid/oversized payload", eventType, messageCode);
                return;
            }
            Optional<Long> allocatedSeq = scanEventRepository.insertEventAtomicCTE(
                    jobId,
                    maxLimit,
                    UUID.randomUUID(),
                    stage,
                    eventType,
                    messageCode,
                    payloadJson,
                    Instant.now()
            );
            if (allocatedSeq.isEmpty()) {
                log.debug("Event {} ({}) dropped/suppressed", eventType, messageCode);
            }
        } catch (Exception e) {
            log.warn("Event persistence error for eventType={}", eventType);
        }
    }
}
