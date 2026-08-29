package com.scanpilot.scanner.pipeline;

import com.scanpilot.github.service.GitHubAppAuthService;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserSessionEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.persistence.repository.UserSessionRepository;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("Scan Pipeline Installation Token Authentication Tests (Issue #54)")
class ScanPipelineInstallationTokenAuthTest {

    @Autowired
    private ScanPipelineService scanPipelineService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @MockitoSpyBean
    private GitHubAppAuthService gitHubAppAuthService;

    @MockitoSpyBean
    private GitCloneService gitCloneService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        scanJobRepository.deleteAll();
        userSessionRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(UserEntity.builder()
                .githubUserId(123456L)
                .login("test-owner")
                .createdAt(Instant.now())
                .build());

        // Add user session with user OAuth token
        userSessionRepository.save(UserSessionEntity.builder()
                .sessionId("sess-" + UUID.randomUUID())
                .userId(user.getId())
                .accessToken("user-oauth-token-secret")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());
    }

    @Test
    @DisplayName("Should use GitHub App installation token exclusively for webhook scans and ignore user OAuth token")
    void testWebhookScanUsesInstallationTokenExclusively() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(987654L)
                .installationId(777888L)
                .fullName("test-owner/test-repo")
                .status("ACTIVE")
                .updatedAt(Instant.now())
                .build());

        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repo.getId())
                .branchName("main")
                .status("QUEUED")
                .stage("QUEUED")
                .triggerType("WEBHOOK_PUSH")
                .expectedCommitSha("4b825dc642cb6eb9a060e54bf8d69288fbee4904")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .heartbeatAt(Instant.now())
                .build());

        doReturn("ghs_installation_access_token_xyz")
                .when(gitHubAppAuthService).createInstallationAccessToken(eq(777888L));

        doAnswer(invocation -> {
            java.nio.file.Path ws = invocation.getArgument(4);
            java.nio.file.Files.createDirectories(ws.resolve(".git"));
            java.nio.file.Files.writeString(ws.resolve(".git/HEAD"), "4b825dc642cb6eb9a060e54bf8d69288fbee4904");
            return null;
        }).when(gitCloneService).cloneRepository(
                any(), any(), any(), any(), any(), any()
        );

        ScanJobEntity completedJob = scanPipelineService.executeScanJob(job.getId());

        assertThat(completedJob.getStatus()).isEqualTo("COMPLETED");

        // Assert gitHubAppAuthService called with installationId
        verify(gitHubAppAuthService).createInstallationAccessToken(eq(777888L));

        // Assert GitCloneService received installation token, NOT user OAuth token
        verify(gitCloneService).cloneRepository(
                eq("test-owner/test-repo"),
                eq("main"),
                eq("4b825dc642cb6eb9a060e54bf8d69288fbee4904"),
                eq("ghs_installation_access_token_xyz"),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Should fail closed when repository has null installationId for webhook scan")
    void testMissingInstallationFailsClosed() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(987654L)
                .installationId(null) // Unlinked repository
                .fullName("test-owner/unlinked-repo")
                .status("ACTIVE")
                .updatedAt(Instant.now())
                .build());

        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repo.getId())
                .branchName("main")
                .status("QUEUED")
                .stage("QUEUED")
                .triggerType("WEBHOOK_PUSH")
                .expectedCommitSha("4b825dc642cb6eb9a060e54bf8d69288fbee4904")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .heartbeatAt(Instant.now())
                .build());

        ScanJobEntity failedJob = scanPipelineService.executeScanJob(job.getId());

        assertThat(failedJob.getStatus()).isEqualTo("FAILED");
        assertThat(failedJob.getErrorMessage()).contains("INSTALLATION_TOKEN_UNAVAILABLE");

        // Verify gitCloneService was NEVER called
        verify(gitCloneService, never()).cloneRepository(any(), any(), any(), any(), any(), any());
    }
}
