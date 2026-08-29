package com.scanpilot.project.service;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.dto.GitHubRepositoryDto;
import com.scanpilot.github.service.GitHubAppService;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserInstallationEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserInstallationRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.project.dto.SelectRepositoryRequest;
import com.scanpilot.project.model.MonitoredProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@DisplayName("ProjectService Two-Level Installation Authorization Tests")
class ProjectServiceVerifiedInstallationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserInstallationRepository userInstallationRepository;

    @MockBean
    private GitHubAppService gitHubAppService;

    private UserSession testSession;
    private UserEntity userEntity;
    private Long installationId;
    private Long githubRepoId;

    @BeforeEach
    void setUp() {
        Long githubUserId = 123456L;
        installationId = 889900L;
        githubRepoId = 445566L;

        userEntity = userRepository.save(UserEntity.builder()
                .githubUserId(githubUserId)
                .login("octocat")
                .name("The Octocat")
                .email("octocat@github.local")
                .createdAt(Instant.now())
                .build());

        testSession = new UserSession(
                "session-1",
                githubUserId,
                "octocat",
                "The Octocat",
                "avatar",
                "octocat@github.local",
                "ghu_test_token",
                installationId,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    @DisplayName("AC-03: Should bind installationId and persist verified server metadata when Level 1 and Level 2 pass")
    void testSelectRepositoryBindsVerifiedInstallation() {
        // Level 1: Save verified association in DB
        userInstallationRepository.save(UserInstallationEntity.builder()
                .userId(userEntity.getId())
                .githubUserId(userEntity.getGithubUserId())
                .installationId(installationId)
                .accountLogin("octocat-org")
                .accountType("Organization")
                .verifiedAt(Instant.now())
                .build());

        // Level 2: GitHub API returns repository in user-accessible list
        when(gitHubAppService.getUserAccessibleInstallationRepositories("ghu_test_token", installationId))
                .thenReturn(List.of(
                        new GitHubRepositoryDto(githubRepoId, "secure-app", "octocat-org/secure-app", "octocat-org", "main", true, "https://github.com/octocat-org/secure-app", "desc", false)
                ));

        SelectRepositoryRequest request = new SelectRepositoryRequest(
                githubRepoId,
                "octocat-org/secure-app",
                "octocat-org",
                "secure-app",
                "main",
                true
        );

        MonitoredProject project = projectService.selectRepository(testSession, request);
        assertThat(project).isNotNull();

        Optional<RepositoryEntity> repoInDb = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), githubRepoId);
        assertThat(repoInDb).isPresent();
        assertThat(repoInDb.get().getInstallationId()).isEqualTo(installationId);
        assertThat(repoInDb.get().getOwner()).isEqualTo("octocat-org");
        assertThat(repoInDb.get().getName()).isEqualTo("secure-app");
        assertThat(repoInDb.get().getFullName()).isEqualTo("octocat-org/secure-app");
        assertThat(repoInDb.get().getIsPrivate()).isTrue();
    }

    @Test
    @DisplayName("Remediation 5: Should derive metadata from trusted server DTO and ignore forged client request fields")
    void testSelectRepositoryPersistsVerifiedServerMetadataIgnoringForgedClientRequest() {
        userInstallationRepository.save(UserInstallationEntity.builder()
                .userId(userEntity.getId())
                .githubUserId(userEntity.getGithubUserId())
                .installationId(installationId)
                .accountLogin("trusted-org")
                .accountType("Organization")
                .verifiedAt(Instant.now())
                .build());

        // Genuine server DTO
        when(gitHubAppService.getUserAccessibleInstallationRepositories("ghu_test_token", installationId))
                .thenReturn(List.of(
                        new GitHubRepositoryDto(githubRepoId, "genuine-app", "trusted-org/genuine-app", "trusted-org", "main", true, "https://github.com/trusted-org/genuine-app", "Genuine description", false)
                ));

        // Forged client request details
        SelectRepositoryRequest forgedRequest = new SelectRepositoryRequest(
                githubRepoId,
                "fake-owner/fake-name",
                "fake-owner",
                "fake-name",
                "hacked-branch",
                false // forged public flag
        );

        MonitoredProject project = projectService.selectRepository(testSession, forgedRequest);
        assertThat(project).isNotNull();

        Optional<RepositoryEntity> repoInDb = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), githubRepoId);
        assertThat(repoInDb).isPresent();
        // Assert server metadata prevailed
        assertThat(repoInDb.get().getOwner()).isEqualTo("trusted-org");
        assertThat(repoInDb.get().getName()).isEqualTo("genuine-app");
        assertThat(repoInDb.get().getFullName()).isEqualTo("trusted-org/genuine-app");
        assertThat(repoInDb.get().getDefaultBranch()).isEqualTo("main");
        assertThat(repoInDb.get().getIsPrivate()).isTrue();
        assertThat(repoInDb.get().getInstallationId()).isEqualTo(installationId);
    }

    @Test
    @DisplayName("Remediation 5: Should preserve existing verified repository metadata during unverified selection attempt")
    void testUnverifiedSelectionDoesNotCorruptAlreadyVerifiedRepository() {
        // Pre-existing repository is verified bound
        repositoryRepository.save(RepositoryEntity.builder()
                .userId(userEntity.getId())
                .githubRepoId(githubRepoId)
                .installationId(installationId)
                .owner("verified-org")
                .name("verified-repo")
                .fullName("verified-org/verified-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(true)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build());

        // Unverified session (Level 1 absent)
        UserSession unverifiedSession = new UserSession(
                "session-unverified",
                userEntity.getGithubUserId(),
                "octocat",
                "The Octocat",
                "avatar",
                "octocat@github.local",
                "ghu_unverified_token",
                null, // no installation
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        SelectRepositoryRequest forgedRequest = new SelectRepositoryRequest(
                githubRepoId,
                "attacker/tampered",
                "attacker",
                "tampered",
                "dev",
                false
        );

        projectService.selectRepository(unverifiedSession, forgedRequest);

        Optional<RepositoryEntity> repoInDb = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), githubRepoId);
        assertThat(repoInDb).isPresent();
        // Metadata must remain preserved from verified state
        assertThat(repoInDb.get().getInstallationId()).isEqualTo(installationId);
        assertThat(repoInDb.get().getOwner()).isEqualTo("verified-org");
        assertThat(repoInDb.get().getName()).isEqualTo("verified-repo");
        assertThat(repoInDb.get().getFullName()).isEqualTo("verified-org/verified-repo");
        assertThat(repoInDb.get().getIsPrivate()).isTrue();
    }

    @Test
    @DisplayName("AC-03: Should fail closed (installationId = null) when selected repository is not in user-accessible repo list (Level 2 fail)")
    void testSelectRepositoryRejectsRepoNotInUserAccessibleList() {
        // Level 1: Verified association exists
        userInstallationRepository.save(UserInstallationEntity.builder()
                .userId(userEntity.getId())
                .githubUserId(userEntity.getGithubUserId())
                .installationId(installationId)
                .accountLogin("octocat-org")
                .accountType("Organization")
                .verifiedAt(Instant.now())
                .build());

        // Level 2: User accessible repo list DOES NOT contain selected repository
        when(gitHubAppService.getUserAccessibleInstallationRepositories("ghu_test_token", installationId))
                .thenReturn(List.of(
                        new GitHubRepositoryDto(999999L, "other-app", "octocat-org/other-app", "octocat-org", "main", false, "https://github.com/octocat-org/other-app", "desc", false)
                ));

        SelectRepositoryRequest request = new SelectRepositoryRequest(
                githubRepoId,
                "octocat-org/secure-app",
                "octocat-org",
                "secure-app",
                "main",
                true
        );

        MonitoredProject project = projectService.selectRepository(testSession, request);
        assertThat(project).isNotNull();

        Optional<RepositoryEntity> repoInDb = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), githubRepoId);
        assertThat(repoInDb).isPresent();
        // Fail-closed: installationId remains null
        assertThat(repoInDb.get().getInstallationId()).isNull();
    }

    @Test
    @DisplayName("AC-03: Should fail closed (installationId = null) when installation is not verified for this user (Level 1 fail)")
    void testUnverifiedInstallationLeavesRepoUnbound() {
        // Level 1: user_installations is empty for this user

        SelectRepositoryRequest request = new SelectRepositoryRequest(
                githubRepoId,
                "octocat-org/secure-app",
                "octocat-org",
                "secure-app",
                "main",
                true
        );

        MonitoredProject project = projectService.selectRepository(testSession, request);
        assertThat(project).isNotNull();

        Optional<RepositoryEntity> repoInDb = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), githubRepoId);
        assertThat(repoInDb).isPresent();
        assertThat(repoInDb.get().getInstallationId()).isNull();
    }

    @Test
    @DisplayName("AC-03: Should find repository when returned on paginated repository page")
    void testPaginationTraversesAllRepositoryPages() {
        userInstallationRepository.save(UserInstallationEntity.builder()
                .userId(userEntity.getId())
                .githubUserId(userEntity.getGithubUserId())
                .installationId(installationId)
                .accountLogin("octocat-org")
                .accountType("Organization")
                .verifiedAt(Instant.now())
                .build());

        when(gitHubAppService.getUserAccessibleInstallationRepositories("ghu_test_token", installationId))
                .thenReturn(List.of(
                        new GitHubRepositoryDto(1001L, "repo-1", "octocat-org/repo-1", "octocat-org", "main", false, "https://github.com/octocat-org/repo-1", "desc", false),
                        new GitHubRepositoryDto(1002L, "repo-2", "octocat-org/repo-2", "octocat-org", "main", false, "https://github.com/octocat-org/repo-2", "desc", false),
                        new GitHubRepositoryDto(githubRepoId, "secure-app", "octocat-org/secure-app", "octocat-org", "main", true, "https://github.com/octocat-org/secure-app", "desc", false)
                ));

        SelectRepositoryRequest request = new SelectRepositoryRequest(
                githubRepoId,
                "octocat-org/secure-app",
                "octocat-org",
                "secure-app",
                "main",
                true
        );

        projectService.selectRepository(testSession, request);

        Optional<RepositoryEntity> repoInDb = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), githubRepoId);
        assertThat(repoInDb).isPresent();
        assertThat(repoInDb.get().getInstallationId()).isEqualTo(installationId);
    }
}
