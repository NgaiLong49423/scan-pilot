package com.scanpilot.scanner.controller;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.persistence.entity.CoverageItemEntity;
import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.project.dto.SelectRepositoryRequest;
import com.scanpilot.project.service.ProjectService;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Scan Controller Integration Tests")
class ScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private FindingLocationRepository findingLocationRepository;

    @Autowired
    private CoverageRecordRepository coverageRecordRepository;

    @Autowired
    private CoverageItemRepository coverageItemRepository;

    private UserSession userSession;
    private UserEntity userEntity;
    private RepositoryEntity repositoryEntity;

    @BeforeEach
    void setUp() {
        sessionService.clearAllSessions();
        projectService.clearAllProjects();

        userSession = sessionService.createSession(
            445566L,
            "scanpilot-tester",
            "Scan Pilot Tester",
            "https://avatars.githubusercontent.com/u/445566",
            "tester@scanpilot.com",
            "gho_test_oauth_token"
        );

        userEntity = userRepository.findByGithubUserId(445566L)
            .orElseGet(() -> userRepository.save(UserEntity.builder()
                .githubUserId(445566L)
                .login("scanpilot-tester")
                .name("Scan Pilot Tester")
                .email("tester@scanpilot.com")
                .createdAt(Instant.now())
                .build()));

        repositoryEntity = repositoryRepository.findByUserIdAndGithubRepoId(userEntity.getId(), 889900L)
            .orElseGet(() -> repositoryRepository.save(RepositoryEntity.builder()
                .userId(userEntity.getId())
                .githubRepoId(889900L)
                .owner("scanpilot-tester")
                .name("target-repo")
                .fullName("scanpilot-tester/target-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build()));

        // Also select in ProjectService
        projectService.selectRepository(userSession, new SelectRepositoryRequest(
            889900L,
            "scanpilot-tester/target-repo",
            "target-repo",
            "scanpilot-tester",
            "main",
            false
        ));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger triggers scan when authenticated")
    void testTriggerScanAuthenticated() throws Exception {
        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main"
            }
            """, repositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").isNotEmpty())
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("main"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger without explicit repositoryId uses active monitored project")
    void testTriggerScanImplicitActiveProject() throws Exception {
        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger without auth returns 401 Unauthorized")
    void testTriggerScanUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/scans/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/scans/jobs/{jobId} returns job details when found")
    void testGetScanJobFound() throws Exception {
        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("COMPLETED")
            .commitSha("abcdef1234567890abcdef1234567890abcdef12")
            .durationMs(550L)
            .startedAt(Instant.now().minusSeconds(10))
            .completedAt(Instant.now())
            .build());

        mockMvc.perform(get("/api/v1/scans/jobs/" + job.getId())
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(job.getId().toString()))
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.durationMs").value(550));
    }

    @Test
    @DisplayName("GET /api/v1/scans/jobs/{jobId} returns 404 when not found")
    void testGetScanJobNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/scans/jobs/" + UUID.randomUUID())
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/scans/repositories/{repositoryId}/findings returns findings with locations")
    void testGetFindings() throws Exception {
        FindingEntity finding = findingRepository.save(FindingEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .ruleId("google-api-key")
            .fingerprint("fp_sample_controller_001")
            .severity("CRITICAL")
            .title("Exposed Google API Key")
            .description("Found active API key in client file")
            .lifecycle("OPEN")
            .remediationQuality("ACTION_REQUIRED")
            .firstSeenAt(Instant.now())
            .lastSeenAt(Instant.now())
            .build());

        findingLocationRepository.save(FindingLocationEntity.builder()
            .findingId(finding.getId())
            .filePath("src/main/resources/application.yml")
            .startLine(10)
            .endLine(10)
            .startColumn(15)
            .endColumn(54)
            .commitSha("c0ffee123456")
            .isCurrentHead(true)
            .detectedAt(Instant.now())
            .build());

        mockMvc.perform(get("/api/v1/scans/repositories/" + repositoryEntity.getId() + "/findings")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].ruleId").value("google-api-key"))
            .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
            .andExpect(jsonPath("$[0].lifecycle").value("OPEN"))
            .andExpect(jsonPath("$[0].remediationQuality").value("ACTION_REQUIRED"))
            .andExpect(jsonPath("$[0].locations", hasSize(1)))
            .andExpect(jsonPath("$[0].locations[0].filePath").value("src/main/resources/application.yml"));
    }

    @Test
    @DisplayName("GET /api/v1/scans/repositories/{repositoryId}/coverage returns latest coverage record and breakdown")
    void testGetCoverageSummary() throws Exception {
        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("COMPLETED")
            .build());

        CoverageRecordEntity record = coverageRecordRepository.save(CoverageRecordEntity.builder()
            .scanJobId(job.getId())
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .totalFiles(25)
            .scannedFiles(23)
            .skippedFiles(2)
            .textFiles(23)
            .binaryFiles(2)
            .undeterminedFiles(0)
            .totalBytes(1048576L)
            .coverageImpact("COMPLETE")
            .createdAt(Instant.now())
            .build());

        coverageItemRepository.save(CoverageItemEntity.builder()
            .coverageRecordId(record.getId())
            .filePath("images/logo.png")
            .classification("BINARY")
            .sizeBytes(204800L)
            .status("SKIPPED")
            .reasonCode("UNSUPPORTED_BINARY_FILE")
            .impact("COMPLETE")
            .details("Binary image file skipped")
            .build());

        mockMvc.perform(get("/api/v1/scans/repositories/" + repositoryEntity.getId() + "/coverage")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFiles").value(25))
            .andExpect(jsonPath("$.scannedFiles").value(23))
            .andExpect(jsonPath("$.skippedFiles").value(2))
            .andExpect(jsonPath("$.coverageImpact").value("COMPLETE"))
            .andExpect(jsonPath("$.skippedItems", hasSize(1)))
            .andExpect(jsonPath("$.skippedItems[0].filePath").value("images/logo.png"));
    }
}
