package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.MonitoredBranchEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.MonitoredBranchRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Repository and Branch Persistence Tests")
class RepositoryAndBranchPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private MonitoredBranchRepository branchRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(UserEntity.builder()
                .githubUserId(1001L)
                .login("repo_owner")
                .name("Repo Owner")
                .createdAt(Instant.now())
                .build());
    }

    @Nested
    @DisplayName("Repository Entity Tests")
    class RepositoryEntityTests {

        @Test
        @DisplayName("Should persist and query repository by user ID, github repo ID, and full name")
        void shouldPersistAndQueryRepository() {
            RepositoryEntity repo = RepositoryEntity.builder()
                    .userId(testUser.getId())
                    .githubRepoId(99001L)
                    .owner("repo_owner")
                    .name("scan-pilot")
                    .fullName("repo_owner/scan-pilot")
                    .defaultBranch("main")
                    .primaryBranch("main")
                    .isPrivate(true)
                    .status("MONITORED")
                    .monitoredAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            RepositoryEntity saved = repositoryRepository.save(repo);
            assertThat(saved.getId()).isNotNull();

            Optional<RepositoryEntity> byUserAndGithubId = repositoryRepository
                    .findByUserIdAndGithubRepoId(testUser.getId(), 99001L);
            assertThat(byUserAndGithubId).isPresent();
            assertThat(byUserAndGithubId.get().getFullName()).isEqualTo("repo_owner/scan-pilot");

            List<RepositoryEntity> userRepos = repositoryRepository.findByUserId(testUser.getId());
            assertThat(userRepos).hasSize(1);

            Optional<RepositoryEntity> byFullName = repositoryRepository.findByFullName("repo_owner/scan-pilot");
            assertThat(byFullName).isPresent();
            assertThat(byFullName.get().getStatus()).isEqualTo("MONITORED");
        }

        @Test
        @DisplayName("Should enforce UNIQUE(user_id, github_repo_id) constraint")
        void shouldEnforceUserAndGithubRepoIdUniqueness() {
            RepositoryEntity repo1 = RepositoryEntity.builder()
                    .userId(testUser.getId())
                    .githubRepoId(99002L)
                    .fullName("repo_owner/app-one")
                    .build();
            repositoryRepository.saveAndFlush(repo1);

            RepositoryEntity repo2 = RepositoryEntity.builder()
                    .userId(testUser.getId())
                    .githubRepoId(99002L)
                    .fullName("repo_owner/app-one-duplicate")
                    .build();

            assertThatThrownBy(() -> repositoryRepository.saveAndFlush(repo2))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Monitored Branch Entity & Slot Capacity Tests")
    class MonitoredBranchTests {

        @Test
        @DisplayName("Should persist branches, query active branches, and enforce slot counts")
        void shouldManageMonitoredBranches() {
            RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                    .userId(testUser.getId())
                    .githubRepoId(99003L)
                    .fullName("repo_owner/app-three")
                    .build());

            MonitoredBranchEntity primary = MonitoredBranchEntity.builder()
                    .repositoryId(repo.getId())
                    .branchName("main")
                    .branchType("PRIMARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            MonitoredBranchEntity secondary1 = MonitoredBranchEntity.builder()
                    .repositoryId(repo.getId())
                    .branchName("develop")
                    .branchType("SECONDARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            MonitoredBranchEntity secondary2 = MonitoredBranchEntity.builder()
                    .repositoryId(repo.getId())
                    .branchName("feature/auth")
                    .branchType("SECONDARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            branchRepository.saveAll(List.of(primary, secondary1, secondary2));

            List<MonitoredBranchEntity> branches = branchRepository.findByRepositoryId(repo.getId());
            assertThat(branches).hasSize(3);

            long activeCount = branchRepository.countByRepositoryIdAndIsActiveTrue(repo.getId());
            assertThat(activeCount).isEqualTo(3); // 1 Primary + 2 Secondary slots maximum in MVP

            Optional<MonitoredBranchEntity> mainBranch = branchRepository
                    .findByRepositoryIdAndBranchName(repo.getId(), "main");
            assertThat(mainBranch).isPresent();
            assertThat(mainBranch.get().getBranchType()).isEqualTo("PRIMARY");
        }

        @Test
        @DisplayName("Should enforce UNIQUE(repository_id, branch_name) constraint")
        void shouldEnforceBranchNameUniquenessPerRepository() {
            RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                    .userId(testUser.getId())
                    .githubRepoId(99004L)
                    .fullName("repo_owner/app-four")
                    .build());

            MonitoredBranchEntity branch1 = MonitoredBranchEntity.builder()
                    .repositoryId(repo.getId())
                    .branchName("main")
                    .branchType("PRIMARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();
            branchRepository.saveAndFlush(branch1);

            MonitoredBranchEntity branch2 = MonitoredBranchEntity.builder()
                    .repositoryId(repo.getId())
                    .branchName("main")
                    .branchType("SECONDARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            assertThatThrownBy(() -> branchRepository.saveAndFlush(branch2))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should maintain primary and secondary branches independently for multiple repositories")
        void shouldMaintainBranchesIndependentlyPerRepository() {
            RepositoryEntity repoA = repositoryRepository.save(RepositoryEntity.builder()
                    .userId(testUser.getId())
                    .githubRepoId(99005L)
                    .fullName("repo_owner/app-five-a")
                    .primaryBranch("main")
                    .build());

            RepositoryEntity repoB = repositoryRepository.save(RepositoryEntity.builder()
                    .userId(testUser.getId())
                    .githubRepoId(99006L)
                    .fullName("repo_owner/app-five-b")
                    .primaryBranch("develop")
                    .build());

            MonitoredBranchEntity repoAPrimary = MonitoredBranchEntity.builder()
                    .repositoryId(repoA.getId())
                    .branchName("main")
                    .branchType("PRIMARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            MonitoredBranchEntity repoASecondary1 = MonitoredBranchEntity.builder()
                    .repositoryId(repoA.getId())
                    .branchName("staging")
                    .branchType("SECONDARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            MonitoredBranchEntity repoBPrimary = MonitoredBranchEntity.builder()
                    .repositoryId(repoB.getId())
                    .branchName("develop")
                    .branchType("PRIMARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            MonitoredBranchEntity repoBSecondary1 = MonitoredBranchEntity.builder()
                    .repositoryId(repoB.getId())
                    .branchName("release/2.0")
                    .branchType("SECONDARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            branchRepository.saveAll(List.of(repoAPrimary, repoASecondary1, repoBPrimary, repoBSecondary1));

            List<MonitoredBranchEntity> branchesA = branchRepository.findByRepositoryIdAndIsActiveTrue(repoA.getId());
            List<MonitoredBranchEntity> branchesB = branchRepository.findByRepositoryIdAndIsActiveTrue(repoB.getId());

            assertThat(branchesA).hasSize(2);
            assertThat(branchesA).extracting(MonitoredBranchEntity::getBranchName)
                    .containsExactlyInAnyOrder("main", "staging");

            assertThat(branchesB).hasSize(2);
            assertThat(branchesB).extracting(MonitoredBranchEntity::getBranchName)
                    .containsExactlyInAnyOrder("develop", "release/2.0");
        }

        @Test
        @DisplayName("Should query active primary branch and maintain exactly one active PRIMARY row after deactivation")
        void shouldMaintainExactlyOneActivePrimaryRow() {
            RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                    .userId(testUser.getId())
                    .githubRepoId(99007L)
                    .fullName("repo_owner/app-single-primary")
                    .defaultBranch("develop")
                    .primaryBranch("develop")
                    .build());

            MonitoredBranchEntity oldPrimary = MonitoredBranchEntity.builder()
                    .repositoryId(repo.getId())
                    .branchName("main")
                    .branchType("PRIMARY")
                    .isActive(false)
                    .createdAt(Instant.now())
                    .build();

            MonitoredBranchEntity newPrimary = MonitoredBranchEntity.builder()
                    .repositoryId(repo.getId())
                    .branchName("develop")
                    .branchType("PRIMARY")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();

            branchRepository.saveAll(List.of(oldPrimary, newPrimary));

            List<MonitoredBranchEntity> activeBranches = branchRepository.findByRepositoryIdAndIsActiveTrue(repo.getId());
            List<MonitoredBranchEntity> activePrimary = activeBranches.stream()
                    .filter(b -> "PRIMARY".equalsIgnoreCase(b.getBranchType()))
                    .toList();

            assertThat(activePrimary).hasSize(1);
            assertThat(activePrimary.get(0).getBranchName()).isEqualTo("develop");
        }
    }
}
