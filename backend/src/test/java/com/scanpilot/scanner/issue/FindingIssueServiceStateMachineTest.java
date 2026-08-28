package com.scanpilot.scanner.issue;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.service.GitHubAppAuthService;
import com.scanpilot.github.service.GitHubAppService;
import com.scanpilot.github.service.GitHubIssueClient;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingIssueLinkEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingIssueLinkRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.dto.CreateFindingIssueRequest;
import com.scanpilot.scanner.dto.FindingIssueLinkDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Finding Issue Service State Machine & Ambiguity Tests")
class FindingIssueServiceStateMachineTest {

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private FindingLocationRepository findingLocationRepository;

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FindingIssueLinkRepository linkRepository;

    @Mock
    private FindingIssueTemplateService templateService;

    @Mock
    private FindingIssueTokenService tokenService;

    @Mock
    private GitHubIssueClient gitHubIssueClient;

    @Mock
    private GitHubAppAuthService gitHubAppAuthService;

    @Mock
    private GitHubAppService gitHubAppService;

    private FindingIssueService findingIssueService;

    private UserSession session;
    private UserEntity user;
    private RepositoryEntity repo;
    private FindingEntity finding;
    private UUID findingId;
    private UUID repoId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        findingIssueService = new FindingIssueService(
                findingRepository,
                findingLocationRepository,
                evidenceItemRepository,
                repositoryRepository,
                userRepository,
                linkRepository,
                templateService,
                tokenService,
                gitHubIssueClient,
                gitHubAppAuthService,
                gitHubAppService
        );

        userId = UUID.randomUUID();
        repoId = UUID.randomUUID();
        findingId = UUID.randomUUID();

        session = UserSession.builder()
                .sessionId("sess-123")
                .githubUserId(12345L)
                .installationId(9999L)
                .build();

        user = UserEntity.builder()
                .id(userId)
                .githubUserId(12345L)
                .build();

        repo = RepositoryEntity.builder()
                .id(repoId)
                .userId(userId)
                .owner("octocat")
                .name("hello-world")
                .build();

        finding = FindingEntity.builder()
                .id(findingId)
                .repositoryId(repoId)
                .ruleId("SP-CONFIG-001")
                .lastSeenAt(Instant.now())
                .build();
    }

    private void mockAuthAndFindingLookups() {
        when(findingRepository.findById(findingId)).thenReturn(Optional.of(finding));
        when(repositoryRepository.findById(repoId)).thenReturn(Optional.of(repo));
        when(userRepository.findByGithubUserId(12345L)).thenReturn(Optional.of(user));
        when(gitHubAppAuthService.isConfigured()).thenReturn(true);
        when(findingLocationRepository.findByFindingId(findingId)).thenReturn(List.of());
        when(evidenceItemRepository.findByFindingId(findingId)).thenReturn(List.of());
        when(templateService.buildTitle(eq(finding), any())).thenReturn("[Security] SP-CONFIG-001: Secret Exposure");
        when(templateService.buildBody(eq(finding), any(), any())).thenReturn("Canonical Body");
        when(tokenService.computeDraftSha256(anyString())).thenReturn("draft-hash");
        doNothing().when(tokenService).validateToken(anyString(), eq(findingId), anyLong(), eq("draft-hash"));
    }

    @Test
    @DisplayName("GIVEN no existing link WHEN issue is created successfully THEN transitions PENDING -> CREATED")
    void testHappyPathIssueCreation() {
        mockAuthAndFindingLookups();
        when(linkRepository.findByFindingId(findingId))
                .thenReturn(Optional.empty()) // initial check
                .thenReturn(Optional.of(FindingIssueLinkEntity.builder()
                        .id(UUID.randomUUID())
                        .findingId(findingId)
                        .repositoryId(repoId)
                        .state("PENDING")
                        .build())); // check after GitHub call

        when(gitHubAppAuthService.createInstallationAccessToken(9999L)).thenReturn("gh-token");
        when(gitHubIssueClient.createIssue("octocat", "hello-world", "gh-token", "[Security] SP-CONFIG-001: Secret Exposure", "Canonical Body"))
                .thenReturn(new GitHubIssueClient.GitHubIssueResult(42, "https://github.com/octocat/hello-world/issues/42"));

        when(linkRepository.saveAndFlush(any(FindingIssueLinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FindingIssueLinkDto result = findingIssueService.createIssue(findingId, new CreateFindingIssueRequest("valid.token"), session);

        assertThat(result).isNotNull();
        assertThat(result.state()).isEqualTo("CREATED");
        assertThat(result.githubIssueNumber()).isEqualTo(42);
        assertThat(result.githubIssueUrl()).isEqualTo("https://github.com/octocat/hello-world/issues/42");
        verify(gitHubIssueClient).createIssue("octocat", "hello-world", "gh-token", "[Security] SP-CONFIG-001: Secret Exposure", "Canonical Body");
    }

    @Test
    @DisplayName("GIVEN link state is CREATED WHEN calling createIssue THEN returns existing link without calling GitHub")
    void testAlreadyCreatedReturnsExisting() {
        mockAuthAndFindingLookups();
        FindingIssueLinkEntity existing = FindingIssueLinkEntity.builder()
                .id(UUID.randomUUID())
                .findingId(findingId)
                .repositoryId(repoId)
                .state("CREATED")
                .githubIssueNumber(10)
                .githubIssueUrl("https://github.com/octocat/hello-world/issues/10")
                .createdAt(Instant.now())
                .build();
        when(linkRepository.findByFindingId(findingId)).thenReturn(Optional.of(existing));

        FindingIssueLinkDto result = findingIssueService.createIssue(findingId, new CreateFindingIssueRequest("valid.token"), session);

        assertThat(result.state()).isEqualTo("CREATED");
        assertThat(result.githubIssueNumber()).isEqualTo(10);
        verify(gitHubIssueClient, never()).createIssue(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GIVEN fresh PENDING (<60s) WHEN calling createIssue THEN throws 409 Conflict (CREATION_IN_PROGRESS)")
    void testFreshPendingThrowsConflict() {
        mockAuthAndFindingLookups();
        FindingIssueLinkEntity pendingLink = FindingIssueLinkEntity.builder()
                .id(UUID.randomUUID())
                .findingId(findingId)
                .repositoryId(repoId)
                .state("PENDING")
                .createdAt(Instant.now().minusSeconds(10))
                .updatedAt(Instant.now().minusSeconds(10))
                .build();
        when(linkRepository.findByFindingId(findingId)).thenReturn(Optional.of(pendingLink));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                findingIssueService.createIssue(findingId, new CreateFindingIssueRequest("valid.token"), session)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("CREATION_IN_PROGRESS");
        verify(gitHubIssueClient, never()).createIssue(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GIVEN stale PENDING (>=60s) and remote issue exists WHEN calling createIssue THEN transitions PENDING -> UNKNOWN -> CREATED")
    void testStalePendingTransitionsToUnknownAndReconcilesExisting() {
        mockAuthAndFindingLookups();
        FindingIssueLinkEntity stalePendingLink = FindingIssueLinkEntity.builder()
                .id(UUID.randomUUID())
                .findingId(findingId)
                .repositoryId(repoId)
                .state("PENDING")
                .createdAt(Instant.now().minusSeconds(120))
                .updatedAt(Instant.now().minusSeconds(120))
                .build();

        when(linkRepository.findByFindingId(findingId)).thenReturn(Optional.of(stalePendingLink));
        when(linkRepository.updateStateConditional(eq(findingId), eq("PENDING"), eq("UNKNOWN"), any(Instant.class)))
                .thenReturn(1);

        when(gitHubAppAuthService.createInstallationAccessToken(9999L)).thenReturn("gh-token");
        when(gitHubIssueClient.findIssueByMarker(eq("octocat"), eq("hello-world"), eq("gh-token"), anyString()))
                .thenReturn(Optional.of(new GitHubIssueClient.GitHubIssueResult(88, "https://github.com/octocat/hello-world/issues/88")));

        when(linkRepository.saveAndFlush(any(FindingIssueLinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FindingIssueLinkDto result = findingIssueService.createIssue(findingId, new CreateFindingIssueRequest("valid.token"), session);

        assertThat(result.state()).isEqualTo("CREATED");
        assertThat(result.githubIssueNumber()).isEqualTo(88);
        verify(gitHubIssueClient, never()).createIssue(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GIVEN stale PENDING (>=60s) and issue absent WHEN caller wins atomic transitions THEN calls GitHub and creates issue")
    void testStalePendingTransitionsToUnknownAndCreatesWhenAbsent() {
        mockAuthAndFindingLookups();
        FindingIssueLinkEntity stalePendingLink = FindingIssueLinkEntity.builder()
                .id(UUID.randomUUID())
                .findingId(findingId)
                .repositoryId(repoId)
                .state("PENDING")
                .createdAt(Instant.now().minusSeconds(120))
                .updatedAt(Instant.now().minusSeconds(120))
                .build();

        when(linkRepository.findByFindingId(findingId))
                .thenReturn(Optional.of(stalePendingLink))
                .thenReturn(Optional.of(stalePendingLink));

        when(linkRepository.updateStateConditional(eq(findingId), eq("PENDING"), eq("UNKNOWN"), any(Instant.class)))
                .thenReturn(1);

        when(gitHubAppAuthService.createInstallationAccessToken(9999L)).thenReturn("gh-token");
        when(gitHubIssueClient.findIssueByMarker(eq("octocat"), eq("hello-world"), eq("gh-token"), anyString()))
                .thenReturn(Optional.empty());

        when(linkRepository.updateStateConditional(eq(findingId), eq("UNKNOWN"), eq("PENDING"), any(Instant.class)))
                .thenReturn(1);

        when(gitHubIssueClient.createIssue("octocat", "hello-world", "gh-token", "[Security] SP-CONFIG-001: Secret Exposure", "Canonical Body"))
                .thenReturn(new GitHubIssueClient.GitHubIssueResult(101, "https://github.com/octocat/hello-world/issues/101"));

        when(linkRepository.saveAndFlush(any(FindingIssueLinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FindingIssueLinkDto result = findingIssueService.createIssue(findingId, new CreateFindingIssueRequest("valid.token"), session);

        assertThat(result.state()).isEqualTo("CREATED");
        assertThat(result.githubIssueNumber()).isEqualTo(101);
        verify(gitHubIssueClient).createIssue("octocat", "hello-world", "gh-token", "[Security] SP-CONFIG-001: Secret Exposure", "Canonical Body");
    }

    @Test
    @DisplayName("GIVEN reconciliation query failure WHEN retrying from UNKNOWN THEN throws 504 and does NOT call createIssue")
    void testReconciliationQueryFailureThrowsAmbiguous() {
        mockAuthAndFindingLookups();
        FindingIssueLinkEntity unknownLink = FindingIssueLinkEntity.builder()
                .id(UUID.randomUUID())
                .findingId(findingId)
                .repositoryId(repoId)
                .state("UNKNOWN")
                .build();

        when(linkRepository.findByFindingId(findingId)).thenReturn(Optional.of(unknownLink));
        when(gitHubAppAuthService.createInstallationAccessToken(9999L)).thenReturn("gh-token");
        when(gitHubIssueClient.findIssueByMarker(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new GitHubIssueClient.GitHubAmbiguousException("RECONCILIATION_QUERY_FAILED"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                findingIssueService.createIssue(findingId, new CreateFindingIssueRequest("valid.token"), session)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(ex.getReason()).isEqualTo("GITHUB_ISSUE_CREATION_AMBIGUOUS");
        verify(gitHubIssueClient, never()).createIssue(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GIVEN atomic UNKNOWN -> PENDING update lost (concurrent retry) WHEN calling createIssue THEN does NOT call GitHub")
    void testRetryFromUnknownAtomicLoserDoesNotCallGitHub() {
        mockAuthAndFindingLookups();
        FindingIssueLinkEntity unknownLink = FindingIssueLinkEntity.builder()
                .id(UUID.randomUUID())
                .findingId(findingId)
                .repositoryId(repoId)
                .state("UNKNOWN")
                .build();

        FindingIssueLinkEntity winnerPendingLink = FindingIssueLinkEntity.builder()
                .id(UUID.randomUUID())
                .findingId(findingId)
                .repositoryId(repoId)
                .state("PENDING")
                .build();

        when(linkRepository.findByFindingId(findingId))
                .thenReturn(Optional.of(unknownLink))
                .thenReturn(Optional.of(winnerPendingLink));

        when(gitHubAppAuthService.createInstallationAccessToken(9999L)).thenReturn("gh-token");
        when(gitHubIssueClient.findIssueByMarker(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(linkRepository.updateStateConditional(eq(findingId), eq("UNKNOWN"), eq("PENDING"), any(Instant.class)))
                .thenReturn(0); // Lost concurrency race

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                findingIssueService.createIssue(findingId, new CreateFindingIssueRequest("valid.token"), session)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("CREATION_IN_PROGRESS");
        verify(gitHubIssueClient, never()).createIssue(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
