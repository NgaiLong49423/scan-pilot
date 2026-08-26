package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanEventEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanEventRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("ScanEventRepository PostgreSQL CTE Integration Tests")
public class ScanEventRepositoryPostgresTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scanpilot_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
            registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
            registry.add("spring.flyway.url", postgres::getJdbcUrl);
            registry.add("spring.flyway.user", postgres::getUsername);
            registry.add("spring.flyway.password", postgres::getPassword);
        }
    }

    @BeforeAll
    static void checkDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available; skipping Testcontainers PostgreSQL test");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private ScanEventRepository scanEventRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private ScanJobEntity testJob;

    @BeforeEach
    void setUp() {
        scanEventRepository.deleteAll();
        scanJobRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(7001L)
                .login("cte_test_user")
                .build());

        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(99001L)
                .fullName("cte_test_user/telemetry-repo")
                .build());

        testJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("RUNNING")
                .stage("FETCHING_SNAPSHOT")
                .nextEventSequence(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("1. Atomic CTE allocates sequence and inserts event in 1 statement")
    void testAtomicCTEAllocatesSequenceAndInsertsEvent() {
        UUID eventId = UUID.randomUUID();
        Optional<Long> allocatedSeq = scanEventRepository.insertEventAtomicCTE(
                testJob.getId(),
                95L,
                eventId,
                "FETCHING_SNAPSHOT",
                "STAGE_TRANSITION",
                "STAGE_STARTED",
                "{\"stage\":\"FETCHING_SNAPSHOT\"}",
                Instant.now()
        );

        assertThat(allocatedSeq).isPresent().contains(1L);

        List<ScanEventEntity> events = scanEventRepository.findByScanJobIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                testJob.getId(), 0L, PageRequest.of(0, 10));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getId()).isEqualTo(eventId);
        assertThat(events.get(0).getSequenceNumber()).isEqualTo(1L);
        assertThat(events.get(0).getMessageCode()).isEqualTo("STAGE_STARTED");
    }

    @Test
    @DisplayName("2. Atomic CTE with cap 95 suppresses non-terminal events when sequence >= 95 and does not increment sequence")
    void testAtomicCTEWithCap95SuppressesNonTerminalAndDoesNotIncrementSequence() {
        jdbcTemplate.update("UPDATE scan_jobs SET next_event_sequence = 95 WHERE id = ?", testJob.getId());

        Optional<Long> allocatedSeq = scanEventRepository.insertEventAtomicCTE(
                testJob.getId(),
                95L,
                UUID.randomUUID(),
                "SCANNING_SECRETS",
                "FINDING_DISCOVERED",
                "FINDING_ALERT",
                "{\"findingIndex\":96}",
                Instant.now()
        );

        assertThat(allocatedSeq).isEmpty();

        ScanJobEntity updatedJob = scanJobRepository.findById(testJob.getId()).orElseThrow();
        assertThat(updatedJob.getNextEventSequence()).isEqualTo(95L);
    }

    @Test
    @DisplayName("3. Atomic CTE with cap 100 allows terminal slots 96 to 100")
    void testAtomicCTEWithCap100AllowsTerminalSlots96To100() {
        jdbcTemplate.update("UPDATE scan_jobs SET next_event_sequence = 95 WHERE id = ?", testJob.getId());

        Optional<Long> allocatedSeq = scanEventRepository.insertEventAtomicCTE(
                testJob.getId(),
                100L,
                UUID.randomUUID(),
                "COMPLETED",
                "SCAN_COMPLETED",
                "JOB_COMPLETED",
                "{\"durationMs\":1500}",
                Instant.now()
        );

        assertThat(allocatedSeq).isPresent().contains(96L);

        ScanJobEntity updatedJob = scanJobRepository.findById(testJob.getId()).orElseThrow();
        assertThat(updatedJob.getNextEventSequence()).isEqualTo(96L);
    }

    @Test
    @DisplayName("4. Atomic CTE rollback on insert failure leaves sequence unchanged")
    void testAtomicCTERollbackOnInsertFailureLeavesSequenceUnchanged() {
        UUID duplicateId = UUID.randomUUID();
        Optional<Long> firstSeq = scanEventRepository.insertEventAtomicCTE(
                testJob.getId(),
                95L,
                duplicateId,
                "FETCHING_SNAPSHOT",
                "STAGE_TRANSITION",
                "STAGE_STARTED",
                "{}",
                Instant.now()
        );
        assertThat(firstSeq).isPresent().contains(1L);

        // Attempting to insert duplicate primary key event ID must fail and rollback (returning empty / handled)
        Optional<Long> duplicateInsert = scanEventRepository.insertEventAtomicCTE(
                testJob.getId(),
                95L,
                duplicateId,
                "CLASSIFYING_FILES",
                "STAGE_TRANSITION",
                "STAGE_STARTED",
                "{}",
                Instant.now()
        );
        assertThat(duplicateInsert).isEmpty();

        ScanJobEntity updatedJob = scanJobRepository.findById(testJob.getId()).orElseThrow();
        assertThat(updatedJob.getNextEventSequence()).isEqualTo(1L);
    }

    @Test
    @DisplayName("5. Zero sequence gaps under high concurrency")
    void testZeroSequenceGapsUnderHighConcurrency() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Optional<Long>>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            tasks.add(() -> scanEventRepository.insertEventAtomicCTE(
                    testJob.getId(),
                    95L,
                    UUID.randomUUID(),
                    "SCANNING_SECRETS",
                    "FINDING_DISCOVERED",
                    "FINDING_ALERT",
                    "{\"findingIndex\":" + idx + "}",
                    Instant.now()
            ));
        }

        List<Future<Optional<Long>>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        List<Long> allocatedSeqs = new ArrayList<>();
        for (Future<Optional<Long>> f : futures) {
            Optional<Long> seq = f.get();
            seq.ifPresent(allocatedSeqs::add);
        }

        Collections.sort(allocatedSeqs);
        assertThat(allocatedSeqs).hasSize(threadCount);
        for (int i = 0; i < threadCount; i++) {
            assertThat(allocatedSeqs.get(i)).isEqualTo((long) (i + 1));
        }
    }

    @Test
    @DisplayName("6. Worker restart resumes monotonic sequence")
    void testWorkerRestartResumesMonotonicSequence() {
        Optional<Long> seq1 = scanEventRepository.insertEventAtomicCTE(
                testJob.getId(),
                95L,
                UUID.randomUUID(),
                "FETCHING_SNAPSHOT",
                "STAGE_TRANSITION",
                "STAGE_STARTED",
                "{}",
                Instant.now()
        );
        assertThat(seq1).isPresent().contains(1L);

        // Simulate new worker loading job from DB with nextEventSequence = 1
        ScanJobEntity reloadedJob = scanJobRepository.findById(testJob.getId()).orElseThrow();
        assertThat(reloadedJob.getNextEventSequence()).isEqualTo(1L);

        Optional<Long> seq2 = scanEventRepository.insertEventAtomicCTE(
                reloadedJob.getId(),
                95L,
                UUID.randomUUID(),
                "CLASSIFYING_FILES",
                "STAGE_TRANSITION",
                "STAGE_STARTED",
                "{}",
                Instant.now()
        );
        assertThat(seq2).isPresent().contains(2L);
    }

    @Test
    @DisplayName("7. Source code inspection confirms absence of fallback allocator in production classes")
    void testNoFallbackAllocatorInSource() throws Exception {
        Path dispatcherPath = Path.of("src/main/java/com/scanpilot/scanner/dispatcher/ScanJobDispatcher.java");
        Path pipelinePath = Path.of("src/main/java/com/scanpilot/scanner/pipeline/ScanPipelineService.java");
        Path jobRepoPath = Path.of("src/main/java/com/scanpilot/persistence/repository/ScanJobRepository.java");

        if (Files.exists(dispatcherPath)) {
            String dispatcherCode = Files.readString(dispatcherPath);
            assertThat(dispatcherCode).doesNotContain("updateNextEventSequence");
            assertThat(dispatcherCode).doesNotContain("maxSeq");
        }
        if (Files.exists(pipelinePath)) {
            String pipelineCode = Files.readString(pipelinePath);
            assertThat(pipelineCode).doesNotContain("updateNextEventSequence");
            assertThat(pipelineCode).doesNotContain("maxSeq");
        }
        if (Files.exists(jobRepoPath)) {
            String repoCode = Files.readString(jobRepoPath);
            assertThat(repoCode).doesNotContain("updateNextEventSequence");
        }
    }
}
