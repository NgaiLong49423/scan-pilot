package com.scanpilot.scanner.remediation;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.service.GitHubAppAuthService;
import com.scanpilot.github.service.GitHubPullRequestClient;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.FindingRemediationPrLinkEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRemediationPrLinkRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.dto.CreateFindingRemediationPrRequest;
import com.scanpilot.scanner.dto.FindingRemediationPrLinkDto;
import com.scanpilot.scanner.dto.FindingRemediationPrPreviewDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindingRemediationPrServiceStateMachineTest {

    private static final String SHA_1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String SHA_2 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private FindingLocationRepository findingLocationRepository;

    @Mock
    private FindingRemediationPrLinkRepository linkRepository;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpringConfigurationPatcher patcher;

    @Mock
    private FindingRemediationPrTokenService tokenService;

    @Mock
    private GitHubPullRequestClient gitHubPullRequestClient;

    @Mock
    private GitHubAppAuthService gitHubAppAuthService;

    @InjectMocks
    private FindingRemediationPrService remediationService;

    private UserSession session;
    private UserEntity user;
    private RepositoryEntity repo;
    private FindingEntity finding;
    private FindingLocationEntity location;

    @BeforeEach
    void setUp() {
        session = UserSession.builder()
                .sessionId("sess-123")
                .githubUserId(12345L)
                .login("test-user")
                .name("Test User")
                .avatarUrl("https://avatar")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        user = UserEntity.builder()
                .id(UUID.randomUUID())
                .githubUserId(12345L)
                .login("test-user")
                .name("Test User")
                .build();

        repo = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .githubRepoId(99999L)
                .installationId(11111L)
                .owner("scanpilot-org")
                .name("target-repo")
                .defaultBranch("main")
                .build();

        finding = FindingEntity.builder()
                .id(UUID.randomUUID())
                .repositoryId(repo.getId())
                .ruleId("SP-CONFIG-001")
                .severity("HIGH")
                .title("Exposed Secret")
                .build();

        location = FindingLocationEntity.builder()
                .id(UUID.randomUUID())
                .findingId(finding.getId())
                .filePath("src/main/resources/application.properties")
                .startLine(3)
                .commitSha(SHA_1)
                .build();
    }

    @Test
    @DisplayName("Generates preview for SP-CONFIG-001 with masked diff and token when location matches HEAD")
    void testGeneratePreviewSuccess() {
        when(findingRepository.findById(finding.getId())).thenReturn(Optional.of(finding));
        when(repositoryRepository.findById(finding.getRepositoryId())).thenReturn(Optional.of(repo));
        when(userRepository.findByGithubUserId(session.getGithubUserId())).thenReturn(Optional.of(user));
        when(gitHubAppAuthService.isConfigured()).thenReturn(true);
        when(gitHubAppAuthService.createInstallationAccessToken(repo.getInstallationId())).thenReturn("ghs_mockToken");
        when(findingLocationRepository.findByFindingId(finding.getId())).thenReturn(List.of(location));
        when(patcher.isSupportedConfigFile("src/main/resources/application.properties")).thenReturn(true);

        when(gitHubPullRequestClient.getDefaultBranchHead("scanpilot-org", "target-repo", "ghs_mockToken"))
                .thenReturn(new GitHubPullRequestClient.DefaultBranchHead("main", SHA_1));
        when(gitHubPullRequestClient.getFileContent("scanpilot-org", "target-repo", "src/main/resources/application.properties", SHA_1, "ghs_mockToken"))
                .thenReturn("spring.datasource.password=secret123");

        SpringConfigurationPatcher.PatchResult patchResult = SpringConfigurationPatcher.PatchResult.ok(
                "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}",
                "spring.datasource.password=***",
                "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}",
                "SPRING_DATASOURCE_PASSWORD"
        );
        when(patcher.createPatch("src/main/resources/application.properties", "spring.datasource.password=secret123", 3, null))
                .thenReturn(patchResult);

        when(tokenService.computePatchPlanHash(finding.getId(), SHA_1, "src/main/resources/application.properties", 3, patchResult.patchedLine()))
                .thenReturn("patch-plan-hash-789");
        when(tokenService.generateToken(finding.getId(), repo.getId(), SHA_1, "patch-plan-hash-789"))
                .thenReturn("signed.preview.token");
        when(linkRepository.findByFindingId(finding.getId())).thenReturn(Optional.empty());

        FindingRemediationPrPreviewDto preview = remediationService.generatePreview(finding.getId(), session);

        assertThat(preview).isNotNull();
        assertThat(preview.findingId()).isEqualTo(finding.getId());
        assertThat(preview.envVariableName()).isEqualTo("SPRING_DATASOURCE_PASSWORD");
        assertThat(preview.originalLineMasked()).isEqualTo("spring.datasource.password=***");
        assertThat(preview.patchedLine()).isEqualTo("spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}");
        assertThat(preview.previewToken()).isEqualTo("signed.preview.token");
        assertThat(preview.revocationWarning()).contains("MANDATORY REVOCATION NOTICE");
    }

    @Test
    @DisplayName("P1-1: Preview fails closed with 409 STALE_REVISION_ERROR when location commit differs from default branch HEAD")
    void testGeneratePreviewRejectsSourceProvenanceMismatch() {
        location.setCommitSha(SHA_1); // Old commit
        when(findingRepository.findById(finding.getId())).thenReturn(Optional.of(finding));
        when(repositoryRepository.findById(finding.getRepositoryId())).thenReturn(Optional.of(repo));
        when(userRepository.findByGithubUserId(session.getGithubUserId())).thenReturn(Optional.of(user));
        when(gitHubAppAuthService.isConfigured()).thenReturn(true);
        when(gitHubAppAuthService.createInstallationAccessToken(repo.getInstallationId())).thenReturn("ghs_mockToken");
        when(findingLocationRepository.findByFindingId(finding.getId())).thenReturn(List.of(location));
        when(patcher.isSupportedConfigFile("src/main/resources/application.properties")).thenReturn(true);

        // HEAD moved to SHA_2
        when(gitHubPullRequestClient.getDefaultBranchHead("scanpilot-org", "target-repo", "ghs_mockToken"))
                .thenReturn(new GitHubPullRequestClient.DefaultBranchHead("main", SHA_2));

        assertThatThrownBy(() -> remediationService.generatePreview(finding.getId(), session))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).contains("STALE_REVISION_ERROR");
                });
    }

    @Test
    @DisplayName("P1-2: getRemediationPrLink rejects cross-tenant / unauthorized access with 403 Forbidden")
    void testGetRemediationPrLinkRejectsCrossTenant() {
        UUID otherUserId = UUID.randomUUID();
        repo.setUserId(otherUserId); // Belongs to different user

        when(findingRepository.findById(finding.getId())).thenReturn(Optional.of(finding));
        when(repositoryRepository.findById(finding.getRepositoryId())).thenReturn(Optional.of(repo));
        when(userRepository.findByGithubUserId(session.getGithubUserId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> remediationService.getRemediationPrLink(finding.getId(), session))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("P1-3: Raw diagnostic leakage prevention — GitHub exception with token/path yields safe static error")
    void testRawDiagnosticLeakageSanitized() {
        String tokenString = "valid.preview.token";
        FindingRemediationPrTokenService.VerifiedRemediationToken verifiedToken =
                new FindingRemediationPrTokenService.VerifiedRemediationToken(finding.getId(), repo.getId(), SHA_1, "hash", 100, 1000);

        when(tokenService.validateToken(tokenString, finding.getId(), null, null)).thenReturn(verifiedToken);
        when(findingRepository.findById(finding.getId())).thenReturn(Optional.of(finding));
        when(repositoryRepository.findById(finding.getRepositoryId())).thenReturn(Optional.of(repo));
        when(userRepository.findByGithubUserId(session.getGithubUserId())).thenReturn(Optional.of(user));
        when(gitHubAppAuthService.isConfigured()).thenReturn(true);
        when(gitHubAppAuthService.createInstallationAccessToken(repo.getInstallationId())).thenReturn("ghs_mockToken");
        when(findingLocationRepository.findByFindingId(finding.getId())).thenReturn(List.of(location));
        when(gitHubPullRequestClient.getDefaultBranchHead("scanpilot-org", "target-repo", "ghs_mockToken"))
                .thenReturn(new GitHubPullRequestClient.DefaultBranchHead("main", SHA_1));
        when(linkRepository.findByFindingIdAndSourceRevisionCommit(finding.getId(), SHA_1)).thenReturn(Optional.empty());
        when(gitHubPullRequestClient.getFileContent("scanpilot-org", "target-repo", "src/main/resources/application.properties", SHA_1, "ghs_mockToken"))
                .thenReturn("spring.datasource.password=secret123");

        SpringConfigurationPatcher.PatchResult patchResult = SpringConfigurationPatcher.PatchResult.ok(
                "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}",
                "spring.datasource.password=***",
                "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}",
                "SPRING_DATASOURCE_PASSWORD"
        );
        when(patcher.createPatch("src/main/resources/application.properties", "spring.datasource.password=secret123", 3, null))
                .thenReturn(patchResult);
        when(tokenService.computePatchPlanHash(finding.getId(), SHA_1, "src/main/resources/application.properties", 3, patchResult.patchedLine()))
                .thenReturn("hash");

        when(linkRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        // Simulate GitHub client throwing exception containing a sensitive token and internal URI
        when(gitHubPullRequestClient.createPullRequest(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("GitHub error for uri: https://api.github.com/repos/private/repo?token=ghp_superSecretToken12345"));

        assertThatThrownBy(() -> remediationService.createRemediationPr(finding.getId(), new CreateFindingRemediationPrRequest(tokenString), session))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    // Must be sanitized static message, never contains token or raw message
                    assertThat(rse.getReason()).isEqualTo("REMEDIATION_PR_CREATION_FAILED");
                    assertThat(rse.getReason()).doesNotContain("ghp_");
                    assertThat(rse.getReason()).doesNotContain("private/repo");
                });

        ArgumentCaptor<FindingRemediationPrLinkEntity> entityCaptor = ArgumentCaptor.forClass(FindingRemediationPrLinkEntity.class);
        verify(linkRepository).save(entityCaptor.capture());
        FindingRemediationPrLinkEntity saved = entityCaptor.getValue();
        assertThat(saved.getState()).isEqualTo("FAILED");
        assertThat(saved.getFailureReason()).isEqualTo("REMEDIATION_PR_CREATION_FAILED");
        assertThat(saved.getFailureReason()).doesNotContain("ghp_");
    }

    @Test
    @DisplayName("Creates remediation PR successfully with valid token and matching HEAD")
    void testCreateRemediationPrSuccess() {
        String tokenString = "valid.preview.token";
        String targetSha = SHA_1;
        String patchPlanHash = "patch-plan-hash-789";

        FindingRemediationPrTokenService.VerifiedRemediationToken verifiedToken =
                new FindingRemediationPrTokenService.VerifiedRemediationToken(finding.getId(), repo.getId(), targetSha, patchPlanHash, 100, 1000);

        when(tokenService.validateToken(tokenString, finding.getId(), null, null)).thenReturn(verifiedToken);
        when(findingRepository.findById(finding.getId())).thenReturn(Optional.of(finding));
        when(repositoryRepository.findById(finding.getRepositoryId())).thenReturn(Optional.of(repo));
        when(userRepository.findByGithubUserId(session.getGithubUserId())).thenReturn(Optional.of(user));
        when(gitHubAppAuthService.isConfigured()).thenReturn(true);
        when(gitHubAppAuthService.createInstallationAccessToken(repo.getInstallationId())).thenReturn("ghs_mockToken");
        when(findingLocationRepository.findByFindingId(finding.getId())).thenReturn(List.of(location));

        when(gitHubPullRequestClient.getDefaultBranchHead("scanpilot-org", "target-repo", "ghs_mockToken"))
                .thenReturn(new GitHubPullRequestClient.DefaultBranchHead("main", targetSha));

        when(linkRepository.findByFindingIdAndSourceRevisionCommit(finding.getId(), targetSha)).thenReturn(Optional.empty());

        when(gitHubPullRequestClient.getFileContent("scanpilot-org", "target-repo", "src/main/resources/application.properties", targetSha, "ghs_mockToken"))
                .thenReturn("spring.datasource.password=secret123");

        SpringConfigurationPatcher.PatchResult patchResult = SpringConfigurationPatcher.PatchResult.ok(
                "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}",
                "spring.datasource.password=***",
                "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}",
                "SPRING_DATASOURCE_PASSWORD"
        );
        when(patcher.createPatch("src/main/resources/application.properties", "spring.datasource.password=secret123", 3, null))
                .thenReturn(patchResult);

        when(tokenService.computePatchPlanHash(finding.getId(), targetSha, "src/main/resources/application.properties", 3, patchResult.patchedLine()))
                .thenReturn(patchPlanHash);

        when(linkRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(gitHubPullRequestClient.createPullRequest(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new GitHubPullRequestClient.GitHubPrResult(77, "https://github.com/scanpilot-org/target-repo/pull/77"));

        FindingRemediationPrLinkDto link = remediationService.createRemediationPr(
                finding.getId(),
                new CreateFindingRemediationPrRequest(tokenString),
                session
        );

        assertThat(link).isNotNull();
        assertThat(link.state()).isEqualTo("CREATED");
        assertThat(link.githubPrNumber()).isEqualTo(77);
        assertThat(link.githubPrUrl()).isEqualTo("https://github.com/scanpilot-org/target-repo/pull/77");

        verify(gitHubPullRequestClient).createBranch(eq("scanpilot-org"), eq("target-repo"), anyString(), eq(targetSha), eq("ghs_mockToken"));
    }
}