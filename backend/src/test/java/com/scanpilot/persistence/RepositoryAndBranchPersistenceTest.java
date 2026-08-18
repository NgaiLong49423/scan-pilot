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
    }
}
