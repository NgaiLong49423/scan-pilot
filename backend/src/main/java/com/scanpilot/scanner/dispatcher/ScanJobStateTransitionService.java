package com.scanpilot.scanner.dispatcher;

import com.scanpilot.persistence.repository.ScanJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

/**
 * Dedicated service for independent transaction boundaries and atomic state transitions
 * without relying on self-invocation (Issue #54).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanJobStateTransitionService {

    private final PlatformTransactionManager transactionManager;
    private final ScanJobRepository scanJobRepository;

    private TransactionTemplate getRequiresNewTxTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    /**
     * Atomically marks a scan job as failed in an independent transaction.
     */
    public void markJobFailed(UUID jobId, String errorMessage) {
        if (jobId == null) {
            return;
        }
        try {
            getRequiresNewTxTemplate().executeWithoutResult(status -> {
                scanJobRepository.findById(jobId).ifPresent(job -> {
                    Instant now = Instant.now();
                    job.setStatus("FAILED");
                    job.setStage("FAILED");
                    job.setErrorMessage(errorMessage);
                    job.setCompletedAt(now);
                    job.setUpdatedAt(now);
                    job.setHeartbeatAt(now);
                    scanJobRepository.saveAndFlush(job);
                    log.info("Scan job {} transitioned to FAILED: {}", jobId, errorMessage);
                });
            });
        } catch (Exception e) {
            log.warn("Failed to mark scan job {} as FAILED: errorType={}", jobId, e.getClass().getSimpleName());
        }
    }

    /**
     * Atomically transitions a queued scan job to running.
     */
    public boolean transitionQueuedToRunning(UUID jobId, String workerInstanceId) {
        if (jobId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(getRequiresNewTxTemplate().execute(status -> {
                return scanJobRepository.findById(jobId).map(job -> {
                    if (!"QUEUED".equals(job.getStatus())) {
                        return false;
                    }
                    Instant now = Instant.now();
                    job.setStatus("RUNNING");
                    job.setStage("RUNNING");
                    job.setStartedAt(now);
                    job.setUpdatedAt(now);
                    job.setHeartbeatAt(now);
                    if (workerInstanceId != null) {
                        job.setWorkerInstanceId(workerInstanceId);
                    }
                    scanJobRepository.saveAndFlush(job);
                    return true;
                }).orElse(false);
            }));
        } catch (Exception e) {
            log.warn("Failed to transition queued job {} to running: errorType={}", jobId, e.getClass().getSimpleName());
            return false;
        }
    }
}
