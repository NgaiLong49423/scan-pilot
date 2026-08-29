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
    private final ScanJobStateTransitionService scanJobStateTransitionService;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Qualifier("scanTaskExecutor")
    private final ThreadPoolTaskExecutor scanTaskExecutor;

    private org.springframework.transaction.support.TransactionTemplate requiresNewTxTemplate;

    private org.springframework.transaction.support.TransactionTemplate getRequiresNewTxTemplate() {
        if (this.requiresNewTxTemplate == null) {
            org.springframework.transaction.support.TransactionTemplate tt = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTxTemplate = tt;
        }
        return this.requiresNewTxTemplate;
    }

    /**
     * Dispatches a manual scan job for the given repository and branch.
     * Prevents duplicate concurrent scans on the same repository via DB-level pessimistic locking.
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
                .triggerType("MANUAL")
                .workerInstanceId(scanWorkerInstance.getInstanceId())
                .createdAt(now)
                .updatedAt(now)
                .heartbeatAt(now)
                .startedAt(null)
                .completedAt(null)
                .build();

        ScanJobEntity savedJob = scanJobRepository.saveAndFlush(scanJob);
        UUID repoId = repo.getId();

        // Emit QUEUED stage started milestone event within dispatch transaction (AC-02, maxLimit = 95)
        emitEvent(savedJob.getId(), "QUEUED", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("QUEUED"), 95L);

        // 4. Trigger queue processor strictly AFTER database transaction commits
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tryProcessNextJobForRepository(repoId);
                }
            });
        } else {
            tryProcessNextJobForRepository(repoId);
        }

        log.info("Dispatched scan job {} for repository {} on branch {} (status=QUEUED, stage=QUEUED, workerInstanceId={})",
                savedJob.getId(), repo.getId(), branch, scanWorkerInstance.getInstanceId());
        return savedJob;
    }

    /**
     * Attempts to claim and execute the next QUEUED job for the repository in FIFO order.
     * Guaranteed at most one RUNNING job per repository at any time.
     */
    public void tryProcessNextJobForRepository(UUID repoId) {
        if (repoId == null) {
            return;
        }

        getRequiresNewTxTemplate().executeWithoutResult(status -> {
            // Pessimistic lock at DB level on repository row
            repositoryRepository.findByIdForUpdate(repoId);

            // Check if there is already a RUNNING job for this repository
            List<ScanJobEntity> runningJobs = scanJobRepository.findByRepositoryIdAndStatus(repoId, "RUNNING");
            if (!runningJobs.isEmpty()) {
                return; // Active job is currently running; its finally block will drain the next job
            }

            // Find oldest QUEUED job for this repository
            Optional<ScanJobEntity> nextQueuedJobOpt = scanJobRepository.findFirstByRepositoryIdAndStatusOrderByCreatedAtAsc(repoId, "QUEUED");
            if (nextQueuedJobOpt.isEmpty()) {
                return;
            }

            ScanJobEntity nextJob = nextQueuedJobOpt.get();
            UUID jobId = nextJob.getId();

            // Transition QUEUED -> RUNNING via independent transaction coordinator
            boolean claimed = scanJobStateTransitionService.transitionQueuedToRunning(jobId, scanWorkerInstance.getInstanceId());
            if (!claimed) {
                return;
            }

            emitEvent(jobId, "RUNNING", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("RUNNING"), 95L);

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
        });
    }

    private void submitTaskToExecutor(UUID jobId, UUID repoId) {
        try {
            scanTaskExecutor.execute(() -> {
                try {
                    scanPipelineService.executeScanJob(jobId);
                } catch (Exception e) {
                    log.error("Unhandled exception during async scan job execution for jobId={}: errorType={} message={}",
                            jobId, e.getClass().getSimpleName(), scanPipelineService.sanitizeErrorMessage(e.getMessage()));
                } finally {
                    // Self-contained queue drain: ScanPipelineService does not call ScanJobDispatcher
                    tryProcessNextJobForRepository(repoId);
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("Scan executor capacity exceeded after commit for jobId={} repositoryId={}: errorType={}",
                    jobId, repoId, e.getClass().getSimpleName());
            emitEvent(jobId, "FAILED", "SCAN_FAILED", "JOB_FAILED", new ScanEventPayload.JobFailedPayload("DISPATCH_CAPACITY_EXCEEDED"), 100L);
            scanJobStateTransitionService.markJobFailed(jobId, CAPACITY_EXCEEDED_MESSAGE);
            // NON-RECURSIVE: do not call tryProcessNextJobForRepository here; reconciler recovers later
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
            getRequiresNewTxTemplate().executeWithoutResult(status -> {
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
            });
        } catch (Exception e) {
            log.warn("Event persistence error for eventType={}", eventType);
        }
    }
}
