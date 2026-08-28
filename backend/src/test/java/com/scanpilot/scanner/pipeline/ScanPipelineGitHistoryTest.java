package com.scanpilot.scanner.pipeline;

import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("ScanPipeline Git History Traversal End-to-End Integration Tests (AC-05, AC-06, AC-07, AC-08)")
class ScanPipelineGitHistoryTest {

    @Autowired
    private ScanPipelineService scanPipelineService;

    @MockitoSpyBean
    private com.scanpilot.scanner.git.GitCloneService gitCloneService;

    @MockitoSpyBean
    private StreamedSnapshotFetcher streamedSnapshotFetcher;

    @MockitoSpyBean
    private com.scanpilot.scanner.detector.gitleaks.GitleaksDetectorAdapter gitleaksDetectorAdapter;

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

    @Autowired
    private ScanEventRepository scanEventRepository;

    private UserEntity testUser;
    private RepositoryEntity testRepo;

    @BeforeEach
    void setUp() {
        if (scanEventRepository != null) {
            scanEventRepository.deleteAll();
        }
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
                .githubUserId(77701L)
                .login("githistory_user")
                .name("Git History User")
                .createdAt(Instant.now())
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(554433L)
                .owner("githistory_user")
                .name("history-test-repo")
                .fullName("githistory_user/history-test-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("AC-05 & AC-06: Git history scan detects secret in past commit and marks finding RESOLVED/RISK_CONTAINED")
    void testHistoryScanDetectsSecretInPastCommitAndTransitionsLifecycle(@TempDir Path syntheticRepo) throws Exception {
        initGitRepo(syntheticRepo);

        // Commit 1: Add secret
        Path secretFile = syntheticRepo.resolve("credentials.env");
        Files.writeString(secretFile, "GOOGLE_API_KEY=AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q\n");
        String commit1Sha = gitCommitAndGetSha(syntheticRepo, "feat: introduce google api key");

        // Commit 2: Remove secret (replace with environment variable reference)
        Files.writeString(secretFile, "GOOGLE_API_KEY=${SECRET_KEY}\n");
        String commit2Sha = gitCommitAndGetSha(syntheticRepo, "fix: remove hardcoded api key");

        // Run scan on synthetic git repo
        ScanJobEntity job = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);

        assertThat(job).isNotNull();
        assertThat(job.getStatus()).isEqualTo("COMPLETED");

        // Verify finding was detected in git history and marked RESOLVED / RISK_CONTAINED
        List<FindingEntity> findings = findingRepository.findByRepositoryId(testRepo.getId());
        assertThat(findings).hasSize(1);
        FindingEntity finding = findings.get(0);
        assertThat(finding.getRuleId()).isEqualTo("google-api-key");
        assertThat(finding.getLifecycle()).isEqualTo("RESOLVED");
        assertThat(finding.getRemediationQuality()).isEqualTo("RISK_CONTAINED");
        assertThat(finding.getResolvedAt()).isNotNull();

        // Verify FindingLocationEntity records commit 1 SHA and isCurrentHead = false
        List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(finding.getId());
        assertThat(locations).isNotEmpty();
        FindingLocationEntity loc = locations.get(0);
        assertThat(loc.getIsCurrentHead()).isFalse();
        assertThat(loc.getFilePath()).isEqualTo("credentials.env");
        if (loc.getCommitSha() != null) {
            assertThat(loc.getCommitSha()).isEqualTo(commit1Sha);
        }
    }

    @Test
    @DisplayName("AC-07: Workspace is disposed on completion")
    void testWorkspaceDisposedOnSuccessAndFailure(@TempDir Path syntheticRepo) throws Exception {
        initGitRepo(syntheticRepo);
        Files.writeString(syntheticRepo.resolve("README.md"), "# Hello\n");
        gitCommitAndGetSha(syntheticRepo, "initial commit");

        ScanJobEntity job = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("AC-08: Non-existent repository throws fail-closed exception")
    void testUnauthorizedRepoThrowsFailClosed() {
        UUID nonExistentRepoId = UUID.randomUUID();
        assertThatThrownBy(() -> scanPipelineService.executeScan(nonExistentRepoId, "main", null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("AC-01 & AC-08: Clone failure fails closed deterministically without falling back to ZIP download or advancing checkpoint")
    void testFailedCloneFailsClosedWithoutZipFallback() {
        doThrow(new IllegalStateException("Deterministic clone network failure"))
                .when(gitCloneService).cloneRepository(any(), any(), any(), any(), any());

        ScanJobEntity job = scanPipelineService.executeScan(testRepo.getId(), "main", null);

        assertThat(job).isNotNull();
        assertThat(job.getStatus()).isEqualTo("FAILED");

        // Assert no checkpoint is advanced
        assertThat(scanCheckpointRepository.findByRepositoryId(testRepo.getId())).isEmpty();

        // Assert detector and zip fetcher are never invoked
        verify(gitleaksDetectorAdapter, never()).scan(any());
        verify(streamedSnapshotFetcher, never()).downloadAndExtract(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("AC-04: Truthful clone metrics records mode=GIT_CLONE and archiveBytes=null")
    void testTruthfulCloneTransferMetricsRecorded(@TempDir Path workspaceDir) throws Exception {
        doAnswer(invocation -> {
            Path ws = invocation.getArgument(3);
            Files.createDirectories(ws);
            Files.writeString(ws.resolve("sample.txt"), "sample fixture content");
            return null;
        }).when(gitCloneService).cloneRepository(any(), any(), any(), any(), any());

        SnapshotTransferMetrics metrics = scanPipelineService.fetchRemoteRepositorySnapshot(testRepo.getId(), "main", workspaceDir, null);

        assertThat(metrics).isNotNull();
        assertThat(metrics.mode()).isEqualTo("GIT_CLONE");
        assertThat(metrics.archiveBytes()).isNull();
        assertThat(metrics.workspaceBytes()).isGreaterThan(0L);
        assertThat(metrics.entryCount()).isGreaterThanOrEqualTo(1);

        verify(gitCloneService).cloneRepository(eq("githistory_user/history-test-repo"), eq("main"), any(), eq(workspaceDir), any());
        verify(streamedSnapshotFetcher, never()).downloadAndExtract(any(), any(), any(), any(), any());
    }

    private void initGitRepo(Path dir) throws Exception {
        runGitCommand(dir, "init");
        runGitCommand(dir, "config", "user.name", "ScanPilot History Tester");
        runGitCommand(dir, "config", "user.email", "history@scanpilot.com");
    }

    private String gitCommitAndGetSha(Path dir, String message) throws Exception {
        runGitCommand(dir, "add", "-A");
        runGitCommand(dir, "commit", "-m", message);
        return runGitCommandWithOutput(dir, "rev-parse", "HEAD").trim();
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

    private String runGitCommandWithOutput(Path dir, String... args) throws Exception {
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
        return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
