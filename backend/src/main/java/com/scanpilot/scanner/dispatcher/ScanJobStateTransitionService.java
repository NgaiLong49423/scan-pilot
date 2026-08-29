package com.scanpilot.scanner.dispatcher;

import com.scanpilot.persistence.repository.ScanJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    private final TransactionTemplate transactionTemplate;
    private final ScanJobRepository scanJobRepository;

    /**
     * Atomically marks a scan job as failed in an independent transaction.
     */
    public void markJobFailed(UUID jobId, String errorMessage) {
        if (jobId == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            Instant now = Instant.now();
            scanJobRepository.updateJobStatusAndError(jobId, "FAILED", "FAILED", errorMessage, now);
            log.info("Scan job {} transitioned to FAILED: {}", jobId, errorMessage);
        });
    }

    /**
     * Atomically transitions a queued scan job to running.
     */
    public boolean transitionQueuedToRunning(UUID jobId, String workerInstanceId) {
        if (jobId == null) {
            return false;
        }
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
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
    }
}
