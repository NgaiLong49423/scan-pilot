package com.scanpilot.scanner.lifecycle;

import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.repository.ScanJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Startup / Periodic Reconciler for in-flight scan jobs (FR-002, AC-52-06, Issue #52).
 * Reconciles stale scan jobs left in QUEUED or RUNNING states without recent heartbeat periodically and upon application startup.
 * Protects active jobs from other healthy instances or long-running scans by enforcing a heartbeat expiration threshold (2 minutes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScanJobRestartReconciler {

    public static final String RESTART_INTERRUPTED_MESSAGE = "Scan job interrupted by system restart; please trigger again";
    public static final Duration HEARTBEAT_EXPIRATION_THRESHOLD = Duration.ofMinutes(2);

    private final ScanJobRepository scanJobRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileStaleJobsOnStartup() {
        reconcileInterruptedJobs();
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public int reconcileInterruptedJobs() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(HEARTBEAT_EXPIRATION_THRESHOLD);

        int reconciled = scanJobRepository.reconcileStaleJobsAtomic(cutoff, RESTART_INTERRUPTED_MESSAGE, now);
        if (reconciled > 0) {
            log.warn("Atomic reconciliation completed. Marked {} expired jobs as FAILED (cutoff={})", reconciled, cutoff);
        } else {
            log.debug("Atomic reconciliation completed. No expired jobs found (cutoff={})", cutoff);
        }
        return reconciled;
    }
}
