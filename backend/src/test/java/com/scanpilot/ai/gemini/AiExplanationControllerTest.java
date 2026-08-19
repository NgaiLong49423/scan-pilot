package com.scanpilot.ai.gemini;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AiExplanationController Integration Tests")
class AiExplanationControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    private EvidenceItemRepository evidenceItemRepository;

    private UserSession userSession;
    private UserEntity userEntity;
    private RepositoryEntity repositoryEntity;
    private FindingEntity findingEntity;

    @BeforeEach
    void setUp() {
        sessionService.clearAllSessions();
        evidenceItemRepository.deleteAll();
        findingLocationRepository.deleteAll();
        findingRepository.deleteAll();

        userSession = sessionService.createSession(
            112233L,
            "scanpilot-ai-tester",
            "Scan Pilot AI Tester",
            "https://avatars.githubusercontent.com/u/112233",
            "tester@scanpilot.com",
            "gho_dummy_token"
        );

        userEntity = userRepository.findByGithubUserId(112233L)
            .orElseGet(() -> userRepository.save(UserEntity.builder()
                .githubUserId(112233L)
                .login("scanpilot-ai-tester")
                .name("Scan Pilot AI Tester")
                .email("tester@scanpilot.com")
                .createdAt(Instant.now())
                .build()));

        repositoryEntity = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), 556677L)
            .orElseGet(() -> repositoryRepository.save(RepositoryEntity.builder()
                .userId(userEntity.getId())
                .githubRepoId(556677L)
                .owner("scanpilot-ai-tester")
                .name("security-demo")
                .fullName("scanpilot-ai-tester/security-demo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(true)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build()));

        findingEntity = findingRepository.save(FindingEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .ruleId("google-api-key")
            .fingerprint("fp-controller-test-001")
            .severity("HIGH")
            .title("Exposed Google API Key")
            .description("Detected Google API Key in client code")
            .lifecycle("OPEN")
            .remediationQuality("UNRESOLVED")
            .firstSeenAt(Instant.now())
            .lastSeenAt(Instant.now())
            .build());

        findingLocationRepository.save(FindingLocationEntity.builder()
            .findingId(findingEntity.getId())
            .filePath("src/main/resources/application.yml")
            .startLine(12)
            .endLine(12)
            .startColumn(10)
            .endColumn(49)
            .commitSha("commit123")
            .author("dev@scanpilot.com")
            .isCurrentHead(true)
            .detectedAt(Instant.now())
            .build());

        evidenceItemRepository.save(EvidenceItemEntity.builder()
            .findingId(findingEntity.getId())
            .evidenceType("TECHNICAL")
            .maskedSecret("AIzaSy****************************9999")
            .redactedSnippet("gemini.key: [REDACTED_SECRET]")
            .verificationStatus("OBSERVED")
            .sourceAttribution("GitleaksDetectorAdapter:SP-CONFIG-001")
            .createdAt(Instant.now())
            .build());
    }

    @Test
    @DisplayName("Unauthenticated request to POST /explain returns 401 Unauthorized")
    void testExplainUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/ai/findings/" + findingEntity.getId() + "/explain"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to GET /explanation returns 401 Unauthorized")
    void testGetExplanationUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/ai/findings/" + findingEntity.getId() + "/explanation"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /explain returns structured explanation guidance and 200 OK")
    void testExplainFindingSuccess() throws Exception {
        Cookie sessionCookie = new Cookie("SCANPILOT_SESSION", userSession.getSessionId());

        mockMvc.perform(post("/api/v1/ai/findings/" + findingEntity.getId() + "/explain")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary", notNullValue()))
            .andExpect(jsonPath("$.riskImpact", notNullValue()))
            .andExpect(jsonPath("$.evidenceLimits", notNullValue()))
            .andExpect(jsonPath("$.remediationSteps", notNullValue()))
            .andExpect(jsonPath("$.remediationDiff", notNullValue()))
            .andExpect(jsonPath("$.revocationCommandHint", notNullValue()))
            .andExpect(jsonPath("$.sourceAttribution", notNullValue()));
    }

    @Test
    @DisplayName("GET /explanation returns 404 when no explanation has been generated yet")
    void testGetExplanationNotFound() throws Exception {
        Cookie sessionCookie = new Cookie("SCANPILOT_SESSION", userSession.getSessionId());

        mockMvc.perform(get("/api/v1/ai/findings/" + findingEntity.getId() + "/explanation")
                .cookie(sessionCookie))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /explanation returns 200 OK with explanation after POST /explain")
    void testGetExistingExplanationSuccess() throws Exception {
        Cookie sessionCookie = new Cookie("SCANPILOT_SESSION", userSession.getSessionId());

        // First trigger explain
        mockMvc.perform(post("/api/v1/ai/findings/" + findingEntity.getId() + "/explain")
                .cookie(sessionCookie))
            .andExpect(status().isOk());

        // Then retrieve explanation
        mockMvc.perform(get("/api/v1/ai/findings/" + findingEntity.getId() + "/explanation")
                .cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary", containsString("Google / Gemini API key")))
            .andExpect(jsonPath("$.remediationSteps", hasSize(4)));
    }

    @Test
    @DisplayName("POST /explain returns 404 NOT FOUND when finding does not exist")
    void testExplainNonExistentFinding() throws Exception {
        Cookie sessionCookie = new Cookie("SCANPILOT_SESSION", userSession.getSessionId());
        UUID nonExistentFindingId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/ai/findings/" + nonExistentFindingId + "/explain")
                .cookie(sessionCookie))
            .andExpect(status().isNotFound());
    }
}
