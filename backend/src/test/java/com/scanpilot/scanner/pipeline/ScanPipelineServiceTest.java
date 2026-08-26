package com.scanpilot.scanner.pipeline;

import com.scanpilot.persistence.entity.CoverageItemEntity;
import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
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
import com.scanpilot.scanner.detector.gitleaks.GitleaksDetectorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.File;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("Scan Pipeline Service Integration Tests")
class ScanPipelineServiceTest {

    @MockitoSpyBean
    private ScanPipelineService scanPipelineService;

    @MockitoSpyBean
    private GitleaksDetectorAdapter gitleaksDetectorAdapter;

    @MockitoSpyBean
    private StreamedSnapshotFetcher streamedSnapshotFetcher;

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
            .githubUserId(8001L)
            .login("pipeline_tester")
            .name("Pipeline Tester")
            .createdAt(Instant.now())
            .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
            .userId(testUser.getId())
            .githubRepoId(998877L)
            .owner("pipeline_tester")
            .name("test-repo")
            .fullName("pipeline_tester/test-repo")
            .defaultBranch("main")
            .primaryBranch("main")
            .isPrivate(false)
            .status("ACTIVE")
            .monitoredAt(Instant.now())
            .build());
    }

    @Nested
    @DisplayName("Three-Stage Finding Lifecycle Journey (FR-051, FR-007, FR-018, FR-019, DEC-012)")
    class ThreeStageLifecycleTests {

        @Test
        @DisplayName("Demonstrates OPEN/ACTION_REQUIRED -> RESOLVED/RISK_CONTAINED -> RESOLVED/VERIFIED_COMPLETE -> REGRESSED")
        void shouldExecuteCompleteLifecycleJourney(@TempDir Path syntheticRepo) throws Exception {
            initGitRepo(syntheticRepo);

            // =========================================================================
            // Stage 1: Initial commit with exposed Google API Key at HEAD
            // Expectation: OPEN / ACTION_REQUIRED
            // =========================================================================
            Path configFile = syntheticRepo.resolve("config.properties");
            Files.writeString(configFile, "apiKey=AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q\n");
            gitCommit(syntheticRepo, "feat: add initial configuration with api key");

            ScanJobEntity job1 = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);
            assertThat(job1.getStatus()).isEqualTo("COMPLETED");
            assertThat(job1.getDurationMs()).isNotNull();

            // Verify Coverage Record
            Optional<CoverageRecordEntity> coverage1 = coverageRecordRepository.findByScanJobId(job1.getId());
            assertThat(coverage1).isPresent();
            assertThat(coverage1.get().getTotalFiles()).isGreaterThanOrEqualTo(1);
            assertThat(coverage1.get().getScannedFiles()).isGreaterThanOrEqualTo(1);

            // Verify Finding in DB
            List<FindingEntity> findings1 = findingRepository.findByRepositoryId(testRepo.getId());
            assertThat(findings1).hasSize(1);
            FindingEntity finding1 = findings1.get(0);
            assertThat(finding1.getLifecycle()).isEqualTo("OPEN");
            assertThat(finding1.getRemediationQuality()).isEqualTo("ACTION_REQUIRED");
            assertThat(finding1.getSeverity()).isEqualTo("CRITICAL");
            assertThat(finding1.getFingerprint()).isNotBlank();
            assertThat(finding1.getResolvedAt()).isNull();

            // Verify Location is at HEAD
            List<FindingLocationEntity> locs1 = findingLocationRepository.findByFindingId(finding1.getId());
            assertThat(locs1).hasSize(1);
            assertThat(locs1.get(0).getIsCurrentHead()).isTrue();
            assertThat(locs1.get(0).getFilePath()).isEqualTo("config.properties");

            // Verify Evidence Item
            List<EvidenceItemEntity> evidence1 = evidenceItemRepository.findByFindingId(finding1.getId());
            assertThat(evidence1).isNotEmpty();
            assertThat(evidence1.get(0).getMaskedSecret()).startsWith("AIzaSy");
            assertThat(evidence1.get(0).getMaskedSecret()).doesNotContain("A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q");
            assertThat(evidence1.get(0).getVerificationStatus()).isEqualTo("OBSERVED");

            // Verify Checkpoint created
            Optional<ScanCheckpointEntity> checkpoint1 = scanCheckpointRepository
                .findTopByRepositoryIdAndBranchNameOrderByCreatedAtDesc(testRepo.getId(), "main");
            assertThat(checkpoint1).isPresent();
            assertThat(checkpoint1.get().getScanJobId()).isEqualTo(job1.getId());

            // =========================================================================
            // Stage 2: Secret removed from HEAD in a new commit, but remains in history
            // Expectation: RESOLVED / RISK_CONTAINED
            // =========================================================================
            Files.writeString(configFile, "apiKey=REPLACED_SAFE_VALUE\n");
            gitCommit(syntheticRepo, "fix: remove hardcoded api key from source");

            ScanJobEntity job2 = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);
            assertThat(job2.getStatus()).isEqualTo("COMPLETED");

            List<FindingEntity> findings2 = findingRepository.findByRepositoryId(testRepo.getId());
            assertThat(findings2).hasSize(1);
            FindingEntity finding2 = findings2.get(0);
            assertThat(finding2.getId()).isEqualTo(finding1.getId()); // Same durable identity
            assertThat(finding2.getLifecycle()).isEqualTo("RESOLVED");
            assertThat(finding2.getRemediationQuality()).isEqualTo("RISK_CONTAINED");
            assertThat(finding2.getResolvedAt()).isNotNull();

            // Location should now reflect historical commit (not current HEAD)
            List<FindingLocationEntity> locs2 = findingLocationRepository.findByFindingId(finding2.getId());
            assertThat(locs2).isNotEmpty();
            assertThat(locs2.stream().noneMatch(FindingLocationEntity::getIsCurrentHead)).isTrue();

            // =========================================================================
            // Stage 3: Clean rewrite where secret is purged completely from Git history
            // Expectation: RESOLVED / VERIFIED_COMPLETE
            // =========================================================================
            // Create a clean synthetic repo without the secret anywhere in history
            Path cleanRepo = syntheticRepo.getParent().resolve("clean-synthetic-repo-" + UUID.randomUUID());
            Files.createDirectories(cleanRepo);
            initGitRepo(cleanRepo);
            Files.writeString(cleanRepo.resolve("config.properties"), "apiKey=CLEAN_ENVIRONMENT_REF\n");
            gitCommit(cleanRepo, "initial commit without secrets");

            ScanJobEntity job3 = scanPipelineService.executeScan(testRepo.getId(), "main", cleanRepo);
            assertThat(job3.getStatus()).isEqualTo("COMPLETED");

            List<FindingEntity> findings3 = findingRepository.findByRepositoryId(testRepo.getId());
            assertThat(findings3).hasSize(1);
            FindingEntity finding3 = findings3.get(0);
            assertThat(finding3.getId()).isEqualTo(finding1.getId());
            assertThat(finding3.getLifecycle()).isEqualTo("RESOLVED");
            assertThat(finding3.getRemediationQuality()).isEqualTo("VERIFIED_COMPLETE");
            assertThat(finding3.getResolvedAt()).isNotNull();

            // =========================================================================
            // Stage 4: Regression - Secret re-appears at current HEAD
            // Expectation: REGRESSED / ACTION_REQUIRED
            // =========================================================================
            Files.writeString(cleanRepo.resolve("config.properties"), "apiKey=AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q\n");
            gitCommit(cleanRepo, "chore: accidentally reverted api key");

            ScanJobEntity job4 = scanPipelineService.executeScan(testRepo.getId(), "main", cleanRepo);
            assertThat(job4.getStatus()).isEqualTo("COMPLETED");

            List<FindingEntity> findings4 = findingRepository.findByRepositoryId(testRepo.getId());
            assertThat(findings4).hasSize(1);
            FindingEntity finding4 = findings4.get(0);
            assertThat(finding4.getId()).isEqualTo(finding1.getId());
            assertThat(finding4.getLifecycle()).isEqualTo("REGRESSED");
            assertThat(finding4.getRemediationQuality()).isEqualTo("ACTION_REQUIRED");
            assertThat(finding4.getResolvedAt()).isNull();

            List<FindingLocationEntity> locs4 = findingLocationRepository.findByFindingId(finding4.getId());
            assertThat(locs4).isNotEmpty();
            assertThat(locs4.stream().anyMatch(FindingLocationEntity::getIsCurrentHead)).isTrue();
        }
    }

    @Nested
    @DisplayName("Fail-Closed Remote Snapshot Tests (Issue #53, FR-025)")
    class FailClosedRemoteSnapshotTests {

        @Test
        @DisplayName("Configured secondary branch with unavailable remote snapshot fails job, creates zero findings/coverage/checkpoints, and does NOT fallback to default branch")
        void testConfiguredSecondaryBranchSnapshotFailureFailsJobWithoutFallback() {
            org.mockito.Mockito.doThrow(new IllegalStateException("Remote repository snapshot for branch 'develop' could not be acquired or verified"))
                .when(streamedSnapshotFetcher).downloadAndExtract(any(), argThat(url -> url != null && url.contains("/zipball/develop")), any(), any(), any());

            // Target branch 'develop' is requested with sourcePath=null (remote snapshot download will fail deterministically without network)
            ScanJobEntity job = scanPipelineService.executeScan(testRepo.getId(), "develop", null);

            assertThat(job).isNotNull();
            assertEquals("FAILED", job.getStatus());
            assertThat(job.getStatus()).isEqualTo("FAILED");
            assertThat(job.getErrorMessage()).contains("Remote repository snapshot for branch 'develop' could not be acquired or verified");
            assertThat(job.getCompletedAt()).isNotNull();

            // Verify transport seam called for /zipball/develop and never for fallback /zipball
            verify(streamedSnapshotFetcher).downloadAndExtract(any(), argThat(url -> url != null && url.contains("/zipball/develop")), any(), any(), any());
            verify(streamedSnapshotFetcher, never()).downloadAndExtract(any(), argThat(url -> url != null && url.endsWith("/zipball")), any(), any(), any());

            // Zero CoverageRecord recorded
            assertTrue(coverageRecordRepository.findAll().stream().filter(c -> c.getScanJobId().equals(job.getId())).findAny().isEmpty());
            assertThat(coverageRecordRepository.findByScanJobId(job.getId())).isEmpty();

            // Zero Finding recorded
            assertTrue(findingRepository.findByRepositoryId(testRepo.getId()).isEmpty());
            assertThat(findingRepository.findByRepositoryId(testRepo.getId())).isEmpty();

            // Zero ScanCheckpoint recorded for requested branch or default branch
            assertTrue(scanCheckpointRepository.findTopByRepositoryIdAndBranchNameOrderByCreatedAtDesc(testRepo.getId(), "develop").isEmpty());
            assertTrue(scanCheckpointRepository.findTopByRepositoryIdAndBranchNameOrderByCreatedAtDesc(testRepo.getId(), "main").isEmpty());
            assertTrue(scanCheckpointRepository.findByRepositoryId(testRepo.getId()).isEmpty());

            // Zero FindingLocation and EvidenceItem recorded
            assertTrue(findingLocationRepository.findAll().isEmpty());
            assertTrue(evidenceItemRepository.findAll().isEmpty());

            // Verify detector is never invoked when snapshot acquisition fails
            verify(gitleaksDetectorAdapter, never()).scan(any());
        }

        @Test
        @DisplayName("executeScanJob(jobId) transitions through monotonic stages and fails closed on download failure")
        void testExecuteScanJobMonotonicStagesAndFailureSanitization() {
            ScanJobEntity queuedJob = scanJobRepository.save(ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .scanMode("SNAPSHOT_AND_HISTORY")
                    .status("QUEUED")
                    .stage("QUEUED")
                    .createdAt(Instant.now())
                    .build());

            org.mockito.Mockito.doThrow(new IllegalStateException("Remote repository snapshot for branch 'main' could not be acquired or verified"))
                    .when(streamedSnapshotFetcher).downloadAndExtract(any(), any(), any(), any(), any());

            ScanJobEntity result = scanPipelineService.executeScanJob(queuedJob.getId());

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getStage()).isEqualTo("FAILED");
            assertThat(result.getErrorMessage()).contains("Remote repository snapshot for branch 'main' could not be acquired or verified");
            assertThat(result.getDurationMs()).isNotNull();

            ScanJobEntity persisted = scanJobRepository.findById(queuedJob.getId()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo("FAILED");
            assertThat(persisted.getStage()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("executeScanJob error sanitization strips tokens and credentials (ghp_... and password=...) from Entity, DTO, and log formatting")
        void testExecuteScanJobSanitizesRawTokensInErrorMessage() {
            String secretToken = "ghp_1234567890abcdefghijklmnopqrstuvwxyz";
            String passwordMarker = "password=SuperSecretPassword123!";
            ScanJobEntity queuedJob = scanJobRepository.save(ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .scanMode("SNAPSHOT_AND_HISTORY")
                    .status("QUEUED")
                    .stage("QUEUED")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());

            org.mockito.Mockito.doThrow(new RuntimeException("GitHub error with token " + secretToken + ", Bearer secret_token_xyz, and " + passwordMarker))
                    .when(scanPipelineService).fetchRemoteRepositorySnapshot(any(), any(), any(), any());

            ScanJobEntity result = scanPipelineService.executeScanJob(queuedJob.getId());

            // 1) Persisted Entity error message does not contain secret markers
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getStage()).isEqualTo("FAILED");
            assertThat(result.getErrorMessage()).doesNotContain(secretToken);
            assertThat(result.getErrorMessage()).doesNotContain("secret_token_xyz");
            assertThat(result.getErrorMessage()).doesNotContain("SuperSecretPassword123!");
            assertThat(result.getErrorMessage()).contains("[REDACTED_TOKEN]");
            assertThat(result.getErrorMessage()).contains("password=[REDACTED]");

            ScanJobEntity persisted = scanJobRepository.findById(queuedJob.getId()).orElseThrow();
            assertThat(persisted.getErrorMessage()).doesNotContain(secretToken);
            assertThat(persisted.getErrorMessage()).doesNotContain("SuperSecretPassword123!");
            assertThat(persisted.getUpdatedAt()).isNotNull();

            // 2) ScanJobDto errorMessage does not contain secret marker
            com.scanpilot.scanner.dto.ScanJobDto dto = com.scanpilot.scanner.dto.ScanJobDto.from(persisted);
            assertThat(dto.errorMessage()).doesNotContain(secretToken);
            assertThat(dto.errorMessage()).doesNotContain("SuperSecretPassword123!");
            assertThat(dto.errorMessage()).contains("[REDACTED_TOKEN]");

            // 3) Direct sanitization method verification
            String sanitized = scanPipelineService.sanitizeErrorMessage("Failure with secret: " + secretToken + " and " + passwordMarker);
            assertThat(sanitized).doesNotContain(secretToken);
            assertThat(sanitized).doesNotContain("SuperSecretPassword123!");
            assertThat(sanitized).contains("[REDACTED_TOKEN]");
        }
    }

    @Nested
    @DisplayName("Atomic Heartbeat Non-Regression on Terminal States (P1-B, FR-002)")
    class AtomicHeartbeatTerminalStateTests {

        @Test
        @DisplayName("testRunningJobHeartbeatStopsOnCompletion: When scan completes (COMPLETED/FAILED), task heartbeat stops and update query does not alter terminal state")
        void testRunningJobHeartbeatStopsOnCompletion() {
            Instant initialTime = Instant.now().minusSeconds(100).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            ScanJobEntity completedJob = scanJobRepository.save(ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .status("COMPLETED")
                    .stage("COMPLETED")
                    .workerInstanceId("test-worker-1")
                    .createdAt(initialTime)
                    .updatedAt(initialTime)
                    .heartbeatAt(initialTime)
                    .completedAt(initialTime)
                    .build());

            Instant newHeartbeatTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            int updatedRows = scanJobRepository.updateHeartbeatForRunningJob(completedJob.getId(), newHeartbeatTime);
            assertThat(updatedRows).isEqualTo(0);

            ScanJobEntity refreshed = scanJobRepository.findById(completedJob.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo("COMPLETED");
            assertThat(refreshed.getStage()).isEqualTo("COMPLETED");
            assertThat(refreshed.getHeartbeatAt()).isEqualTo(initialTime);
        }

        @Test
        @DisplayName("Atomic heartbeat update does not overwrite COMPLETED job status or stage")
        void testHeartbeatDoesNotOverwriteCompletedJob() {
            Instant initialTime = Instant.now().minusSeconds(100).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            ScanJobEntity completedJob = scanJobRepository.save(ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .status("COMPLETED")
                    .stage("COMPLETED")
                    .workerInstanceId("test-worker-1")
                    .createdAt(initialTime)
                    .updatedAt(initialTime)
                    .heartbeatAt(initialTime)
                    .completedAt(initialTime)
                    .build());

            Instant newHeartbeatTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            int queuedUpdated = scanJobRepository.updateHeartbeatForQueuedJobsByWorker("test-worker-1", newHeartbeatTime);
            int runningUpdated = scanJobRepository.updateHeartbeatForRunningJob(completedJob.getId(), newHeartbeatTime);
            assertThat(queuedUpdated).isEqualTo(0);
            assertThat(runningUpdated).isEqualTo(0);

            ScanJobEntity refreshed = scanJobRepository.findById(completedJob.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo("COMPLETED");
            assertThat(refreshed.getStage()).isEqualTo("COMPLETED");
            assertThat(refreshed.getHeartbeatAt()).isEqualTo(initialTime);
        }

        @Test
        @DisplayName("Atomic heartbeat update does not overwrite FAILED job status or stage")
        void testHeartbeatDoesNotOverwriteFailedJob() {
            Instant initialTime = Instant.now().minusSeconds(100).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            ScanJobEntity failedJob = scanJobRepository.save(ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .status("FAILED")
                    .stage("FAILED")
                    .errorMessage("Original error")
                    .workerInstanceId("test-worker-1")
                    .createdAt(initialTime)
                    .updatedAt(initialTime)
                    .heartbeatAt(initialTime)
                    .completedAt(initialTime)
                    .build());

            Instant newHeartbeatTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            int queuedUpdated = scanJobRepository.updateHeartbeatForQueuedJobsByWorker("test-worker-1", newHeartbeatTime);
            int runningUpdated = scanJobRepository.updateHeartbeatForRunningJob(failedJob.getId(), newHeartbeatTime);
            assertThat(queuedUpdated).isEqualTo(0);
            assertThat(runningUpdated).isEqualTo(0);

            ScanJobEntity refreshed = scanJobRepository.findById(failedJob.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo("FAILED");
            assertThat(refreshed.getStage()).isEqualTo("FAILED");
            assertThat(refreshed.getErrorMessage()).isEqualTo("Original error");
            assertThat(refreshed.getHeartbeatAt()).isEqualTo(initialTime);
        }

        @Test
        @DisplayName("Atomic heartbeat updates successfully update QUEUED and RUNNING jobs according to lifecycle")
        void testHeartbeatUpdatesQueuedAndRunningJobs() {
            Instant initialTime = Instant.now().minusSeconds(100).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            ScanJobEntity queuedJob = scanJobRepository.save(ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .status("QUEUED")
                    .stage("QUEUED")
                    .workerInstanceId("test-worker-1")
                    .createdAt(initialTime)
                    .updatedAt(initialTime)
                    .heartbeatAt(initialTime)
                    .build());

            ScanJobEntity runningJob = scanJobRepository.save(ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .status("RUNNING")
                    .stage("CLASSIFYING_FILES")
                    .workerInstanceId("test-worker-1")
                    .createdAt(initialTime)
                    .updatedAt(initialTime)
                    .heartbeatAt(initialTime)
                    .build());

            Instant newHeartbeatTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            int queuedUpdated = scanJobRepository.updateHeartbeatForQueuedJobsByWorker("test-worker-1", newHeartbeatTime);
            int runningUpdated = scanJobRepository.updateHeartbeatForRunningJob(runningJob.getId(), newHeartbeatTime);
            assertThat(queuedUpdated).isEqualTo(1);
            assertThat(runningUpdated).isEqualTo(1);

            ScanJobEntity refreshedQueued = scanJobRepository.findById(queuedJob.getId()).orElseThrow();
            assertThat(refreshedQueued.getStatus()).isEqualTo("QUEUED");
            assertThat(refreshedQueued.getStage()).isEqualTo("QUEUED");
            assertThat(refreshedQueued.getHeartbeatAt()).isAfter(initialTime);

            ScanJobEntity refreshedRunning = scanJobRepository.findById(runningJob.getId()).orElseThrow();
            assertThat(refreshedRunning.getStatus()).isEqualTo("RUNNING");
            assertThat(refreshedRunning.getStage()).isEqualTo("CLASSIFYING_FILES");
            assertThat(refreshedRunning.getHeartbeatAt()).isAfter(initialTime);
        }
    }

    private void initGitRepo(Path dir) throws Exception {
        runGitCommand(dir, "init");
        runGitCommand(dir, "config", "user.name", "ScanPilot Test");
        runGitCommand(dir, "config", "user.email", "tester@scanpilot.com");
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
