package com.scanpilot.scanner.pipeline;

import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanCheckpointEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanCheckpointRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.config.SnapshotGuardrailProperties;
import com.scanpilot.scanner.detector.gitleaks.GitleaksDetectorAdapter;
import com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest;
import com.scanpilot.scanner.detector.gitleaks.GitleaksScanResult;
import com.scanpilot.scanner.exception.ResourceGuardrailExceededException;
import com.scanpilot.scanner.workspace.GitWorkspace;
import com.scanpilot.scanner.workspace.GitWorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("Scan Pipeline Guardrail Integration Tests (AC-07, AC-08, R67-09)")
class ScanPipelineGuardrailTest {

    @MockitoSpyBean
    private ScanPipelineService scanPipelineService;

    @MockitoSpyBean
    private StreamedSnapshotFetcher streamedSnapshotFetcher;

    @MockitoSpyBean
    private com.scanpilot.scanner.git.GitCloneService gitCloneService;

    @MockitoSpyBean
    private GitleaksDetectorAdapter gitleaksDetectorAdapter;

    @MockitoSpyBean
    private GitWorkspaceManager gitWorkspaceManager;

    @Autowired
    private SnapshotGuardrailProperties snapshotGuardrailProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private ScanCheckpointRepository scanCheckpointRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private FindingLocationRepository findingLocationRepository;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    @Autowired
    private CoverageRecordRepository coverageRecordRepository;

    @Autowired
    private CoverageItemRepository coverageItemRepository;

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
        scanJobRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(UserEntity.builder()
                .githubUserId(9001L)
                .login("guardrail_tester")
                .name("Guardrail Tester")
                .createdAt(Instant.now())
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(112233L)
                .owner("guardrail_tester")
                .name("guardrail-repo")
                .fullName("guardrail_tester/guardrail-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("AC-07: Guardrail abort persists early INCOMPLETE coverage record and blocks checkpoint advancement")
    void testGuardrailAbortPersistsIncompleteRecordAndBlocksCheckpoint() {
        // Mock git clone service to throw ResourceGuardrailExceededException
        doThrow(new ResourceGuardrailExceededException("REPOSITORY_TOO_LARGE", 25 * 1024 * 1024L, 100, 20 * 1024 * 1024L))
                .when(gitCloneService).cloneRepository(any(), any(), any(), any(), any());

        ScanJobEntity resultJob = scanPipelineService.executeScan(testRepo.getId(), "main", null);

        assertThat(resultJob).isNotNull();
        assertThat(resultJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(resultJob.getStage()).isEqualTo("COMPLETED");
        assertThat(resultJob.getDurationMs()).isNotNull();

        // 1. Assert early CoverageRecordEntity was saved with INCOMPLETE and telemetry details
        Optional<CoverageRecordEntity> coverageOpt = coverageRecordRepository.findByScanJobId(resultJob.getId());
        assertThat(coverageOpt).isPresent();
        CoverageRecordEntity coverage = coverageOpt.get();
        assertThat(coverage.getCoverageImpact()).isEqualTo("INCOMPLETE");
        assertThat(coverage.getReasonCode()).isEqualTo("REPOSITORY_TOO_LARGE");
        assertThat(coverage.getLimitHitValue()).isEqualTo(20 * 1024 * 1024L);
        assertThat(coverage.getTotalBytes()).isEqualTo(25 * 1024 * 1024L);

        // 2. Assert checkpoint advancement is blocked (0 checkpoints saved)
        List<ScanCheckpointEntity> checkpoints = scanCheckpointRepository.findByRepositoryId(testRepo.getId());
        assertThat(checkpoints).isEmpty();
    }

    @Test
    @DisplayName("AC-08: Normal repository scan under limits completes and advances checkpoint")
    void testUnderLimitsScanCompletesAndAdvancesCheckpoint(@TempDir Path syntheticRepo) throws Exception {
        initGitRepo(syntheticRepo);
        Path codeFile = syntheticRepo.resolve("App.java");
        Files.writeString(codeFile, "public class App { public static void main(String[] args) {} }");
        gitCommit(syntheticRepo, "initial commit");

        ScanJobEntity resultJob = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);

        assertThat(resultJob).isNotNull();
        assertThat(resultJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(resultJob.getStage()).isEqualTo("COMPLETED");

        // 1. Assert CoverageRecordEntity is COMPLETE
        Optional<CoverageRecordEntity> coverageOpt = coverageRecordRepository.findByScanJobId(resultJob.getId());
        assertThat(coverageOpt).isPresent();
        CoverageRecordEntity coverage = coverageOpt.get();
        assertThat(coverage.getCoverageImpact()).isEqualTo("COMPLETE");

        // 2. Assert Checkpoint is created and advanced
        Optional<ScanCheckpointEntity> checkpointOpt = scanCheckpointRepository
                .findTopByRepositoryIdAndBranchNameOrderByCreatedAtDesc(testRepo.getId(), "main");
        assertThat(checkpointOpt).isPresent();
        assertThat(checkpointOpt.get().getScanJobId()).isEqualTo(resultJob.getId());
    }

    @Test
    @DisplayName("R67-05: Pipeline strictly guarantees workspace deletion on disk in finally block when guardrail aborts")
    void testPipelineGuaranteesWorkspaceDeletionOnGuardrailAbort() {
        List<Path> createdWorkspaces = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            GitWorkspace ws = (GitWorkspace) invocation.callRealMethod();
            createdWorkspaces.add(ws.workspacePath());
            return ws;
        }).when(gitWorkspaceManager).createWorkspace(any());

        doThrow(new ResourceGuardrailExceededException("REPOSITORY_TOO_LARGE", 25 * 1024 * 1024L, 100, 20 * 1024 * 1024L))
                .when(streamedSnapshotFetcher).downloadAndExtract(any(), any(), any(), any(), any());

        ScanJobEntity resultJob = scanPipelineService.executeScan(testRepo.getId(), "main", null);

        assertThat(resultJob).isNotNull();
        assertThat(createdWorkspaces).isNotEmpty();

        for (Path wsPath : createdWorkspaces) {
            assertThat(Files.exists(wsPath))
                    .as("Workspace directory %s must be completely disposed and purged from disk by ScanPipelineService finally block", wsPath)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("R67-05: Async executeScanJob strictly purges workspace directory on disk when guardrail triggers")
    void testAsyncExecuteScanJobPurgesWorkspaceOnGuardrailTrigger() {
        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("PENDING")
                .createdAt(Instant.now())
                .build());

        List<Path> createdWorkspaces = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            GitWorkspace ws = (GitWorkspace) invocation.callRealMethod();
            createdWorkspaces.add(ws.workspacePath());
            return ws;
        }).when(gitWorkspaceManager).createWorkspace(any());

        doThrow(new ResourceGuardrailExceededException("SCAN_TIMEOUT", 0, 0, 180))
                .when(streamedSnapshotFetcher).downloadAndExtract(any(), any(), any(), any(), any());

        ScanJobEntity resultJob = scanPipelineService.executeScanJob(job.getId());

        assertThat(resultJob).isNotNull();
        assertThat(createdWorkspaces).isNotEmpty();

        for (Path wsPath : createdWorkspaces) {
            assertThat(Files.exists(wsPath))
                    .as("Workspace directory %s must be deleted from filesystem on async scan job timeout", wsPath)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("R67-09: testWholeScanJobDeadlineEnforcesCumulativeTimeoutAcrossStages - stage timeout stops pipeline, records incomplete coverage, and cleans workspace")
    void testWholeScanJobDeadlineEnforcesCumulativeTimeoutAcrossStages(@TempDir Path syntheticRepo) throws Exception {
        initGitRepo(syntheticRepo);
        Path codeFile = syntheticRepo.resolve("App.java");
        Files.writeString(codeFile, "public class App {}");
        gitCommit(syntheticRepo, "commit 1");

        int originalTimeout = snapshotGuardrailProperties.getMaxScanTimeoutSeconds();
        snapshotGuardrailProperties.setMaxScanTimeoutSeconds(1);

        List<Path> createdWorkspaces = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            GitWorkspace ws = (GitWorkspace) invocation.callRealMethod();
            createdWorkspaces.add(ws.workspacePath());
            return ws;
        }).when(gitWorkspaceManager).createWorkspace(any());

        // Simulate snapshot scan stage consuming entire 1s deadline (sleeping 1.1s)
        org.mockito.Mockito.doAnswer(invocation -> {
            Thread.sleep(1100);
            return GitleaksScanResult.success(List.of(), 0, syntheticRepo.toString(), 1100);
        }).when(gitleaksDetectorAdapter).scan(argThat(r -> !r.isGitScan()));

        try {
            ScanJobEntity resultJob = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);

            assertThat(resultJob).isNotNull();
            assertThat(resultJob.getStatus()).isEqualTo("COMPLETED");
            assertThat(resultJob.getStage()).isEqualTo("COMPLETED");

            // Assert CoverageRecordEntity is INCOMPLETE with SCAN_TIMEOUT
            Optional<CoverageRecordEntity> coverageOpt = coverageRecordRepository.findByScanJobId(resultJob.getId());
            assertThat(coverageOpt).isPresent();
            CoverageRecordEntity coverage = coverageOpt.get();
            assertThat(coverage.getCoverageImpact()).isEqualTo("INCOMPLETE");
            assertThat(coverage.getReasonCode()).isEqualTo("SCAN_TIMEOUT");
            assertThat(coverage.getLimitHitValue()).isEqualTo(1L);

            // Assert 0 checkpoints saved
            List<ScanCheckpointEntity> checkpoints = scanCheckpointRepository.findByRepositoryId(testRepo.getId());
            assertThat(checkpoints).isEmpty();

            // Assert workspace directory was purged in finally
            assertThat(createdWorkspaces).isNotEmpty();
            for (Path wsPath : createdWorkspaces) {
                assertThat(Files.exists(wsPath))
                        .as("Workspace directory %s must be deleted on deadline expiry", wsPath)
                        .isFalse();
            }
        } finally {
            snapshotGuardrailProperties.setMaxScanTimeoutSeconds(originalTimeout);
        }
    }

    @Test
    @DisplayName("R67-09: History scan receives computed remaining timeout, not a fresh 180s timeout")
    void testHistoryScanReceivesRemainingTimeoutNotFreshTimeout(@TempDir Path syntheticRepo) throws Exception {
        initGitRepo(syntheticRepo);
        Path codeFile = syntheticRepo.resolve("App.java");
        Files.writeString(codeFile, "public class App {}");
        gitCommit(syntheticRepo, "commit 1");

        int originalTimeout = snapshotGuardrailProperties.getMaxScanTimeoutSeconds();
        snapshotGuardrailProperties.setMaxScanTimeoutSeconds(30);

        ArgumentCaptor<GitleaksScanRequest> requestCaptor = ArgumentCaptor.forClass(GitleaksScanRequest.class);

        // Simulate snapshot scan taking 2 seconds
        org.mockito.Mockito.doAnswer(invocation -> {
            Thread.sleep(2000);
            return GitleaksScanResult.success(List.of(), 0, syntheticRepo.toString(), 2000);
        }).when(gitleaksDetectorAdapter).scan(argThat(r -> !r.isGitScan()));

        try {
            ScanJobEntity resultJob = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);

            assertThat(resultJob).isNotNull();
            assertThat(resultJob.getStatus()).isEqualTo("COMPLETED");

            verify(gitleaksDetectorAdapter, atLeastOnce()).scan(requestCaptor.capture());
            List<GitleaksScanRequest> requests = requestCaptor.getAllValues();

            // Find snapshot request and history request
            GitleaksScanRequest snapshotReq = requests.stream().filter(r -> !r.isGitScan()).findFirst().orElseThrow();
            GitleaksScanRequest historyReq = requests.stream().filter(GitleaksScanRequest::isGitScan).findFirst().orElseThrow();

            assertThat(snapshotReq.overrideTimeoutSeconds()).isNotNull();
            assertThat(snapshotReq.overrideTimeoutSeconds()).isLessThanOrEqualTo(30);

            assertThat(historyReq.overrideTimeoutSeconds()).isNotNull();
            // History scan must strictly receive remaining time (<= 28s because snapshot took 2s), never fresh 30s or 180s
            assertThat(historyReq.overrideTimeoutSeconds()).isLessThanOrEqualTo(28);
            assertThat(historyReq.overrideTimeoutSeconds()).isLessThan(snapshotReq.overrideTimeoutSeconds());
        } finally {
            snapshotGuardrailProperties.setMaxScanTimeoutSeconds(originalTimeout);
        }
    }

    @Test
    @DisplayName("R67-09: testPipelineAbortsWithScanTimeoutWhenFetchStageExhaustsDeadline - fetch stage deadline exhaustion aborts scan, records incomplete coverage, blocks checkpoints, and purges workspace")
    void testPipelineAbortsWithScanTimeoutWhenFetchStageExhaustsDeadline() {
        List<Path> createdWorkspaces = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            GitWorkspace ws = (GitWorkspace) invocation.callRealMethod();
            createdWorkspaces.add(ws.workspacePath());
            return ws;
        }).when(gitWorkspaceManager).createWorkspace(any());

        doThrow(new ResourceGuardrailExceededException("SCAN_TIMEOUT", 1024, 5, 180))
                .when(gitCloneService).cloneRepository(any(), any(), any(), any(), any());

        // 1. Test synchronous executeScan
        ScanJobEntity syncJob = scanPipelineService.executeScan(testRepo.getId(), "main", null);

        assertThat(syncJob).isNotNull();
        assertThat(syncJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(syncJob.getStage()).isEqualTo("COMPLETED");

        Optional<CoverageRecordEntity> syncCoverageOpt = coverageRecordRepository.findByScanJobId(syncJob.getId());
        assertThat(syncCoverageOpt).isPresent();
        CoverageRecordEntity syncCoverage = syncCoverageOpt.get();
        assertThat(syncCoverage.getCoverageImpact()).isEqualTo("INCOMPLETE");
        assertThat(syncCoverage.getReasonCode()).isEqualTo("SCAN_TIMEOUT");
        assertThat(syncCoverage.getLimitHitValue()).isEqualTo(180L);
        assertThat(syncCoverage.getTotalBytes()).isEqualTo(1024L);
        assertThat(syncCoverage.getTotalFiles()).isEqualTo(5);

        List<ScanCheckpointEntity> syncCheckpoints = scanCheckpointRepository.findByRepositoryId(testRepo.getId());
        assertThat(syncCheckpoints).isEmpty();

        // 2. Test asynchronous executeScanJob
        ScanJobEntity queuedJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("PENDING")
                .createdAt(Instant.now())
                .build());

        ScanJobEntity asyncJob = scanPipelineService.executeScanJob(queuedJob.getId());

        assertThat(asyncJob).isNotNull();
        assertThat(asyncJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(asyncJob.getStage()).isEqualTo("COMPLETED");

        Optional<CoverageRecordEntity> asyncCoverageOpt = coverageRecordRepository.findByScanJobId(asyncJob.getId());
        assertThat(asyncCoverageOpt).isPresent();
        CoverageRecordEntity asyncCoverage = asyncCoverageOpt.get();
        assertThat(asyncCoverage.getCoverageImpact()).isEqualTo("INCOMPLETE");
        assertThat(asyncCoverage.getReasonCode()).isEqualTo("SCAN_TIMEOUT");
        assertThat(asyncCoverage.getLimitHitValue()).isEqualTo(180L);
        assertThat(asyncCoverage.getTotalBytes()).isEqualTo(1024L);
        assertThat(asyncCoverage.getTotalFiles()).isEqualTo(5);

        List<ScanCheckpointEntity> allCheckpoints = scanCheckpointRepository.findByRepositoryId(testRepo.getId());
        assertThat(allCheckpoints).isEmpty();

        // 3. Workspace cleanup assertion
        assertThat(createdWorkspaces).isNotEmpty();
        for (Path wsPath : createdWorkspaces) {
            assertThat(Files.exists(wsPath))
                    .as("Workspace directory %s must be purged from disk when fetch stage exhausts deadline", wsPath)
                    .isFalse();
        }
    }

    private void initGitRepo(Path dir) throws Exception {
        runGitCommand(dir, "init");
        runGitCommand(dir, "config", "user.name", "ScanPilot Guardrail Test");
        runGitCommand(dir, "config", "user.email", "guardrail@scanpilot.com");
    }

    private void gitCommit(Path dir, String message) throws Exception {
        runGitCommand(dir, "add", "-A");
        runGitCommand(dir, "commit", "-m", message);
    }

    private void runGitCommand(Path dir, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
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
