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

    private UserSession userSession;
    private UserEntity userEntity;
    private RepositoryEntity repositoryEntity;

    private UserSession otherUserSession;
    private UserEntity otherUserEntity;
    private RepositoryEntity otherRepositoryEntity;

    @BeforeEach
    void setUp() {
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
    @DisplayName("POST /api/v1/scans/trigger triggers scan when authenticated with valid repository UUID and completed pipeline")
    void testTriggerScanAuthenticated() throws Exception {
        ScanJobEntity completedJob = ScanJobEntity.builder()
            .id(UUID.randomUUID())
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("COMPLETED")
            .startedAt(Instant.now().minusSeconds(1))
            .completedAt(Instant.now())
            .durationMs(450L)
            .build();
        when(scanPipelineService.executeScan(eq(repositoryEntity.getId()), eq("main"), any()))
            .thenReturn(completedJob);

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
            .andExpect(jsonPath("$.jobId").value(completedJob.getId().toString()))
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("main"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.message").value("Scan executed successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/scans/trigger when pipeline execution fails returns HTTP 422 Unprocessable Entity with safe message without leaking internal error details")
    void testTriggerScanPipelineFailureReturns422() throws Exception {
        ScanJobEntity failedJob = ScanJobEntity.builder()
            .id(UUID.randomUUID())
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("FAILED")
            .errorMessage("INTERNAL_DIAGNOSTIC_MARKER_DO_NOT_EXPOSE")
            .startedAt(Instant.now().minusSeconds(1))
            .completedAt(Instant.now())
            .durationMs(120L)
            .build();
        when(scanPipelineService.executeScan(eq(repositoryEntity.getId()), eq("main"), any()))
            .thenReturn(failedJob);

        String requestJson = String.format("""
            {
                "repositoryId": "%s",
                "branchName": "main"
            }
            """, repositoryEntity.getId());

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/api/v1/scans/trigger")
                .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.jobId").value(failedJob.getId().toString()))
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("main"))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.message").value("Scan could not complete for the requested repository branch. No new evidence was recorded."))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(responseBody)
            .doesNotContain("INTERNAL_DIAGNOSTIC_MARKER_DO_NOT_EXPOSE")
            .contains("Scan could not complete for the requested repository branch. No new evidence was recorded.");
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

        ScanJobEntity completedJob = ScanJobEntity.builder()
            .id(UUID.randomUUID())
            .repositoryId(repositoryEntity.getId())
            .branchName("develop")
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("COMPLETED")
            .startedAt(Instant.now().minusSeconds(1))
            .completedAt(Instant.now())
            .durationMs(450L)
            .build();
        when(scanPipelineService.executeScan(eq(repositoryEntity.getId()), eq("develop"), any()))
            .thenReturn(completedJob);

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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("develop"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
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

        ScanJobEntity jobA = ScanJobEntity.builder()
            .id(UUID.randomUUID())
            .repositoryId(repositoryEntity.getId())
            .branchName("main")
            .status("COMPLETED")
            .build();
        ScanJobEntity jobB = ScanJobEntity.builder()
            .id(UUID.randomUUID())
            .repositoryId(repoB.getId())
            .branchName("develop")
            .status("COMPLETED")
            .build();
        when(scanPipelineService.executeScan(eq(repositoryEntity.getId()), eq("main"), any())).thenReturn(jobA);
        when(scanPipelineService.executeScan(eq(repoB.getId()), eq("develop"), any())).thenReturn(jobB);

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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repositoryId").value(repositoryEntity.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("main"));

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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repositoryId").value(repoB.getId().toString()))
            .andExpect(jsonPath("$.branchName").value("develop"));

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

        ScanJobEntity jobB = ScanJobEntity.builder()
            .id(UUID.randomUUID())
            .repositoryId(repoB.getId())
            .branchName("main")
            .status("COMPLETED")
            .build();
        when(scanPipelineService.executeScan(eq(repoB.getId()), eq("main"), any())).thenReturn(jobB);

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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.repositoryId").value(repoB.getId().toString()));
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
        ScanJobEntity developJob = ScanJobEntity.builder()
            .id(UUID.randomUUID())
            .repositoryId(repositoryEntity.getId())
            .branchName("develop")
            .status("COMPLETED")
            .build();
        when(scanPipelineService.executeScan(eq(repositoryEntity.getId()), eq("develop"), any()))
            .thenReturn(developJob);

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
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.branchName").value("develop"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
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
}
