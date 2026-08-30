package com.scanpilot.scanner.lifecycle;

import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.scanner.dispatcher.ScanJobDispatcher;
import com.scanpilot.scanner.telemetry.ScanEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Startup / Periodic Reconciler for in-flight scan jobs (FR-002, AC-52-06, Issue #52, Issue #54).
 * Reconciles stalled scan jobs left in RUNNING state without recent heartbeat periodically and upon application startup.
 * Discovers and kicks stranded QUEUED jobs for idle repositories.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScanJobRestartReconciler {

    public static final String RESTART_INTERRUPTED_MESSAGE = "Scan job interrupted by system restart; please trigger again";
    public static final Duration HEARTBEAT_EXPIRATION_THRESHOLD = Duration.ofMinutes(2);

    private final ScanJobRepository scanJobRepository;
    private final ScanJobDispatcher scanJobDispatcher;

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileStaleJobsOnStartup() {
        reconcileInterruptedJobs();
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public int reconcileInterruptedJobs() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(HEARTBEAT_EXPIRATION_THRESHOLD);

        List<ScanJobEntity> staleJobs = scanJobRepository.findStaleRunningJobs(cutoff);
        int reconciled = 0;
        for (ScanJobEntity job : staleJobs) {
            try {
                int updated = scanJobRepository.reconcileStaleJobByIdAtomic(
                        job.getId(), cutoff, RESTART_INTERRUPTED_MESSAGE, now);
                if (updated > 0) {
                    scanJobDispatcher.emitEvent(job.getId(), "FAILED", "SCAN_FAILED", "JOB_FAILED",
                            new ScanEventPayload.JobFailedPayload("STALE_HEARTBEAT_TIMEOUT"), 100L);
                    reconciled++;
                }
            } catch (Exception e) {
                log.warn("Failed to reconcile stale job {}: errorType={}", job.getId(), e.getClass().getSimpleName());
            }
        }

        if (reconciled > 0) {
            log.warn("Atomic reconciliation completed. Marked {} expired RUNNING jobs as FAILED (cutoff={})", reconciled, cutoff);
        } else {
            log.debug("Atomic reconciliation completed. No expired jobs found (cutoff={})", cutoff);
        }

        // Kick stranded queues for repositories with QUEUED jobs and zero RUNNING jobs
        try {
            List<UUID> strandedRepoIds = scanJobRepository.findRepositoriesWithQueuedJobsAndNoRunningJobs();
            for (UUID repoId : strandedRepoIds) {
                scanJobDispatcher.tryProcessNextJobForRepository(repoId);
            }
        } catch (Exception e) {
            log.warn("Error kicking stranded repository queues: errorType={}", e.getClass().getSimpleName());
        }

        return reconciled;
    }
}
