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
import com.scanpilot.persistence.entity.MonitoredBranchEntity;
import com.scanpilot.persistence.repository.MonitoredBranchRepository;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.project.dto.SelectRepositoryRequest;
import com.scanpilot.project.service.ProjectService;
import com.scanpilot.scanner.pipeline.ScanPipelineService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @MockitoBean
    private ScanPipelineService scanPipelineService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private MonitoredBranchRepository monitoredBranchRepository;

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

    @Autowired
    private com.scanpilot.persistence.repository.ScanEventRepository scanEventRepository;

    @Autowired
    private com.scanpilot.persistence.repository.FindingIssueLinkRepository findingIssueLinkRepository;

    private UserSession userSession;
    private UserEntity userEntity;
    private RepositoryEntity repositoryEntity;

    private UserSession otherUserSession;
    private UserEntity otherUserEntity;
    private RepositoryEntity otherRepositoryEntity;

    @BeforeEach
    void setUp() {
        if (scanEventRepository != null) {
            scanEventRepository.deleteAll();
        }
        if (findingIssueLinkRepository != null) {
            findingIssueLinkRepository.deleteAll();
        }
        findingLocationRepository.deleteAll();
        findingRepository.deleteAll();
        coverageItemRepository.deleteAll();
        coverageRecordRepository.deleteAll();
        scanJobRepository.deleteAll();
        monitoredBranchRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();
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

        userEntity = userRepository.save(UserEntity.builder()
            .githubUserId(445566L)
            .login("scanpilot-tester")
            .name("Scan Pilot Tester")
            .email("tester@scanpilot.com")
            .createdAt(Instant.now())
            .build());

        repositoryEntity = repositoryRepository.save(RepositoryEntity.builder()
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
            .build());

        // Create other user for authorization boundary testing
        otherUserSession = sessionService.createSession(
            778899L,
            "other-user",
            "Other User",
            "https://avatars.githubusercontent.com/u/778899",
            "other@scanpilot.com",
            "gho_other_oauth_token"
        );

        otherUserEntity = userRepository.findByGithubUserId(778899L)
            .orElseGet(() -> userRepository.save(UserEntity.builder()
                .githubUserId(778899L)
                .login("other-user")
                .name("Other User")
                .email("other@scanpilot.com")
                .createdAt(Instant.now())
                .build()));

        otherRepositoryEntity = repositoryRepository.findByUserIdAndGithubRepoId(otherUserEntity.getId(), 991122L)
            .orElseGet(() -> repositoryRepository.save(RepositoryEntity.builder()
                .userId(otherUserEntity.getId())
                .githubRepoId(991122L)
                .owner("other-user")
                .name("secret-repo")
                .fullName("other-user/secret-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(true)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build()));

        monitoredBranchRepository.save(MonitoredBranchEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .branchType("PRIMARY")
            .isActive(true)
            .createdAt(Instant.now())
            .build());

        monitoredBranchRepository.save(MonitoredBranchEntity.builder()
            .repositoryId(otherRepositoryEntity.getId())
            .branchName("main")
            .branchType("PRIMARY")
            .isActive(true)
            .createdAt(Instant.now())
            .build());
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger triggers async scan returning HTTP 202 Accepted with QUEUED job")
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
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").isNotEmpty())
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("main"))
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.stage").value("QUEUED"))
            .andExpect(jsonPath("$.message").value("Scan job queued successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger with duplicate active scan returns existing active job")
    void testTriggerScanDuplicateReturnsExistingActiveJob() throws Exception {
        ScanJobEntity activeJob = scanJobRepository.save(ScanJobEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("RUNNING")
            .stage("SCANNING_SECRETS")
            .createdAt(Instant.now().minusSeconds(10))
            .startedAt(Instant.now().minusSeconds(5))
            .build());

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
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").value(activeJob.getId().toString()))
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("main"))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.stage").value("SCANNING_SECRETS"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger missing repositoryId returns 400 Bad Request (fail-closed, zero fallback)")
    void testTriggerScanMissingRepositoryIdFailClosed() throws Exception {
        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Invalid, missing, or unauthorized repository ID"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger sending numeric githubRepoId returns 400 Bad Request (fail-closed)")
    void testTriggerScanNumericGithubRepoIdFailClosed() throws Exception {
        String requestJson = """
            {
                "repositoryId": 889900,
                "branchName": "main"
            }
            """;

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid, missing, or unauthorized repository ID"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger with non-existent repository UUID returns 404 Not Found (fail-closed)")
    void testTriggerScanNonExistentRepositoryFailClosed() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main"
            }
            """, nonExistentId);

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Invalid, missing, or unauthorized repository ID"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger with repository owned by another user returns 404 Not Found (fail-closed)")
    void testTriggerScanUnauthorizedRepositoryFailClosed() throws Exception {
        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main"
            }
            """, otherRepositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Invalid, missing, or unauthorized repository ID"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger with custom sourcePath returns 400 Bad Request (fail-closed)")
    void testTriggerScanCustomSourcePathRejectedFailClosed() throws Exception {
        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main",
                "sourcePath": "/tmp/custom-path"
            }
            """, repositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Custom sourcePath is not permitted for remote repository scans"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger with unconfigured branch returns 400 Bad Request (fail-closed)")
    void testTriggerScanUnconfiguredBranchRejectedFailClosed() throws Exception {
        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "feature/unmonitored"
            }
            """, repositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Branch 'feature/unmonitored' is not configured for monitoring on this repository"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger with configured secondary branch succeeds (200 OK)")
    void testTriggerScanConfiguredSecondaryBranchSuccess() throws Exception {
        monitoredBranchRepository.save(MonitoredBranchEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .branchName("develop")
            .branchType("SECONDARY")
            .isActive(true)
            .createdAt(Instant.now())
            .build());

        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "develop"
            }
            """, repositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("develop"))
            .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger with deactivated secondary branch returns 400 Bad Request (fail-closed)")
    void testTriggerScanDeactivatedSecondaryBranchRejectedFailClosed() throws Exception {
        monitoredBranchRepository.save(MonitoredBranchEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .branchName("old-feature")
            .branchType("SECONDARY")
            .isActive(false)
            .createdAt(Instant.now())
            .build());

        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "old-feature"
            }
            """, repositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Branch 'old-feature' is not configured for monitoring on this repository"));
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
    @DisplayName("Monitoring multiple repos on PostgreSQL concurrently and isolating scans between Repo A and Repo B")
    void testConcurrentMultiRepoMonitoringAndScanIsolation() throws Exception {
        // Create second repository for user
        RepositoryEntity repoB = repositoryRepository.save(RepositoryEntity.builder()
            .userId(userEntity.getId())
            .githubRepoId(995511L)
            .owner("scanpilot-tester")
            .name("secondary-repo")
            .fullName("scanpilot-tester/secondary-repo")
            .defaultBranch("main")
            .primaryBranch("develop")
            .isPrivate(true)
            .status("ACTIVE")
            .monitoredAt(Instant.now())
            .build());

        monitoredBranchRepository.save(MonitoredBranchEntity.builder()
            .repositoryId(repoB.getId())
            .branchName("develop")
            .branchType("PRIMARY")
            .isActive(true)
            .createdAt(Instant.now())
            .build());

        // Scan Repo A
        String requestA = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main"
            }
            """, repositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestA))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("main"))
            .andExpect(jsonPath("$.status").value("QUEUED"));

        // Scan Repo B
        String requestB = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "develop"
            }
            """, repoB.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestB))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.repositoryId").value(repoB.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("develop"))
            .andExpect(jsonPath("$.status").value("QUEUED"));

        // Verify both repositories exist in PostgreSQL for the user
        List<RepositoryEntity> userRepos = repositoryRepository.findByUserId(userEntity.getId());
        org.assertj.core.api.Assertions.assertThat(userRepos).hasSize(2);
    }

    @Test
    @DisplayName("Modifying UI context in ProjectService does NOT affect target repository of triggerScan")
    void testTriggerScanTargetIndependentOfCurrentProject() throws Exception {
        // User selects Repo A in ProjectService
        projectService.selectRepository(userSession, new SelectRepositoryRequest(
            889900L,
            "scanpilot-tester/target-repo",
            "target-repo",
            "scanpilot-tester",
            "main",
            false
        ));

        // Create Repo B in PostgreSQL
        RepositoryEntity repoB = repositoryRepository.save(RepositoryEntity.builder()
            .userId(userEntity.getId())
            .githubRepoId(995511L)
            .owner("scanpilot-tester")
            .name("secondary-repo")
            .fullName("scanpilot-tester/secondary-repo")
            .defaultBranch("main")
            .primaryBranch("main")
            .isPrivate(true)
            .status("ACTIVE")
            .monitoredAt(Instant.now())
            .build());

        monitoredBranchRepository.save(MonitoredBranchEntity.builder()
            .repositoryId(repoB.getId())
            .branchName("main")
            .branchType("PRIMARY")
            .isActive(true)
            .createdAt(Instant.now())
            .build());

        // Trigger scan explicitly for Repo B
        String requestB = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main"
            }
            """, repoB.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestB))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.repositoryId").value(repoB.getId().toString()))
            .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    @DisplayName("Reselecting repo with new default branch deactivates old primary and rejects scan on old primary (Issue #53)")
    void testReselectDefaultBranchDeactivatesOldPrimaryAndRejectsScanOnOldPrimary() throws Exception {
        // Initial state: repositoryEntity defaultBranch is 'main', primary active branch is 'main'
        // Reselect repository with new default branch 'develop'
        projectService.selectRepository(userSession, new SelectRepositoryRequest(
            889900L,
            "scanpilot-tester/target-repo",
            "target-repo",
            "scanpilot-tester",
            "develop",
            false
        ));

        // Ensure database has exactly 1 active PRIMARY row
        List<MonitoredBranchEntity> activePrimary = monitoredBranchRepository
            .findByRepositoryIdAndIsActiveTrue(repositoryEntity.getId())
            .stream()
            .filter(b -> "PRIMARY".equalsIgnoreCase(b.getBranchType()))
            .toList();
        org.assertj.core.api.Assertions.assertThat(activePrimary).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(activePrimary.get(0).getBranchName()).isEqualTo("develop");

        // Old primary branch 'main' should now be deactivated (isActive=false)
        Optional<MonitoredBranchEntity> oldPrimary = monitoredBranchRepository
            .findByRepositoryIdAndBranchName(repositoryEntity.getId(), "main");
        org.assertj.core.api.Assertions.assertThat(oldPrimary).isPresent();
        org.assertj.core.api.Assertions.assertThat(oldPrimary.get().getIsActive()).isFalse();

        // 1. Scan on old primary branch 'main' is rejected with HTTP 400 Bad Request
        String requestOld = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main"
            }
            """, repositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestOld))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Branch 'main' is not configured for monitoring on this repository"));

        // 2. Scan on new primary branch 'develop' succeeds
        String requestNew = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "develop"
            }
            """, repositoryEntity.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestNew))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.branchName").value("develop"))
            .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger when branch exists on RepositoryEntity but has no active row in monitored_branches returns 400 Bad Request (fail-closed)")
    void testTriggerScanBranchWithoutActiveMonitoredRowFailClosed() throws Exception {
        RepositoryEntity unlinkedRepo = repositoryRepository.save(RepositoryEntity.builder()
            .userId(userEntity.getId())
            .githubRepoId(554433L)
            .owner("scanpilot-tester")
            .name("unlinked-repo")
            .fullName("scanpilot-tester/unlinked-repo")
            .defaultBranch("main")
            .primaryBranch("main")
            .isPrivate(false)
            .status("ACTIVE")
            .monitoredAt(Instant.now())
            .build());

        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main"
            }
            """, unlinkedRepo.getId());

        mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Branch 'main' is not configured for monitoring on this repository"));
    }

    @Test
    @DisplayName("GET /api/v1/scans/jobs/{jobId} returns job details when owned by authenticated user")
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
    @DisplayName("GET /api/v1/scans/jobs/{jobId} returns 404 when jobId belongs to another user")
    void testGetScanJobUnauthorizedReturns404() throws Exception {
        ScanJobEntity otherJob = scanJobRepository.save(ScanJobEntity.builder()
            .repositoryId(otherRepositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("COMPLETED")
            .commitSha("fedcba0987654321fedcba0987654321fedcba09")
            .durationMs(400L)
            .startedAt(Instant.now().minusSeconds(10))
            .completedAt(Instant.now())
            .build());

        mockMvc.perform(get("/api/v1/scans/jobs/" + otherJob.getId())
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/scans/jobs/{jobId} returns 404 when not found")
    void testGetScanJobNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/scans/jobs/" + UUID.randomUUID())
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/scans/repositories/{repositoryId}/findings returns findings when authorized")
    void testGetFindingsAuthorized() throws Exception {
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
    @DisplayName("GET /api/v1/scans/repositories/{repositoryId}/findings returns 404 for unauthorized repo")
    void testGetFindingsUnauthorizedReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/scans/repositories/" + otherRepositoryEntity.getId() + "/findings")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/scans/repositories/{repositoryId}/coverage returns latest coverage record when authorized")
    void testGetCoverageSummaryAuthorized() throws Exception {
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

    @Test
    @DisplayName("GET /api/v1/scans/repositories/{repositoryId}/coverage returns 404 for unauthorized repo")
    void testGetCoverageUnauthorizedReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/scans/repositories/" + otherRepositoryEntity.getId() + "/coverage")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/scans/jobs/{jobId}/events returns 401 unauthenticated when no session cookie")
    void testGetScanEventsUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/scans/jobs/" + UUID.randomUUID() + "/events"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/scans/jobs/{jobId}/events returns 404 for non-owner job")
    void testGetScanEventsNonOwnerReturns404() throws Exception {
        ScanJobEntity otherJob = scanJobRepository.save(ScanJobEntity.builder()
            .repositoryId(otherRepositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("RUNNING")
            .stage("SCANNING_SECRETS")
            .nextEventSequence(2L)
            .createdAt(Instant.now())
            .build());

        mockMvc.perform(get("/api/v1/scans/jobs/" + otherJob.getId() + "/events")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/scans/jobs/{jobId}/events returns ascending events with lastSequence and hasMore metadata")
    void testGetScanEventsAuthorizedReturnsEvents() throws Exception {
        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("RUNNING")
            .stage("SCANNING_SECRETS")
            .nextEventSequence(3L)
            .createdAt(Instant.now())
            .build());

        scanEventRepository.save(com.scanpilot.persistence.entity.ScanEventEntity.builder()
            .id(UUID.randomUUID())
            .scanJobId(job.getId())
            .sequenceNumber(1L)
            .stage("FETCHING_SNAPSHOT")
            .eventType("STAGE_TRANSITION")
            .messageCode("STAGE_STARTED")
            .payloadJson("{\"stage\":\"FETCHING_SNAPSHOT\"}")
            .createdAt(Instant.now().minusSeconds(5))
            .build());

        scanEventRepository.save(com.scanpilot.persistence.entity.ScanEventEntity.builder()
            .id(UUID.randomUUID())
            .scanJobId(job.getId())
            .sequenceNumber(2L)
            .stage("FETCHING_SNAPSHOT")
            .eventType("SNAPSHOT_ACQUIRED")
            .messageCode("SNAPSHOT_FETCHED")
            .payloadJson("{\"archiveBytes\":1024,\"workspaceBytes\":2048,\"entryCount\":5}")
            .createdAt(Instant.now().minusSeconds(4))
            .build());

        scanEventRepository.save(com.scanpilot.persistence.entity.ScanEventEntity.builder()
            .id(UUID.randomUUID())
            .scanJobId(job.getId())
            .sequenceNumber(3L)
            .stage("CLASSIFYING_FILES")
            .eventType("STAGE_TRANSITION")
            .messageCode("STAGE_STARTED")
            .payloadJson("{\"stage\":\"CLASSIFYING_FILES\"}")
            .createdAt(Instant.now().minusSeconds(3))
            .build());

        // Fetch first page with afterSeq=0, limit=2
        mockMvc.perform(get("/api/v1/scans/jobs/" + job.getId() + "/events?afterSeq=0&limit=2")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(job.getId().toString()))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.lastSequence").value(3))
            .andExpect(jsonPath("$.hasMore").value(true))
            .andExpect(jsonPath("$.events", hasSize(2)))
            .andExpect(jsonPath("$.events[0].sequenceNumber").value(1))
            .andExpect(jsonPath("$.events[0].messageCode").value("STAGE_STARTED"))
            .andExpect(jsonPath("$.events[1].sequenceNumber").value(2))
            .andExpect(jsonPath("$.events[1].messageCode").value("SNAPSHOT_FETCHED"));

        // Fetch second page with afterSeq=2, limit=2
        mockMvc.perform(get("/api/v1/scans/jobs/" + job.getId() + "/events?afterSeq=2&limit=2")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(job.getId().toString()))
            .andExpect(jsonPath("$.lastSequence").value(3))
            .andExpect(jsonPath("$.hasMore").value(false))
            .andExpect(jsonPath("$.events", hasSize(1)))
            .andExpect(jsonPath("$.events[0].sequenceNumber").value(3))
            .andExpect(jsonPath("$.events[0].messageCode").value("STAGE_STARTED"));
    }

    @Test
    @DisplayName("Fail-Closed Pagination: limit <= 0, limit > 50, afterSeq < 0 return 400 Bad Request")
    void testFailClosedPaginationValidation() throws Exception {
        ScanJobEntity job = scanJobRepository.save(ScanJobEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("RUNNING")
            .stage("SCANNING_SECRETS")
            .nextEventSequence(1L)
            .createdAt(Instant.now())
            .build());

        // limit = 0 -> 400
        mockMvc.perform(get("/api/v1/scans/jobs/" + job.getId() + "/events?afterSeq=0&limit=0")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isBadRequest());

        // limit = -1 -> 400
        mockMvc.perform(get("/api/v1/scans/jobs/" + job.getId() + "/events?afterSeq=0&limit=-1")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isBadRequest());

        // limit = 51 -> 400
        mockMvc.perform(get("/api/v1/scans/jobs/" + job.getId() + "/events?afterSeq=0&limit=51")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isBadRequest());

        // afterSeq = -1 -> 400
        mockMvc.perform(get("/api/v1/scans/jobs/" + job.getId() + "/events?afterSeq=-1&limit=50")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AC-C Proof: Persisted CREATED GitHub Issue link survives reload in getFindings endpoint")
    void testPersistedGitHubLinkSurvivesReloadInGetFindings() throws Exception {
        FindingEntity finding1 = findingRepository.save(FindingEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .ruleId("SP-CONFIG-001")
            .fingerprint("fp-linked-1")
            .severity("HIGH")
            .title("Exposed API Key")
            .lifecycle("OPEN")
            .remediationQuality("ACTION_REQUIRED")
            .firstSeenAt(Instant.now())
            .lastSeenAt(Instant.now())
            .build());

        findingLocationRepository.save(FindingLocationEntity.builder()
            .findingId(finding1.getId())
            .filePath("src/main/resources/application.yml")
            .startLine(42)
            .commitSha("abcdef123456")
            .build());

        // Persist CREATED issue link for finding1
        findingIssueLinkRepository.save(com.scanpilot.persistence.entity.FindingIssueLinkEntity.builder()
            .findingId(finding1.getId())
            .repositoryId(repositoryEntity.getId())
            .state("CREATED")
            .githubIssueNumber(42)
            .githubIssueUrl("https://github.com/scanpilot-tester/target-repo/issues/42")
            .idempotencyMarker("scanpilot-finding-" + finding1.getId())
            .createdByUserId(userEntity.getId())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build());

        // Create an unlinked finding2
        FindingEntity finding2 = findingRepository.save(FindingEntity.builder()
            .repositoryId(repositoryEntity.getId())
            .ruleId("SP-CONFIG-001")
            .fingerprint("fp-unlinked-2")
            .severity("MEDIUM")
            .title("Exposed JWT Secret")
            .lifecycle("OPEN")
            .remediationQuality("ACTION_REQUIRED")
            .firstSeenAt(Instant.now())
            .lastSeenAt(Instant.now())
            .build());

        findingLocationRepository.save(FindingLocationEntity.builder()
            .findingId(finding2.getId())
            .filePath("src/main/resources/jwt.properties")
            .startLine(10)
            .commitSha("abcdef123456")
            .build());

        mockMvc.perform(get("/api/v1/scans/repositories/" + repositoryEntity.getId() + "/findings")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(finding1.getId().toString()))
            .andExpect(jsonPath("$[0].githubIssueNumber").value(42))
            .andExpect(jsonPath("$[0].githubIssueUrl").value("https://github.com/scanpilot-tester/target-repo/issues/42"))
            .andExpect(jsonPath("$[0].issueLinkState").value("CREATED"))
            .andExpect(jsonPath("$[0].lifecycle").value("OPEN"))
            .andExpect(jsonPath("$[1].id").value(finding2.getId().toString()))
            .andExpect(jsonPath("$[1].githubIssueNumber").doesNotExist())
            .andExpect(jsonPath("$[1].githubIssueUrl").doesNotExist())
            .andExpect(jsonPath("$[1].issueLinkState").doesNotExist());
    }
}
