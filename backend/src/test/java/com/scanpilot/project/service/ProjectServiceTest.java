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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    private ProjectService projectService;
    private com.scanpilot.persistence.repository.UserRepository userRepository;
    private com.scanpilot.persistence.repository.RepositoryRepository repositoryRepository;
    private com.scanpilot.persistence.repository.MonitoredBranchRepository monitoredBranchRepository;
    private UserSession userSession;

    @BeforeEach
    void setUp() {
        userRepository = mock(com.scanpilot.persistence.repository.UserRepository.class);
        repositoryRepository = mock(com.scanpilot.persistence.repository.RepositoryRepository.class);
        monitoredBranchRepository = mock(com.scanpilot.persistence.repository.MonitoredBranchRepository.class);

        java.util.UUID mockUserId = java.util.UUID.randomUUID();
        java.util.UUID mockRepoId = java.util.UUID.randomUUID();

        when(userRepository.findByGithubUserId(any())).thenReturn(Optional.of(
                com.scanpilot.persistence.entity.UserEntity.builder()
                        .id(mockUserId)
                        .githubUserId(1001L)
                        .login("johndoe")
                        .build()
        ));
        when(userRepository.save(any())).thenAnswer(invocation -> {
            com.scanpilot.persistence.entity.UserEntity u = invocation.getArgument(0);
            if (u != null) {
                u.setId(mockUserId);
            }
            return u;
        });

        java.util.Map<java.util.UUID, com.scanpilot.persistence.entity.RepositoryEntity> repoStore = new java.util.concurrent.ConcurrentHashMap<>();

        when(repositoryRepository.findByUserIdAndGithubRepoId(any(), any())).thenReturn(Optional.empty());
        when(repositoryRepository.save(any())).thenAnswer(invocation -> {
            com.scanpilot.persistence.entity.RepositoryEntity r = invocation.getArgument(0);
            if (r != null) {
                if (r.getId() == null) {
                    r.setId(mockRepoId);
                }
                repoStore.put(r.getId(), r);
            }
            return r;
        });
        when(repositoryRepository.findById(any(java.util.UUID.class))).thenAnswer(invocation -> {
            java.util.UUID id = invocation.getArgument(0);
            return Optional.ofNullable(repoStore.get(id));
        });

        projectService = new ProjectService(userRepository, repositoryRepository, monitoredBranchRepository);
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
    @DisplayName("selectRepository fails safely (throws exception) if database persistence fails")
    void testSelectRepositoryDatabaseFailure() {
        when(repositoryRepository.save(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("DB error"));

        SelectRepositoryRequest request = new SelectRepositoryRequest(
                555L,
                "johndoe/secure-vault",
                "secure-vault",
                "johndoe",
                "main",
                true
        );

        assertThatThrownBy(() -> projectService.selectRepository(userSession, request))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
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
        MonitoredProject project = projectService.selectRepository(userSession, repo);
        java.util.UUID repoId = java.util.UUID.fromString(project.getId());

        BranchConfigRequest request = new BranchConfigRequest(repoId, List.of("develop", "release/v1.0"));
        MonitoredProject updated = projectService.updateBranchConfiguration(userSession, request);

        assertThat(updated.getSecondaryBranches()).containsExactly("develop", "release/v1.0");
    }

    @Test
    @DisplayName("updateBranchConfiguration rejects more than 2 secondary branches (FR-020, FR-023)")
    void testUpdateBranchConfigurationExceedsMax() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "main", false);
        MonitoredProject project = projectService.selectRepository(userSession, repo);
        java.util.UUID repoId = java.util.UUID.fromString(project.getId());

        BranchConfigRequest request = new BranchConfigRequest(repoId, List.of("develop", "release/v1.0", "staging"));
        assertThatThrownBy(() -> projectService.updateBranchConfiguration(userSession, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum of 2 secondary branches allowed");
    }

    @Test
    @DisplayName("updateBranchConfiguration excludes duplicates and branch identical to primary")
    void testUpdateBranchConfigurationCleansInput() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "main", false);
        MonitoredProject project = projectService.selectRepository(userSession, repo);
        java.util.UUID repoId = java.util.UUID.fromString(project.getId());

        // "main" is primary branch, "develop" is duplicated
        BranchConfigRequest request = new BranchConfigRequest(repoId, List.of("develop", "main", "develop", "feature/auth"));
        MonitoredProject updated = projectService.updateBranchConfiguration(userSession, request);

        assertThat(updated.getSecondaryBranches()).containsExactly("develop", "feature/auth");
    }

    @Test
    @DisplayName("updateBranchConfiguration throws IllegalArgumentException when repositoryId is null (fail-closed)")
    void testUpdateBranchConfigurationMissingRepoId() {
        BranchConfigRequest request = new BranchConfigRequest(null, List.of("develop"));
        assertThatThrownBy(() -> projectService.updateBranchConfiguration(userSession, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository ID is required");
    }

    @Test
    @DisplayName("updateBranchConfiguration throws NoSuchElementException when repository not found or unauthorized")
    void testUpdateBranchConfigurationRepoNotFound() {
        java.util.UUID unknownId = java.util.UUID.randomUUID();
        when(repositoryRepository.findById(unknownId)).thenReturn(Optional.empty());
        BranchConfigRequest request = new BranchConfigRequest(unknownId, List.of("develop"));
        assertThatThrownBy(() -> projectService.updateBranchConfiguration(userSession, request))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("handleDefaultBranchSync automatically updates primary branch (FR-022)")
    void testHandleDefaultBranchSync() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "master", false);
        MonitoredProject project = projectService.selectRepository(userSession, repo);
        java.util.UUID repoId = java.util.UUID.fromString(project.getId());

        project = projectService.updateBranchConfiguration(userSession, new BranchConfigRequest(repoId, List.of("staging")));

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
        java.util.UUID repoId = java.util.UUID.fromString(project.getId());

        project = projectService.updateBranchConfiguration(userSession, new BranchConfigRequest(repoId, List.of("main", "release/v2.0")));
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

    @Test
    @DisplayName("getAllMonitoredProjects retrieves multiple repositories from PostgreSQL authority")
    void testGetAllMonitoredProjectsFromPostgreSql() {
        java.util.UUID mockUserId = java.util.UUID.randomUUID();
        when(userRepository.findByGithubUserId(1001L)).thenReturn(Optional.of(
                com.scanpilot.persistence.entity.UserEntity.builder()
                        .id(mockUserId)
                        .githubUserId(1001L)
                        .login("johndoe")
                        .build()
        ));

        com.scanpilot.persistence.entity.RepositoryEntity repo1 = com.scanpilot.persistence.entity.RepositoryEntity.builder()
                .id(java.util.UUID.randomUUID())
                .userId(mockUserId)
                .githubRepoId(111L)
                .owner("johndoe")
                .name("repo-alpha")
                .fullName("johndoe/repo-alpha")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build();

        com.scanpilot.persistence.entity.RepositoryEntity repo2 = com.scanpilot.persistence.entity.RepositoryEntity.builder()
                .id(java.util.UUID.randomUUID())
                .userId(mockUserId)
                .githubRepoId(222L)
                .owner("johndoe")
                .name("repo-beta")
                .fullName("johndoe/repo-beta")
                .defaultBranch("master")
                .primaryBranch("master")
                .isPrivate(true)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build();

        when(repositoryRepository.findByUserId(mockUserId)).thenReturn(List.of(repo1, repo2));
        when(monitoredBranchRepository.findByRepositoryIdAndIsActiveTrue(repo1.getId())).thenReturn(List.of(
                com.scanpilot.persistence.entity.MonitoredBranchEntity.builder()
                        .repositoryId(repo1.getId())
                        .branchName("develop")
                        .branchType("SECONDARY")
                        .isActive(true)
                        .build()
        ));
        when(monitoredBranchRepository.findByRepositoryIdAndIsActiveTrue(repo2.getId())).thenReturn(List.of());

        List<MonitoredProject> monitored = projectService.getAllMonitoredProjects(userSession);

        assertThat(monitored).hasSize(2);
        assertThat(monitored.get(0).getFullName()).isEqualTo("johndoe/repo-alpha");
        assertThat(monitored.get(0).getSecondaryBranches()).containsExactly("develop");
        assertThat(monitored.get(1).getFullName()).isEqualTo("johndoe/repo-beta");
        assertThat(monitored.get(1).getSecondaryBranches()).isEmpty();
    }

    @Test
    @DisplayName("selectRepository persists PRIMARY branch to MonitoredBranchRepository")
    void testSelectRepositoryPersistsPrimaryBranch() {
        SelectRepositoryRequest request = new SelectRepositoryRequest(
                555L,
                "johndoe/secure-vault",
                "secure-vault",
                "johndoe",
                "main",
                true
        );

        when(monitoredBranchRepository.findByRepositoryIdAndBranchName(any(), any())).thenReturn(Optional.empty());

        MonitoredProject project = projectService.selectRepository(userSession, request);

        assertThat(project).isNotNull();
        org.mockito.Mockito.verify(monitoredBranchRepository).save(org.mockito.ArgumentMatchers.argThat(b ->
                "main".equals(b.getBranchName()) && "PRIMARY".equals(b.getBranchType()) && Boolean.TRUE.equals(b.getIsActive())
        ));
    }

    @Test
    @DisplayName("updateBranchConfiguration persists SECONDARY branches to MonitoredBranchRepository and deactivates removed")
    void testUpdateBranchConfigurationPersistsSecondaryBranches() {
        SelectRepositoryRequest repo = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "main", false);
        MonitoredProject project = projectService.selectRepository(userSession, repo);
        java.util.UUID repoId = java.util.UUID.fromString(project.getId());

        com.scanpilot.persistence.entity.MonitoredBranchEntity oldBranch = com.scanpilot.persistence.entity.MonitoredBranchEntity.builder()
                .repositoryId(repoId)
                .branchName("old-feat")
                .branchType("SECONDARY")
                .isActive(true)
                .build();

        when(monitoredBranchRepository.findByRepositoryId(repoId)).thenReturn(new java.util.ArrayList<>(List.of(oldBranch)));

        BranchConfigRequest request = new BranchConfigRequest(repoId, List.of("develop", "release/v1.0"));
        MonitoredProject updated = projectService.updateBranchConfiguration(userSession, request);

        assertThat(updated.getSecondaryBranches()).containsExactly("develop", "release/v1.0");
        assertThat(oldBranch.getIsActive()).isFalse(); // old secondary branch deactivated
        org.mockito.Mockito.verify(monitoredBranchRepository).save(oldBranch);
    }

    @Test
    @DisplayName("selectRepository with new default branch updates primary and deactivates old primary in MonitoredBranchRepository")
    void testReselectDefaultBranchDeactivatesOldPrimary() {
        SelectRepositoryRequest initialReq = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "main", false);
        MonitoredProject initialProj = projectService.selectRepository(userSession, initialReq);
        java.util.UUID repoId = java.util.UUID.fromString(initialProj.getId());

        com.scanpilot.persistence.entity.MonitoredBranchEntity oldPrimaryRow = com.scanpilot.persistence.entity.MonitoredBranchEntity.builder()
                .repositoryId(repoId)
                .branchName("main")
                .branchType("PRIMARY")
                .isActive(true)
                .build();

        when(monitoredBranchRepository.findByRepositoryId(repoId)).thenReturn(new java.util.ArrayList<>(List.of(oldPrimaryRow)));

        // Reselect repo with new default branch 'develop'
        SelectRepositoryRequest reselectReq = new SelectRepositoryRequest(555L, "johndoe/repo-one", "repo-one", "johndoe", "develop", false);
        MonitoredProject updatedProj = projectService.selectRepository(userSession, reselectReq);

        assertThat(updatedProj.getDefaultBranch()).isEqualTo("develop");
        assertThat(updatedProj.getPrimaryBranch()).isEqualTo("develop");

        // Old primary branch 'main' was deactivated
        assertThat(oldPrimaryRow.getIsActive()).isFalse();
        org.mockito.Mockito.verify(monitoredBranchRepository).save(oldPrimaryRow);

        // New primary branch 'develop' was saved with branchType=PRIMARY and isActive=true
        org.mockito.Mockito.verify(monitoredBranchRepository).save(org.mockito.ArgumentMatchers.argThat(b ->
                "develop".equals(b.getBranchName()) && "PRIMARY".equals(b.getBranchType()) && Boolean.TRUE.equals(b.getIsActive())
        ));
    }
}
