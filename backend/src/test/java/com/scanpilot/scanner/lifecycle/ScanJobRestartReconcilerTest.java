package com.scanpilot.scanner.lifecycle;

import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.config.ScanWorkerInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("ScanJobRestartReconciler Tests (FR-002, AC-52-06, Issue #52 Revision 4)")
class ScanJobRestartReconcilerTest {

    @Autowired
    private ScanJobRestartReconciler reconciler;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScanWorkerHeartbeatScheduler heartbeatScheduler;

    @Autowired
    private ScanWorkerInstance scanWorkerInstance;

    private RepositoryEntity repositoryEntity;

    @BeforeEach
    void setUp() {
        scanJobRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(556677L)
                .login("reconciler-tester")
                .name("Reconciler Tester")
                .build());

        repositoryEntity = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(112233L)
                .owner("reconciler-tester")
                .name("test-repo")
                .fullName("reconciler-tester/test-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .build());
    }

    @Test
    @DisplayName("testPeriodicReconciliationTransitionsExpiredRunningJobWithoutRestart: RUNNING job with expired heartbeat (> 2m) is transitioned to FAILED by periodic reconciliation without restart")
    void testPeriodicReconciliationTransitionsExpiredRunningJobWithoutRestart() {
        Instant now = Instant.now();

        ScanJobEntity staleJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("RUNNING")
                .stage("CLASSIFYING_FILES")
                .createdAt(now.minusSeconds(300))
                .startedAt(now.minusSeconds(290))
                .updatedAt(now.minusSeconds(180)) // 3 mins ago (> 2 min threshold)
                .heartbeatAt(now.minusSeconds(180)) // Expired heartbeat (3 mins ago)
                .workerInstanceId("crashed-worker-node")
                .build());

        // Periodic reconciliation called directly without ApplicationReadyEvent / system restart
        int reconciled = reconciler.reconcileInterruptedJobs();

        assertThat(reconciled).isEqualTo(1);
        ScanJobEntity result = scanJobRepository.findById(staleJob.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getStage()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).isEqualTo(ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE);
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("testPeriodicReconciliationPreservesActiveRunningJobWithFreshHeartbeat: RUNNING job with fresh heartbeat (< 2m) is not marked FAILED by periodic reconciliation")
    void testPeriodicReconciliationPreservesActiveRunningJobWithFreshHeartbeat() {
        Instant now = Instant.now();

        ScanJobEntity activeJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("RUNNING")
                .stage("SCANNING_SECRETS")
                .createdAt(now.minusSeconds(1200)) // 20 mins ago (active long running scan)
                .startedAt(now.minusSeconds(1190))
                .updatedAt(now.minusSeconds(20))
                .heartbeatAt(now.minusSeconds(20)) // Fresh heartbeat (20 seconds ago)
                .workerInstanceId("active-worker-node")
                .build());

        int reconciled = reconciler.reconcileInterruptedJobs();

        assertThat(reconciled).isEqualTo(0);
        ScanJobEntity result = scanJobRepository.findById(activeJob.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo("RUNNING");
        assertThat(result.getStage()).isEqualTo("SCANNING_SECRETS");
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("testPeriodicReconciliationPreservesActiveQueuedJobWithFreshHeartbeat: Peer node QUEUED job with fresh heartbeat (< 2m) stays QUEUED during periodic reconciliation")
    void testPeriodicReconciliationPreservesActiveQueuedJobWithFreshHeartbeat() {
        Instant now = Instant.now();

        ScanJobEntity queuedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("QUEUED")
                .stage("QUEUED")
                .createdAt(now.minusSeconds(600)) // 10 mins ago in queue
                .updatedAt(now.minusSeconds(15))
                .heartbeatAt(now.minusSeconds(15)) // Fresh heartbeat from peer node worker
                .workerInstanceId("peer-worker-node-1")
                .build());

        int reconciled = reconciler.reconcileInterruptedJobs();

        assertThat(reconciled).isEqualTo(0);
        ScanJobEntity result = scanJobRepository.findById(queuedJob.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo("QUEUED");
        assertThat(result.getStage()).isEqualTo("QUEUED");
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("testPeriodicReconciliationDoesNotAffectTerminalJobs: COMPLETED and FAILED jobs are not modified by periodic reconciliation")
    void testPeriodicReconciliationDoesNotAffectTerminalJobs() {
        Instant now = Instant.now();

        ScanJobEntity completedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("COMPLETED")
                .stage("COMPLETED")
                .createdAt(now.minusSeconds(1200))
                .startedAt(now.minusSeconds(1190))
                .completedAt(now.minusSeconds(1100))
                .updatedAt(now.minusSeconds(1100))
                .heartbeatAt(now.minusSeconds(1100))
                .build());

        ScanJobEntity failedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("FAILED")
                .stage("FAILED")
                .errorMessage("Original failure")
                .createdAt(now.minusSeconds(1200))
                .startedAt(now.minusSeconds(1190))
                .completedAt(now.minusSeconds(1100))
                .updatedAt(now.minusSeconds(1100))
                .heartbeatAt(now.minusSeconds(1100))
                .build());

        int reconciled = reconciler.reconcileInterruptedJobs();

        assertThat(reconciled).isEqualTo(0);
        ScanJobEntity untouchedCompleted = scanJobRepository.findById(completedJob.getId()).orElseThrow();
        assertThat(untouchedCompleted.getStatus()).isEqualTo("COMPLETED");
        assertThat(untouchedCompleted.getStage()).isEqualTo("COMPLETED");

        ScanJobEntity untouchedFailed = scanJobRepository.findById(failedJob.getId()).orElseThrow();
        assertThat(untouchedFailed.getStatus()).isEqualTo("FAILED");
        assertThat(untouchedFailed.getStage()).isEqualTo("FAILED");
        assertThat(untouchedFailed.getErrorMessage()).isEqualTo("Original failure");
    }

    @Test
    @DisplayName("Startup reconciliation: QUEUED job older than 10 minutes with recent heartbeat stays QUEUED")
    void testQueuedJobWithRecentHeartbeatStaysQueuedOnStartup() {
        Instant now = Instant.now();

        ScanJobEntity longQueuedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("QUEUED")
                .stage("QUEUED")
                .createdAt(now.minusSeconds(600)) // 10 mins ago
                .updatedAt(now.minusSeconds(10))
                .heartbeatAt(now.minusSeconds(10)) // Fresh heartbeat (10 seconds ago from worker scheduler)
                .workerInstanceId("peer-worker-node-1")
                .build());

        reconciler.reconcileStaleJobsOnStartup();

        ScanJobEntity result = scanJobRepository.findById(longQueuedJob.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo("QUEUED");
        assertThat(result.getStage()).isEqualTo("QUEUED");
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Startup reconciliation: RUNNING job older than 10 minutes with recent heartbeat stays RUNNING")
    void testRunningJobWithRecentHeartbeatStaysRunningOnStartup() {
        Instant now = Instant.now();

        ScanJobEntity longRunningJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("RUNNING")
                .stage("SCANNING_SECRETS")
                .createdAt(now.minusSeconds(1200)) // 20 mins ago
                .startedAt(now.minusSeconds(1190))
                .updatedAt(now.minusSeconds(30))
                .heartbeatAt(now.minusSeconds(30)) // Fresh heartbeat (30 seconds ago)
                .workerInstanceId("peer-worker-node-1")
                .build());

        reconciler.reconcileStaleJobsOnStartup();

        ScanJobEntity result = scanJobRepository.findById(longRunningJob.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo("RUNNING");
        assertThat(result.getStage()).isEqualTo("SCANNING_SECRETS");
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Startup reconciliation: Job with expired heartbeat (> 2m ago) is reconciled to FAILED")
    void testJobWithExpiredHeartbeatIsReconciledToFailedOnStartup() {
        Instant now = Instant.now();

        ScanJobEntity staleJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("RUNNING")
                .stage("CLASSIFYING_FILES")
                .createdAt(now.minusSeconds(300)) // 5 mins ago
                .startedAt(now.minusSeconds(290))
                .updatedAt(now.minusSeconds(180)) // 3 mins ago (> 2 min threshold)
                .heartbeatAt(now.minusSeconds(180)) // Expired heartbeat (3 mins ago)
                .workerInstanceId("dead-worker-node")
                .build());

        reconciler.reconcileStaleJobsOnStartup();

        ScanJobEntity result = scanJobRepository.findById(staleJob.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getStage()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).isEqualTo(ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE);
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Mixed state reconciliation: preserves recent jobs, reconciles expired jobs, leaves COMPLETED jobs untouched")
    void testReconcileStaleJobsOnStartupWithMixedStates() {
        Instant now = Instant.now();

        // 1. Stale queued job (createdAt 5m ago, no heartbeat) -> FAILED
        ScanJobEntity staleQueuedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("QUEUED")
                .stage("QUEUED")
                .createdAt(now.minusSeconds(300))
                .updatedAt(now.minusSeconds(300))
                .heartbeatAt(now.minusSeconds(300))
                .build());

        // 2. Stale running job (heartbeat 4 mins ago) -> FAILED
        ScanJobEntity staleRunningJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("RUNNING")
                .stage("SCANNING_SECRETS")
                .createdAt(now.minusSeconds(750))
                .startedAt(now.minusSeconds(740))
                .updatedAt(now.minusSeconds(240))
                .heartbeatAt(now.minusSeconds(240))
                .build());

        // 3. Recently active running job from peer instance (heartbeat 10s ago) -> stays RUNNING
        ScanJobEntity recentRunningJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("feature")
                .status("RUNNING")
                .stage("CLASSIFYING_FILES")
                .createdAt(now.minusSeconds(60))
                .startedAt(now.minusSeconds(55))
                .updatedAt(now.minusSeconds(10))
                .heartbeatAt(now.minusSeconds(10))
                .build());

        // 4. Completed job -> untouched
        ScanJobEntity completedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("COMPLETED")
                .stage("COMPLETED")
                .createdAt(now.minusSeconds(1200))
                .startedAt(now.minusSeconds(1190))
                .completedAt(now.minusSeconds(1100))
                .updatedAt(now.minusSeconds(1100))
                .build());

        reconciler.reconcileStaleJobsOnStartup();

        // Verify stale queued job reconciled
        ScanJobEntity updatedStaleQueued = scanJobRepository.findById(staleQueuedJob.getId()).orElseThrow();
        assertThat(updatedStaleQueued.getStatus()).isEqualTo("FAILED");
        assertThat(updatedStaleQueued.getStage()).isEqualTo("FAILED");
        assertThat(updatedStaleQueued.getErrorMessage()).isEqualTo(ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE);
        assertThat(updatedStaleQueued.getCompletedAt()).isNotNull();

        // Verify stale running job reconciled
        ScanJobEntity updatedStaleRunning = scanJobRepository.findById(staleRunningJob.getId()).orElseThrow();
        assertThat(updatedStaleRunning.getStatus()).isEqualTo("FAILED");
        assertThat(updatedStaleRunning.getStage()).isEqualTo("FAILED");
        assertThat(updatedStaleRunning.getErrorMessage()).isEqualTo(ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE);
        assertThat(updatedStaleRunning.getCompletedAt()).isNotNull();

        // Verify recently active job is preserved and NOT marked FAILED
        ScanJobEntity preservedRecent = scanJobRepository.findById(recentRunningJob.getId()).orElseThrow();
        assertThat(preservedRecent.getStatus()).isEqualTo("RUNNING");
        assertThat(preservedRecent.getStage()).isEqualTo("CLASSIFYING_FILES");
        assertThat(preservedRecent.getErrorMessage()).isNull();

        // Verify completed job untouched
        ScanJobEntity untouchedCompleted = scanJobRepository.findById(completedJob.getId()).orElseThrow();
        assertThat(untouchedCompleted.getStatus()).isEqualTo("COMPLETED");
        assertThat(untouchedCompleted.getStage()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("testAtomicReconciliationDoesNotOverwriteRefreshedHeartbeat: Refreshed heartbeat (< 2m) returns 0 updated rows and stays active")
    void testAtomicReconciliationDoesNotOverwriteRefreshedHeartbeat() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(ScanJobRestartReconciler.HEARTBEAT_EXPIRATION_THRESHOLD);

        ScanJobEntity activeJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("RUNNING")
                .stage("SCANNING_SECRETS")
                .createdAt(now.minusSeconds(300))
                .startedAt(now.minusSeconds(290))
                .updatedAt(now.minusSeconds(10))
                .heartbeatAt(now.minusSeconds(10)) // Fresh heartbeat (10 seconds ago)
                .workerInstanceId("node-active")
                .build());

        int updated = scanJobRepository.reconcileStaleJobsAtomic(cutoff, ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE, now);
        assertThat(updated).isEqualTo(0);

        ScanJobEntity refreshed = scanJobRepository.findById(activeJob.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("RUNNING");
        assertThat(refreshed.getStage()).isEqualTo("SCANNING_SECRETS");
        assertThat(refreshed.getErrorMessage()).isNull();
        assertThat(refreshed.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("testAtomicReconciliationTransitionsExpiredJobs: Expired heartbeat (> 2m) transitions to FAILED with safe message")
    void testAtomicReconciliationTransitionsExpiredJobs() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(ScanJobRestartReconciler.HEARTBEAT_EXPIRATION_THRESHOLD);

        ScanJobEntity staleJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("RUNNING")
                .stage("CLASSIFYING_FILES")
                .createdAt(now.minusSeconds(300))
                .startedAt(now.minusSeconds(290))
                .updatedAt(now.minusSeconds(180))
                .heartbeatAt(now.minusSeconds(180)) // Expired (> 2m ago)
                .workerInstanceId("node-crashed")
                .build());

        int updated = scanJobRepository.reconcileStaleJobsAtomic(cutoff, ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE, now);
        assertThat(updated).isEqualTo(1);

        ScanJobEntity refreshed = scanJobRepository.findById(staleJob.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("FAILED");
        assertThat(refreshed.getStage()).isEqualTo("FAILED");
        assertThat(refreshed.getErrorMessage()).isEqualTo(ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE);
        assertThat(refreshed.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("testQueuedJobHeartbeatPreservedByWorkerScheduler: Queued job maintains heartbeat via worker scheduler")
    void testQueuedJobHeartbeatPreservedByWorkerScheduler() {
        Instant initialTime = Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

        ScanJobEntity queuedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .status("QUEUED")
                .stage("QUEUED")
                .createdAt(initialTime)
                .updatedAt(initialTime)
                .heartbeatAt(initialTime)
                .workerInstanceId(scanWorkerInstance.getInstanceId())
                .build());

        heartbeatScheduler.sendHeartbeatForQueuedJobs();

        ScanJobEntity refreshed = scanJobRepository.findById(queuedJob.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("QUEUED");
        assertThat(refreshed.getStage()).isEqualTo("QUEUED");
        assertThat(refreshed.getHeartbeatAt()).isAfter(initialTime);
    }
}
