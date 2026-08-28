package com.scanpilot.scanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingIssueLinkEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.FindingIssueLinkRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.dto.CreateFindingIssueRequest;
import com.scanpilot.scanner.issue.FindingIssueTokenService;
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
import java.util.Optional;
import java.util.UUID;

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
@DisplayName("Finding Issue Controller Integration & Authorization Tests")
class FindingIssueControllerTest {

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
    private FindingIssueLinkRepository linkRepository;

    @Autowired
    private FindingIssueTokenService tokenService;

    @MockitoBean
    private com.scanpilot.github.service.GitHubIssueClient gitHubIssueClient;

    @MockitoBean
    private com.scanpilot.github.service.GitHubAppAuthService gitHubAppAuthService;

    private UserSession userSession;
    private UserEntity userEntity;
    private RepositoryEntity repositoryEntity;
    private FindingEntity findingEntity;

    private UserSession otherUserSession;
    private UserEntity otherUserEntity;
    private RepositoryEntity otherRepositoryEntity;
    private FindingEntity otherFindingEntity;

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

        userSession = sessionService.createSession(
                112233L,
                "alice",
                "Alice Developer",
                "https://avatars.githubusercontent.com/u/112233",
                "alice@example.com",
                "gho_alice_token"
        );
        userSession.setInstallationId(5555L);
        sessionService.updateInstallationId(userSession.getSessionId(), 5555L);

        userEntity = userRepository.save(UserEntity.builder()
                .githubUserId(112233L)
                .login("alice")
                .name("Alice Developer")
                .email("alice@example.com")
                .createdAt(Instant.now())
                .build());

        repositoryEntity = repositoryRepository.save(RepositoryEntity.builder()
                .userId(userEntity.getId())
                .githubRepoId(1001L)
                .owner("alice")
                .name("secret-app")
                .fullName("alice/secret-app")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build());

        findingEntity = findingRepository.save(FindingEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .ruleId("SP-CONFIG-001")
                .fingerprint("fp-alice-1")
                .severity("HIGH")
                .title("Exposed Stripe API Key")
                .lifecycle("OPEN")
                .remediationQuality("ACTION_REQUIRED")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build());

        findingLocationRepository.save(FindingLocationEntity.builder()
                .findingId(findingEntity.getId())
                .filePath("src/main/resources/stripe.env")
                .startLine(12)
                .commitSha("abcdef123456")
                .build());

        // Other user setup (multi-tenant boundary)
        otherUserSession = sessionService.createSession(
                998877L,
                "bob",
                "Bob Developer",
                "https://avatars.githubusercontent.com/u/998877",
                "bob@example.com",
                "gho_bob_token"
        );
        otherUserSession.setInstallationId(7777L);
        sessionService.updateInstallationId(otherUserSession.getSessionId(), 7777L);

        otherUserEntity = userRepository.save(UserEntity.builder()
                .githubUserId(998877L)
                .login("bob")
                .name("Bob Developer")
                .email("bob@example.com")
                .createdAt(Instant.now())
                .build());

        otherRepositoryEntity = repositoryRepository.save(RepositoryEntity.builder()
                .userId(otherUserEntity.getId())
                .githubRepoId(2002L)
                .owner("bob")
                .name("bob-project")
                .fullName("bob/bob-project")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build());

        otherFindingEntity = findingRepository.save(FindingEntity.builder()
                .repositoryId(otherRepositoryEntity.getId())
                .ruleId("SP-CONFIG-001")
                .fingerprint("fp-bob-1")
                .severity("MEDIUM")
                .title("Bob Private Key")
                .lifecycle("OPEN")
                .remediationQuality("ACTION_REQUIRED")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("GET /issue-preview without session returns 401 Unauthorized")
    void testGetPreviewUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/findings/" + findingEntity.getId() + "/issue-preview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /issue-preview on another tenant's finding returns 403 Forbidden")
    void testGetPreviewCrossTenantReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/findings/" + otherFindingEntity.getId() + "/issue-preview")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /issue-preview with valid session returns 200 OK with previewToken and canonical markdown")
    void testGetPreviewSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/findings/" + findingEntity.getId() + "/issue-preview")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findingId", is(findingEntity.getId().toString())))
                .andExpect(jsonPath("$.title", is("[Security] SP-CONFIG-001: Potential secret exposure in src/main/resources/stripe.env")))
                .andExpect(jsonPath("$.body").isString())
                .andExpect(jsonPath("$.previewToken").isString())
                .andExpect(jsonPath("$.alreadyLinked", is(false)));
    }

    @Test
    @DisplayName("POST /issue with valid previewToken creates GitHub issue and returns 201 Created")
    void testCreateIssueSuccess() throws Exception {
        when(gitHubIssueClient.createIssue(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new com.scanpilot.github.service.GitHubIssueClient.GitHubIssueResult(42, "https://github.com/alice/secret-app/issues/42"));

        // First generate a valid preview token
        String bodyString = mockMvc.perform(get("/api/v1/findings/" + findingEntity.getId() + "/issue-preview")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String previewToken = objectMapper.readTree(bodyString).path("previewToken").asText();

        CreateFindingIssueRequest request = new CreateFindingIssueRequest(previewToken);

        mockMvc.perform(post("/api/v1/findings/" + findingEntity.getId() + "/issue")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.findingId", is(findingEntity.getId().toString())))
                .andExpect(jsonPath("$.state", is("CREATED")))
                .andExpect(jsonPath("$.githubIssueNumber", is(42)))
                .andExpect(jsonPath("$.githubIssueUrl", is("https://github.com/alice/secret-app/issues/42")));
    }

    @Test
    @DisplayName("POST /issue with tampered/invalid previewToken returns 409 Conflict")
    void testCreateIssueTamperedTokenReturns409() throws Exception {
        CreateFindingIssueRequest request = new CreateFindingIssueRequest("tampered.token.value");

        mockMvc.perform(post("/api/v1/findings/" + findingEntity.getId() + "/issue")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /issue when already PENDING returns 409 Conflict (CREATION_IN_PROGRESS)")
    void testCreateIssuePendingReturns409() throws Exception {
        // Pre-insert PENDING row
        linkRepository.save(FindingIssueLinkEntity.builder()
                .findingId(findingEntity.getId())
                .repositoryId(repositoryEntity.getId())
                .state("PENDING")
                .idempotencyMarker("scanpilot-finding-" + findingEntity.getId())
                .createdByUserId(userEntity.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        String bodyString = mockMvc.perform(get("/api/v1/findings/" + findingEntity.getId() + "/issue-preview")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String previewToken = objectMapper.readTree(bodyString).path("previewToken").asText();

        CreateFindingIssueRequest request = new CreateFindingIssueRequest(previewToken);

        mockMvc.perform(post("/api/v1/findings/" + findingEntity.getId() + "/issue")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /issue for existing link returns 200 OK")
    void testGetIssueLinkSuccess() throws Exception {
        linkRepository.save(FindingIssueLinkEntity.builder()
                .findingId(findingEntity.getId())
                .repositoryId(repositoryEntity.getId())
                .state("CREATED")
                .githubIssueNumber(88)
                .githubIssueUrl("https://github.com/alice/secret-app/issues/88")
                .idempotencyMarker("marker-88")
                .createdByUserId(userEntity.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/findings/" + findingEntity.getId() + "/issue")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubIssueNumber", is(88)))
                .andExpect(jsonPath("$.state", is("CREATED")));
    }
}
