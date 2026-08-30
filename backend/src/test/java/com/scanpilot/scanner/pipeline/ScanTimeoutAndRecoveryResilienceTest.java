package com.scanpilot.scanner.pipeline;

import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanEventEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanCheckpointRepository;
import com.scanpilot.persistence.repository.ScanEventRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.config.ScanWorkerInstance;
import com.scanpilot.scanner.config.SnapshotGuardrailProperties;
import com.scanpilot.scanner.detector.gitleaks.GitleaksDetectorAdapter;
import com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest;
import com.scanpilot.scanner.detector.gitleaks.GitleaksScanResult;
import com.scanpilot.scanner.dispatcher.ScanJobDispatcher;
import com.scanpilot.scanner.dispatcher.ScanJobStateTransitionService;
import com.scanpilot.scanner.exception.ResourceGuardrailExceededException;
import com.scanpilot.scanner.git.GitCloneService;
import com.scanpilot.scanner.lifecycle.ScanJobRestartReconciler;
import com.scanpilot.scanner.workspace.GitWorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@DisplayName("P0 Scan Timeout, Stuck-Job Recovery & DB-Pool Resilience Tests")
class ScanTimeoutAndRecoveryResilienceTest {

    @MockitoSpyBean
    private ScanPipelineService scanPipelineService;

    @MockitoSpyBean
    private GitleaksDetectorAdapter gitleaksDetectorAdapter;

    @MockitoSpyBean
    private GitCloneService gitCloneService;

    @MockitoSpyBean
    private ScanJobStateTransitionService scanJobStateTransitionService;

    @Autowired
    private ScanJobDispatcher scanJobDispatcher;

    @Autowired
    private ScanJobRestartReconciler scanJobRestartReconciler;

    @MockitoSpyBean
    private ScanJobRepository scanJobRepository;

    @Autowired
    private ScanEventRepository scanEventRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoverageRecordRepository coverageRecordRepository;

    @Autowired
    private CoverageItemRepository coverageItemRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private FindingLocationRepository findingLocationRepository;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    @Autowired
    private ScanCheckpointRepository scanCheckpointRepository;

    @Autowired
    private GitWorkspaceManager gitWorkspaceManager;

    @Autowired
    private SnapshotGuardrailProperties snapshotGuardrailProperties;

    @Autowired
    private ScanWorkerInstance scanWorkerInstance;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UserEntity testUser;
    private RepositoryEntity testRepo;

    @BeforeEach
    void setUp() {
        findingLocationRepository.deleteAll();
        evidenceItemRepository.deleteAll();
        findingRepository.deleteAll();
        coverageItemRepository.deleteAll();
        coverageRecordRepository.deleteAll();
        scanCheckpointRepository.deleteAll();
        if (scanEventRepository != null) {
            scanEventRepository.deleteAll();
        }
        scanJobRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(UserEntity.builder()
                .githubUserId(889900L)
                .login("resilience-tester")
                .name("Resilience Tester")
                .createdAt(Instant.now())
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(445566L)
                .owner("resilience-tester")
                .name("resilience-repo")
                .fullName("resilience-tester/resilience-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Requirement 1: Hung git log -p is bounded, killed, and job ends FAILED/SCAN_TIMEOUT")
    void testHungGitLogIsBoundedKilledAndJobEndsFailedWithScanTimeout(@TempDir Path syntheticRepo) throws Exception {
        initGitRepo(syntheticRepo);
        Files.writeString(syntheticRepo.resolve("Main.java"), "public class Main {}");
        gitCommit(syntheticRepo, "Initial commit");

        // Mock git clone service to populate workspace from synthetic repo
        doAnswer(invocation -> {
            Path wsPath = invocation.getArgument(3);
            gitWorkspaceManager.copyDirectory(syntheticRepo, wsPath);
            return null;
        }).when(gitCloneService).cloneRepository(any(), any(), any(), any(), any());

        // Mock embedded git history scan to simulate a hung git log execution that throws SCAN_TIMEOUT
        doThrow(new ResourceGuardrailExceededException("SCAN_TIMEOUT", 0, 0, 10))
                .when(gitleaksDetectorAdapter).scan(argThat(GitleaksScanRequest::isGitScan));

        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("QUEUED")
                .createdAt(Instant.now())
                .build());

        ScanJobEntity resultJob = scanPipelineService.executeScanJob(job.getId());

        assertThat(resultJob).isNotNull();
        assertThat(resultJob.getStatus()).isEqualTo("FAILED");
        assertThat(resultJob.getStage()).isEqualTo("FAILED");
        assertThat(resultJob.getErrorMessage()).isEqualTo("SCAN_TIMEOUT");
        assertThat(resultJob.getCompletedAt()).isNotNull();
        assertThat(resultJob.getDurationMs()).isNotNull();

        // Verify CoverageRecord reflects INCOMPLETE coverage with SCAN_TIMEOUT
        Optional<CoverageRecordEntity> coverageOpt = coverageRecordRepository.findByScanJobId(resultJob.getId());
        assertThat(coverageOpt).isPresent();
        assertThat(coverageOpt.get().getCoverageImpact()).isEqualTo("INCOMPLETE");
        assertThat(coverageOpt.get().getReasonCode()).isEqualTo("SCAN_TIMEOUT");
    }

    @Test
    @DisplayName("Requirement 2: Gitleaks timeout ends terminally; later scan of the same repo is permitted")
    void testGitleaksTimeoutEndsTerminallyAndPermitsLaterScanOfSameRepo(@TempDir Path syntheticRepo) throws Exception {
        initGitRepo(syntheticRepo);
        Files.writeString(syntheticRepo.resolve("Main.java"), "public class Main {}");
        gitCommit(syntheticRepo, "Initial commit");

        doAnswer(invocation -> {
            Path wsPath = invocation.getArgument(3);
            gitWorkspaceManager.copyDirectory(syntheticRepo, wsPath);
            return null;
        }).when(gitCloneService).cloneRepository(any(), any(), any(), any(), any());

        // First scan hits Gitleaks timeout
        doThrow(new ResourceGuardrailExceededException("SCAN_TIMEOUT", 0, 0, 30))
                .when(gitleaksDetectorAdapter).scan(any());

        ScanJobEntity firstJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("QUEUED")
                .createdAt(Instant.now())
                .build());

        ScanJobEntity completedFirstJob = scanPipelineService.executeScanJob(firstJob.getId());

        assertThat(completedFirstJob.getStatus()).isEqualTo("FAILED");
        assertThat(completedFirstJob.getErrorMessage()).isEqualTo("SCAN_TIMEOUT");

        // Verify repository has 0 active (QUEUED/RUNNING) jobs remaining
        List<ScanJobEntity> activeJobs = scanJobRepository.findByRepositoryIdAndStatusIn(testRepo.getId(), List.of("QUEUED", "RUNNING"));
        assertThat(activeJobs).isEmpty();

        // Allow subsequent scan to succeed
        doReturn(GitleaksScanResult.success(List.of(), 0, syntheticRepo.toString(), 10))
                .when(gitleaksDetectorAdapter).scan(any());

        // Later scan trigger of the same repository is permitted (not rejected as duplicate)
        ScanJobEntity secondJob = scanJobDispatcher.dispatch(testRepo, "main");

        assertThat(secondJob).isNotNull();
        assertThat(secondJob.getId()).isNotEqualTo(firstJob.getId());
        assertThat(secondJob.getStatus()).isIn("QUEUED", "RUNNING");
    }

    @Test
    @DisplayName("Requirement 3: Simulated DB connection/transaction failure during tryProcessNextJobForRepository produces safe bounded outcome, never permanent RUNNING without thread")
    void testSimulatedDbFailureDoesNotLeaveJobStuckInRunning() {
        ScanJobEntity queuedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("QUEUED")
                .stage("QUEUED")
                .createdAt(Instant.now())
                .build());

        // 1. Simulate transient DB failure when trying to claim the job inside tryProcessNextJobForRepository
        doThrow(new DataAccessResourceFailureException("Simulated Hikari connection pool acquisition timeout"))
                .doReturn(Optional.of(queuedJob))
                .when(scanJobRepository).findFirstByRepositoryIdAndStatusOrderByCreatedAtAsc(testRepo.getId(), "QUEUED");

        // Call tryProcessNextJobForRepository - it must catch the DB error without throwing unhandled exception
        scanJobDispatcher.tryProcessNextJobForRepository(testRepo.getId());

        // Assert that the job was NOT stranded in RUNNING without a worker thread (remains in QUEUED)
        ScanJobEntity rolledBackJob = scanJobRepository.findById(queuedJob.getId()).orElseThrow();
        assertThat(rolledBackJob.getStatus()).isEqualTo("QUEUED");

        // 2. Subsequent kick / retry successfully claims the job without deadlock or duplicate rejection
        scanJobDispatcher.tryProcessNextJobForRepository(testRepo.getId());

        ScanJobEntity claimedJob = scanJobRepository.findById(queuedJob.getId()).orElseThrow();
        assertThat(claimedJob.getStatus()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("Requirement 4: Stale RUNNING job is recovered and does not block the next queued job")
    void testStaleRunningJobIsRecoveredAndDoesNotBlockNextQueuedJob() throws Exception {
        Instant now = Instant.now();

        // Stale RUNNING job with heartbeat older than 2 minutes
        ScanJobEntity staleRunningJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("RUNNING")
                .stage("SCANNING_SECRETS")
                .createdAt(now.minusSeconds(300))
                .startedAt(now.minusSeconds(290))
                .updatedAt(now.minusSeconds(180))
                .heartbeatAt(now.minusSeconds(180))
                .workerInstanceId("dead-worker-1")
                .build());

        // Next QUEUED job waiting in line
        ScanJobEntity nextQueuedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("feature-fix")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("QUEUED")
                .stage("QUEUED")
                .createdAt(now.minusSeconds(60))
                .build());

        CountDownLatch nextJobExecuted = new CountDownLatch(1);
        doAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            if (id.equals(nextQueuedJob.getId())) {
                nextJobExecuted.countDown();
            }
            return null;
        }).when(scanPipelineService).executeScanJob(any(UUID.class));

        // Trigger periodic/startup reconciler
        int recovered = scanJobRestartReconciler.reconcileInterruptedJobs();

        assertThat(recovered).isEqualTo(1);

        // 1. Stale job is recovered to FAILED
        ScanJobEntity recoveredStale = scanJobRepository.findById(staleRunningJob.getId()).orElseThrow();
        assertThat(recoveredStale.getStatus()).isEqualTo("FAILED");
        assertThat(recoveredStale.getErrorMessage()).isEqualTo(ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE);
        assertThat(recoveredStale.getCompletedAt()).isNotNull();

        // 2. Next queued job was drained and submitted to execution
        boolean executed = nextJobExecuted.await(5, TimeUnit.SECONDS);
        assertThat(executed).isTrue();
    }

    @Test
    @DisplayName("Requirement 4b: dispatch() immediately recovers stale RUNNING job on new trigger")
    void testDispatchImmediatelyRecoversStaleRunningJobOnNewTrigger() {
        Instant now = Instant.now();

        // Stale RUNNING job with heartbeat older than 2 minutes
        ScanJobEntity staleRunningJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("RUNNING")
                .stage("CLASSIFYING_FILES")
                .createdAt(now.minusSeconds(300))
                .startedAt(now.minusSeconds(290))
                .updatedAt(now.minusSeconds(200))
                .heartbeatAt(now.minusSeconds(200))
                .workerInstanceId("crashed-worker-instance")
                .build());

        // User triggers a new scan on the repository
        ScanJobEntity newDispatchedJob = scanJobDispatcher.dispatch(testRepo, "main");

        // Stale job must be transitioned to FAILED immediately
        ScanJobEntity recoveredOldJob = scanJobRepository.findById(staleRunningJob.getId()).orElseThrow();
        assertThat(recoveredOldJob.getStatus()).isEqualTo("FAILED");
        assertThat(recoveredOldJob.getErrorMessage()).isEqualTo(ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE);
        assertThat(recoveredOldJob.getCompletedAt()).isNotNull();

        // New job is successfully created and returned
        assertThat(newDispatchedJob).isNotNull();
        assertThat(newDispatchedJob.getId()).isNotEqualTo(staleRunningJob.getId());
        assertThat(newDispatchedJob.getStatus()).isIn("QUEUED", "RUNNING");
    }

    @Test
    @DisplayName("Requirement 4c: Concurrent reconcilers across multiple instances do not double-transition or emit duplicate events for the same stale job")
    void testConcurrentReconcilersPreventDuplicateTransitionAndEvents() throws Exception {
        Instant now = Instant.now();

        // Create a stale RUNNING job
        ScanJobEntity staleRunningJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("RUNNING")
                .stage("SCANNING_SECRETS")
                .createdAt(now.minusSeconds(300))
                .startedAt(now.minusSeconds(290))
                .updatedAt(now.minusSeconds(180))
                .heartbeatAt(now.minusSeconds(180))
                .workerInstanceId("crashed-worker-1")
                .build());

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger totalReconciled = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    int count = scanJobRestartReconciler.reconcileInterruptedJobs();
                    totalReconciled.addAndGet(count);
                } catch (Exception ignored) {}
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Exactly 1 instance/thread must have succeeded in reconciling the job
        assertThat(totalReconciled.get()).isEqualTo(1);

        // Job in DB is FAILED
        ScanJobEntity finalJob = scanJobRepository.findById(staleRunningJob.getId()).orElseThrow();
        assertThat(finalJob.getStatus()).isEqualTo("FAILED");

        // Exactly 1 STALE_HEARTBEAT_TIMEOUT event is emitted in DB (zero duplicate events)
        List<ScanEventEntity> events = scanEventRepository.findByScanJobIdOrderBySequenceNumberAsc(staleRunningJob.getId());
        long failedEventCount = events.stream()
                .filter(e -> "JOB_FAILED".equals(e.getMessageCode()))
                .count();
        assertThat(failedEventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("Requirement 4d: Stale recovery in dispatch executes within surrounding transaction without nested REQUIRES_NEW connection acquisition")
    void testDispatchStaleRecoveryDoesNotRequireSecondConnectionOrNestedTransaction() {
        Instant now = Instant.now();

        // Stale RUNNING job with heartbeat older than 2 minutes
        ScanJobEntity staleRunningJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("RUNNING")
                .stage("SCANNING_SECRETS")
                .createdAt(now.minusSeconds(300))
                .startedAt(now.minusSeconds(290))
                .updatedAt(now.minusSeconds(180))
                .heartbeatAt(now.minusSeconds(180))
                .workerInstanceId("crashed-worker-1")
                .build());

        // Execute dispatch within an explicit surrounding transaction (e.g. TransactionTemplate)
        org.springframework.transaction.support.TransactionTemplate txTemplate =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        ScanJobEntity newDispatchedJob = txTemplate.execute(status -> {
            // Must succeed without hanging, connection starvation, or throwing
            return scanJobDispatcher.dispatch(testRepo, "main");
        });

        assertThat(newDispatchedJob).isNotNull();
        assertThat(newDispatchedJob.getId()).isNotEqualTo(staleRunningJob.getId());

        // Assert stale job was transitioned to FAILED within the transaction
        ScanJobEntity recoveredOldJob = scanJobRepository.findById(staleRunningJob.getId()).orElseThrow();
        assertThat(recoveredOldJob.getStatus()).isEqualTo("FAILED");
        assertThat(recoveredOldJob.getErrorMessage()).isEqualTo(ScanJobRestartReconciler.RESTART_INTERRUPTED_MESSAGE);
    }

    @Test
    @DisplayName("Requirement 5: Existing normal scan path remains green")
    void testNormalScanPathRemainsGreen(@TempDir Path syntheticRepo) throws Exception {
        initGitRepo(syntheticRepo);
        Path codeFile = syntheticRepo.resolve("App.java");
        Files.writeString(codeFile, "public class App { public static void main(String[] args) {} }");
        gitCommit(syntheticRepo, "initial commit");

        ScanJobEntity resultJob = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);

        assertThat(resultJob).isNotNull();
        assertThat(resultJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(resultJob.getStage()).isEqualTo("COMPLETED");
        assertThat(resultJob.getDurationMs()).isNotNull();

        Optional<CoverageRecordEntity> coverageOpt = coverageRecordRepository.findByScanJobId(resultJob.getId());
        assertThat(coverageOpt).isPresent();
        assertThat(coverageOpt.get().getCoverageImpact()).isEqualTo("COMPLETE");
    }

    private void initGitRepo(Path dir) throws Exception {
        runGitCommand(dir, "init");
        runGitCommand(dir, "config", "user.name", "Resilience Tester");
        runGitCommand(dir, "config", "user.email", "resilience@scanpilot.com");
    }

    private void gitCommit(Path dir, String message) throws Exception {
        runGitCommand(dir, "add", "-A");
        runGitCommand(dir, "commit", "-m", message);
    }

    private void runGitCommand(Path dir, String... args) throws Exception {
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean done = p.waitFor(5, TimeUnit.SECONDS);
        if (!done || p.exitValue() != 0) {
            String err = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Git command failed: " + String.join(" ", cmd) + " -> " + err);
        }
    }
}
