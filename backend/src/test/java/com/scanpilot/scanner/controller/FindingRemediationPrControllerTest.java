package com.scanpilot.scanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
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
import com.scanpilot.scanner.remediation.FindingRemediationPrTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Finding Remediation PR Controller Integration & Authorization Tests")
class FindingRemediationPrControllerTest {

    private static final String TARGET_SHA = "1111111111111111111111111111111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private FindingLocationRepository findingLocationRepository;

    @Autowired
    private FindingRemediationPrLinkRepository linkRepository;

    @Autowired
    private FindingRemediationPrTokenService tokenService;

    @MockitoBean
    private com.scanpilot.github.service.GitHubPullRequestClient gitHubPullRequestClient;

    @MockitoBean
    private com.scanpilot.github.service.GitHubAppAuthService gitHubAppAuthService;

    private UserSession userSession;
    private UserEntity userEntity;
    private RepositoryEntity repositoryEntity;
    private FindingEntity findingEntity;
    private FindingLocationEntity locationEntity;

    private UserSession bobSession;
    private UserEntity bobEntity;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAll();
        findingLocationRepository.deleteAll();
        findingRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();
        sessionService.clearAllSessions();

        when(gitHubAppAuthService.isConfigured()).thenReturn(true);
        when(gitHubAppAuthService.createInstallationAccessToken(any())).thenReturn("mock-installation-token");

        userEntity = userRepository.save(UserEntity.builder()
                .githubUserId(12345L)
                .login("alice")
                .name("Alice Developer")
                .email("alice@example.com")
                .avatarUrl("https://github.com/alice.png")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        userSession = sessionService.createSession(
                userEntity.getGithubUserId(),
                userEntity.getLogin(),
                userEntity.getName(),
                userEntity.getAvatarUrl(),
                userEntity.getEmail(),
                "gho_sample_token"
        );

        bobEntity = userRepository.save(UserEntity.builder()
                .githubUserId(99999L)
                .login("bob")
                .name("Bob Developer")
                .email("bob@example.com")
                .avatarUrl("https://github.com/bob.png")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        bobSession = sessionService.createSession(
                bobEntity.getGithubUserId(),
                bobEntity.getLogin(),
                bobEntity.getName(),
                bobEntity.getAvatarUrl(),
                bobEntity.getEmail(),
                "gho_bob_token"
        );

        repositoryEntity = repositoryRepository.save(RepositoryEntity.builder()
                .userId(userEntity.getId())
                .githubRepoId(1001L)
                .installationId(2001L)
                .owner("alice-org")
                .name("payments-service")
                .fullName("alice-org/payments-service")
                .defaultBranch("main")
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        findingEntity = findingRepository.save(FindingEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .ruleId("SP-CONFIG-001")
                .fingerprint("fp-db-password-1")
                .severity("CRITICAL")
                .title("Exposed DB Password")
                .lifecycle("OPEN")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build());

        locationEntity = findingLocationRepository.save(FindingLocationEntity.builder()
                .findingId(findingEntity.getId())
                .filePath("src/main/resources/application.properties")
                .startLine(2)
                .commitSha(TARGET_SHA)
                .isCurrentHead(true)
                .detectedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("GET preview returns 200 OK with masked diff and signed token")
    void testGetPreviewSuccess() throws Exception {
        when(gitHubPullRequestClient.getDefaultBranchHead(anyString(), anyString(), anyString()))
                .thenReturn(new com.scanpilot.github.service.GitHubPullRequestClient.DefaultBranchHead("main", TARGET_SHA));
        when(gitHubPullRequestClient.getFileContent(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("server.port=8080\nspring.datasource.password=superSecret123\n");

        mockMvc.perform(get("/api/v1/findings/{findingId}/remediation-pr-preview", findingEntity.getId())
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findingId", is(findingEntity.getId().toString())))
                .andExpect(jsonPath("$.envVariableName", is("SPRING_DATASOURCE_PASSWORD")))
                .andExpect(jsonPath("$.originalLineMasked", is("spring.datasource.password=***")))
                .andExpect(jsonPath("$.patchedLine", is("spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}")))
                .andExpect(jsonPath("$.previewToken").isNotEmpty())
                .andExpect(jsonPath("$.revocationWarning", containsString("MANDATORY REVOCATION NOTICE")));
    }

    @Test
    @DisplayName("POST remediation-pr creates PR successfully with valid token")
    void testCreateRemediationPrSuccess() throws Exception {
        when(gitHubPullRequestClient.getDefaultBranchHead(anyString(), anyString(), anyString()))
                .thenReturn(new com.scanpilot.github.service.GitHubPullRequestClient.DefaultBranchHead("main", TARGET_SHA));
        when(gitHubPullRequestClient.getFileContent(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("server.port=8080\nspring.datasource.password=superSecret123\n");

        String patchPlanHash = tokenService.computePatchPlanHash(
                findingEntity.getId(),
                TARGET_SHA,
                "src/main/resources/application.properties",
                2,
                "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}"
        );
        String token = tokenService.generateToken(findingEntity.getId(), repositoryEntity.getId(), TARGET_SHA, patchPlanHash);

        when(gitHubPullRequestClient.createPullRequest(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new com.scanpilot.github.service.GitHubPullRequestClient.GitHubPrResult(99, "https://github.com/alice-org/payments-service/pull/99"));

        CreateFindingRemediationPrRequest request = new CreateFindingRemediationPrRequest(token);

        mockMvc.perform(post("/api/v1/findings/{findingId}/remediation-pr", findingEntity.getId())
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state", is("CREATED")))
                .andExpect(jsonPath("$.githubPrNumber", is(99)))
                .andExpect(jsonPath("$.githubPrUrl", is("https://github.com/alice-org/payments-service/pull/99")));
    }

    @Test
    @DisplayName("POST remediation-pr rejects extra properties with 400 Bad Request")
    void testCreateRemediationPrRejectsExtraProperties() throws Exception {
        String jsonWithExtra = "{\"previewToken\":\"valid.token\",\"extraProp\":\"malicious_injection\"}";

        mockMvc.perform(post("/api/v1/findings/{findingId}/remediation-pr", findingEntity.getId())
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithExtra))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET remediation-pr returns persisted link for owner")
    void testGetRemediationPrLink() throws Exception {
        FindingRemediationPrLinkEntity link = linkRepository.save(FindingRemediationPrLinkEntity.builder()
                .findingId(findingEntity.getId())
                .repositoryId(repositoryEntity.getId())
                .sourceRevisionCommit(TARGET_SHA)
                .targetBranch("main")
                .headBranch("scanpilot/remediation-test")
                .state("CREATED")
                .githubPrNumber(99)
                .githubPrUrl("https://github.com/alice-org/payments-service/pull/99")
                .idempotencyMarker("m1")
                .createdByUserId(userEntity.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/findings/{findingId}/remediation-pr", findingEntity.getId())
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("CREATED")))
                .andExpect(jsonPath("$.githubPrNumber", is(99)))
                .andExpect(jsonPath("$.githubPrUrl", is("https://github.com/alice-org/payments-service/pull/99")));
    }

    @Test
    @DisplayName("P1-2: GET remediation-pr rejects non-owner with 403 Forbidden")
    void testGetRemediationPrLinkRejectsNonOwner() throws Exception {
        linkRepository.save(FindingRemediationPrLinkEntity.builder()
                .findingId(findingEntity.getId())
                .repositoryId(repositoryEntity.getId())
                .sourceRevisionCommit(TARGET_SHA)
                .targetBranch("main")
                .headBranch("scanpilot/remediation-test")
                .state("CREATED")
                .githubPrNumber(99)
                .githubPrUrl("https://github.com/alice-org/payments-service/pull/99")
                .idempotencyMarker("m1")
                .createdByUserId(userEntity.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/findings/{findingId}/remediation-pr", findingEntity.getId())
                        .cookie(new Cookie("SCANPILOT_SESSION", bobSession.getSessionId())))
                .andExpect(status().isForbidden());
    }
}