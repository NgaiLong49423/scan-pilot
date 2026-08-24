package com.scanpilot.scanner.lifecycle;

import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.scanner.config.ScanWorkerInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Periodically updates heartbeats atomically for QUEUED scan jobs
 * owned by this JVM worker instance (FR-002, Issue #52 Revision 4).
 * Ensures queued jobs waiting for worker capacity are not marked as dead/stale by peer reconcilers.
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class ScanWorkerHeartbeatScheduler {

    private final ScanJobRepository scanJobRepository;
    private final ScanWorkerInstance scanWorkerInstance;

    @Scheduled(fixedDelay = 15000)
    public void sendHeartbeatForQueuedJobs() {
        try {
            int updatedCount = scanJobRepository.updateHeartbeatForQueuedJobsByWorker(
                    scanWorkerInstance.getInstanceId(),
                    Instant.now()
            );
            if (updatedCount > 0) {
                log.debug("Updated heartbeat for {} queued job(s) on worker {}",
                        updatedCount, scanWorkerInstance.getInstanceId());
            }
        } catch (Exception e) {
            log.warn("Failed to update heartbeat for queued jobs on worker {}: errorType={}",
                    scanWorkerInstance.getInstanceId(), e.getClass().getSimpleName());
        }
    }
}
