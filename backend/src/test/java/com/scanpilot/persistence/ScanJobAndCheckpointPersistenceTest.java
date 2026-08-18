package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanCheckpointEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanCheckpointRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Scan Job and Checkpoint Persistence Tests")
class ScanJobAndCheckpointPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private ScanCheckpointRepository checkpointRepository;

    private RepositoryEntity testRepo;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(2001L)
                .login("scan_owner")
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(88001L)
                .fullName("scan_owner/sample-service")
                .build());
    }

    @Nested
    @DisplayName("Scan Job Persistence Tests")
    class ScanJobTests {

        @Test
        @DisplayName("Should persist scan jobs and query by status and ordering")
        void shouldPersistAndQueryScanJobs() {
            Instant now = Instant.now();

            ScanJobEntity job1 = ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .scanMode("SNAPSHOT")
                    .status("COMPLETED")
                    .commitSha("abc111222333")
                    .durationMs(1420L)
                    .startedAt(now.minus(10, ChronoUnit.MINUTES))
                    .completedAt(now.minus(9, ChronoUnit.MINUTES))
                    .build();

            ScanJobEntity job2 = ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .scanMode("HISTORY")
                    .status("IN_PROGRESS")
                    .commitSha("def444555666")
                    .startedAt(now)
                    .build();

            scanJobRepository.saveAll(List.of(job1, job2));

            List<ScanJobEntity> jobsOrdered = scanJobRepository.findByRepositoryIdOrderByStartedAtDesc(testRepo.getId());
            assertThat(jobsOrdered).hasSize(2);
            assertThat(jobsOrdered.get(0).getCommitSha()).isEqualTo("def444555666");

            List<ScanJobEntity> inProgressJobs = scanJobRepository.findByRepositoryIdAndStatus(testRepo.getId(), "IN_PROGRESS");
            assertThat(inProgressJobs).hasSize(1);

            Optional<ScanJobEntity> latestMainJob = scanJobRepository
                    .findTopByRepositoryIdAndBranchNameOrderByStartedAtDesc(testRepo.getId(), "main");
            assertThat(latestMainJob).isPresent();
            assertThat(latestMainJob.get().getScanMode()).isEqualTo("HISTORY");
        }
    }

    @Nested
    @DisplayName("Scan Checkpoint Persistence Tests")
    class ScanCheckpointTests {

        @Test
        @DisplayName("Should persist scan checkpoints and query the latest verified checkpoint")
        void shouldPersistAndRetrieveCheckpoints() {
            Instant now = Instant.now();

            ScanJobEntity completedJob = scanJobRepository.save(ScanJobEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .scanMode("HISTORY")
                    .status("COMPLETED")
                    .commitSha("commit_sha_base_001")
                    .durationMs(3500L)
                    .startedAt(now.minus(1, ChronoUnit.HOURS))
                    .completedAt(now.minus(58, ChronoUnit.MINUTES))
                    .build());

            ScanCheckpointEntity checkpoint1 = ScanCheckpointEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .verifiedCommitSha("commit_sha_base_001")
                    .scanJobId(completedJob.getId())
                    .createdAt(now.minus(58, ChronoUnit.MINUTES))
                    .build();

            ScanCheckpointEntity checkpoint2 = ScanCheckpointEntity.builder()
                    .repositoryId(testRepo.getId())
                    .branchName("main")
                    .verifiedCommitSha("commit_sha_base_002")
                    .scanJobId(completedJob.getId())
                    .createdAt(now)
                    .build();

            checkpointRepository.saveAll(List.of(checkpoint1, checkpoint2));

            Optional<ScanCheckpointEntity> latest = checkpointRepository
                    .findTopByRepositoryIdAndBranchNameOrderByCreatedAtDesc(testRepo.getId(), "main");

            assertThat(latest).isPresent();
            assertThat(latest.get().getVerifiedCommitSha()).isEqualTo("commit_sha_base_002");
            assertThat(latest.get().getScanJobId()).isEqualTo(completedJob.getId());
        }
    }
}
