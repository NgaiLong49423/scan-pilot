package com.scanpilot.scanner.pipeline;

import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanCheckpointRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.git.GitCloneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("Scan Pipeline Exact-SHA Checkout Tests (Issue #54)")
class ScanPipelineExactShaCheckoutTest {

    @Autowired
    private ScanPipelineService scanPipelineService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private ScanCheckpointRepository scanCheckpointRepository;

    @MockitoSpyBean
    private GitCloneService gitCloneService;

    @MockitoSpyBean
    private com.scanpilot.github.service.GitHubAppAuthService gitHubAppAuthService;

    private RepositoryEntity repository;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.doReturn("ghs_test_token_123")
                .when(gitHubAppAuthService).createInstallationAccessToken(any());

        scanJobRepository.deleteAll();
        scanCheckpointRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(123456L)
                .login("test-owner")
                .createdAt(Instant.now())
                .build());

        repository = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(987654L)
                .installationId(555666L)
                .fullName("test-owner/test-repo")
                .status("ACTIVE")
                .updatedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Should pass exact expected commit SHA to GitCloneService and verify workspace HEAD for webhook PR scan")
    void testWebhookPrPassesExpectedCommitShaToGitClone() {
        String expectedSha = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repository.getId())
                .branchName("feature-pr")
                .status("QUEUED")
                .stage("QUEUED")
                .triggerType("WEBHOOK_PULL_REQUEST")
                .expectedCommitSha(expectedSha)
                .prNumber(42)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .heartbeatAt(Instant.now())
                .build());

        doAnswer(invocation -> {
            java.nio.file.Path ws = invocation.getArgument(4);
            java.nio.file.Files.createDirectories(ws.resolve(".git"));
            java.nio.file.Files.writeString(ws.resolve(".git/HEAD"), expectedSha);
            return null;
        }).when(gitCloneService).cloneRepository(
                eq("test-owner/test-repo"),
                eq("feature-pr"),
                eq(expectedSha),
                any(),
                any(),
                any()
        );

        ScanJobEntity completedJob = scanPipelineService.executeScanJob(job.getId());

        assertThat(completedJob.getStatus()).isEqualTo("COMPLETED");
        assertThat(completedJob.getCommitSha()).isEqualTo(expectedSha);
        verify(gitCloneService).cloneRepository(
                eq("test-owner/test-repo"),
                eq("feature-pr"),
                eq(expectedSha),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Should fail closed with COMMIT_CHECKOUT_FAILED when workspace HEAD is null/unproven (R54-B3-01)")
    void testWorkspaceHeadNullFailsClosedWithCommitCheckoutFailed() {
        String expectedSha = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repository.getId())
                .branchName("feature-pr")
                .status("QUEUED")
                .stage("QUEUED")
                .triggerType("WEBHOOK_PULL_REQUEST")
                .expectedCommitSha(expectedSha)
                .prNumber(42)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .heartbeatAt(Instant.now())
                .build());

        doAnswer(invocation -> {
            // Mock git clone succeeds without creating .git/HEAD
            return null;
        }).when(gitCloneService).cloneRepository(
                eq("test-owner/test-repo"),
                eq("feature-pr"),
                eq(expectedSha),
                any(),
                any(),
                any()
        );

        ScanJobEntity failedJob = scanPipelineService.executeScanJob(job.getId());

        assertThat(failedJob.getStatus()).isEqualTo("FAILED");
        assertThat(failedJob.getCommitSha()).isNull(); // Zero unverified commit attribution
        assertThat(failedJob.getErrorMessage()).contains("COMMIT_CHECKOUT_FAILED");

        // Zero checkpoint advanced
        assertThat(scanCheckpointRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should fail closed with zero fake SHA and zero checkpoint when git checkout fails for webhook scan")
    void testCheckoutFailureFailsClosedWithoutFakeShaOrCheckpoint() {
        String expectedSha = "bad0000000000000000000000000000000000000";
        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repository.getId())
                .branchName("feature-broken")
                .status("QUEUED")
                .stage("QUEUED")
                .triggerType("WEBHOOK_PUSH")
                .expectedCommitSha(expectedSha)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .heartbeatAt(Instant.now())
                .build());

        doThrow(new IllegalStateException("Git clone failed with exit code 128"))
                .when(gitCloneService).cloneRepository(
                        eq("test-owner/test-repo"),
                        eq("feature-broken"),
                        eq(expectedSha),
                        any(),
                        any(),
                        any()
                );

        ScanJobEntity failedJob = scanPipelineService.executeScanJob(job.getId());

        assertThat(failedJob.getStatus()).isEqualTo("FAILED");
        assertThat(failedJob.getCommitSha()).isNull(); // Zero fake HEAD-<uuid> SHA

        // Zero checkpoint advanced
        assertThat(scanCheckpointRepository.findAll()).isEmpty();
    }
}
