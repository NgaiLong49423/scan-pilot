package com.scanpilot.project.service;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.project.dto.BranchConfigRequest;
import com.scanpilot.project.dto.SelectRepositoryRequest;
import com.scanpilot.project.model.MonitoredProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectServiceTest {

    private ProjectService projectService;
    private UserSession userSession;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService();
        userSession = new UserSession(
                "session-123",
                1001L,
                "johndoe",
                "John Doe",
                "https://avatar",
                "john@example.com",
                "gho_token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    @DisplayName("selectRepository derives PRIMARY branch from GitHub default branch and initializes project")
    void testSelectRepositoryDerivesPrimary() {
        SelectRepositoryRequest request = new SelectRepositoryRequest(
                555L,
                "johndoe/secure-vault",
                "secure-vault",
                "johndoe",
                "main",
                true
        );

        MonitoredProject project = projectService.selectRepository(userSession, request);

        assertThat(project).isNotNull();
        assertThat(project.getId()).isNotBlank();
        assertThat(project.getUserId()).isEqualTo(1001L);
        assertThat(project.getGithubRepoId()).isEqualTo(555L);
        assertThat(project.getFullName()).isEqualTo("johndoe/secure-vault");
        assertThat(project.getOwner()).isEqualTo("johndoe");
        assertThat(project.getName()).isEqualTo("secure-vault");
        assertThat(project.getDefaultBranch()).isEqualTo("main");
        assertThat(project.getPrimaryBranch()).isEqualTo("main"); // FR-020, FR-022
        assertThat(project.getSecondaryBranches()).isEmpty();
        assertThat(project.isPrivate()).isTrue();
        assertThat(project.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("selectRepository enforces 1 selected personal repository per user (DEC-046)")
    void testSelectRepositoryEnforcesSingleSelectedRepo() {
        SelectRepositoryRequest repo1 = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "main", false);
        SelectRepositoryRequest repo2 = new SelectRepositoryRequest(666L, "johndoe/repo-two", "repo-two", "johndoe", "master", true);

        projectService.selectRepository(userSession, repo1);
        MonitoredProject project2 = projectService.selectRepository(userSession, repo2);

        Optional<MonitoredProject> current = projectService.getCurrentProject(userSession);
        assertThat(current).isPresent();
        assertThat(current.get().getGithubRepoId()).isEqualTo(666L);
        assertThat(current.get().getFullName()).isEqualTo("johndoe/repo-two");
        assertThat(current.get().getPrimaryBranch()).isEqualTo("master");
    }

    @Test
    @DisplayName("updateBranchConfiguration allows configuring up to 2 secondary branch slots (FR-020)")
    void testUpdateBranchConfigurationValid() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "main", false);
        projectService.selectRepository(userSession, repo);

        BranchConfigRequest request = new BranchConfigRequest(List.of("develop", "release/v1.0"));
        MonitoredProject updated = projectService.updateBranchConfiguration(userSession, request);

        assertThat(updated.getSecondaryBranches()).containsExactly("develop", "release/v1.0");
    }

    @Test
    @DisplayName("updateBranchConfiguration rejects more than 2 secondary branches (FR-020, FR-023)")
    void testUpdateBranchConfigurationExceedsMax() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "main", false);
        projectService.selectRepository(userSession, repo);

        BranchConfigRequest request = new BranchConfigRequest(List.of("develop", "release/v1.0", "staging"));
        assertThatThrownBy(() -> projectService.updateBranchConfiguration(userSession, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum of 2 secondary branches allowed");
    }

    @Test
    @DisplayName("updateBranchConfiguration excludes duplicates and branch identical to primary")
    void testUpdateBranchConfigurationCleansInput() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "main", false);
        projectService.selectRepository(userSession, repo);

        // "main" is primary branch, "develop" is duplicated
        BranchConfigRequest request = new BranchConfigRequest(List.of("develop", "main", "develop", "feature/auth"));
        MonitoredProject updated = projectService.updateBranchConfiguration(userSession, request);

        assertThat(updated.getSecondaryBranches()).containsExactly("develop", "feature/auth");
    }

    @Test
    @DisplayName("updateBranchConfiguration throws NoSuchElementException when no project monitored")
    void testUpdateBranchConfigurationNoProject() {
        BranchConfigRequest request = new BranchConfigRequest(List.of("develop"));
        assertThatThrownBy(() -> projectService.updateBranchConfiguration(userSession, request))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("handleDefaultBranchSync automatically updates primary branch (FR-022)")
    void testHandleDefaultBranchSync() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "master", false);
        MonitoredProject project = projectService.selectRepository(userSession, repo);

        projectService.updateBranchConfiguration(userSession, new BranchConfigRequest(List.of("staging")));

        projectService.handleDefaultBranchSync(project, "main");

        assertThat(project.getDefaultBranch()).isEqualTo("main");
        assertThat(project.getPrimaryBranch()).isEqualTo("main"); // FR-022
        assertThat(project.getSecondaryBranches()).containsExactly("staging"); // FR-023
    }

    @Test
    @DisplayName("handleDefaultBranchSync retains secondary branches and removes new default from secondary if present (FR-022, FR-023)")
    void testHandleDefaultBranchSyncRemovesNewDefaultFromSecondary() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "master", false);
        MonitoredProject project = projectService.selectRepository(userSession, repo);

        projectService.updateBranchConfiguration(userSession, new BranchConfigRequest(List.of("main", "release/v2.0")));
        assertThat(project.getSecondaryBranches()).containsExactly("main", "release/v2.0");

        // GitHub changes default branch from master -> main
        projectService.handleDefaultBranchSync(project, "main");

        assertThat(project.getPrimaryBranch()).isEqualTo("main");
        // "main" promoted to primary, leaving "release/v2.0" as secondary
        assertThat(project.getSecondaryBranches()).containsExactly("release/v2.0");
    }

    @Test
    @DisplayName("MonitoredProject supports Lombok builder, setters, and toString")
    void testMonitoredProjectLombokFeatures() {
        MonitoredProject project = MonitoredProject.builder()
                .id("proj-1")
                .userId(1001L)
                .githubRepoId(999L)
                .owner("owner1")
                .name("repo1")
                .fullName("owner1/repo1")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(true)
                .status("ACTIVE")
                .build();

        assertThat(project.getId()).isEqualTo("proj-1");
        assertThat(project.getFullName()).isEqualTo("owner1/repo1");
        assertThat(project.isPrivate()).isTrue();
        assertThat(project.toString()).contains("owner1/repo1");

        project.setStatus("PAUSED");
        assertThat(project.getStatus()).isEqualTo("PAUSED");
    }
}
