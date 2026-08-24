package com.scanpilot.scanner.dispatcher;

import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.config.ScanWorkerInstance;
import com.scanpilot.scanner.pipeline.ScanPipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("ScanJobDispatcher Integration & Unit Tests")
class ScanJobDispatcherTest {

    @Autowired
    private ScanJobDispatcher scanJobDispatcher;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScanWorkerInstance scanWorkerInstance;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ScanPipelineService scanPipelineService;

    private RepositoryEntity repositoryEntity;

    @BeforeEach
    void setUp() {
        scanJobRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(123456L)
                .login("dispatcher-tester")
                .name("Dispatcher Tester")
                .build());

        repositoryEntity = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(998877L)
                .owner("dispatcher-tester")
                .name("test-repo")
                .fullName("dispatcher-tester/test-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .build());
    }

    @Test
    @DisplayName("Test 1: Worker only executes after transaction commits and job is fully visible in DB")
    void testWorkerExecutesAfterTransactionCommit() throws Exception {
        AtomicBoolean jobVisibleDuringExecution = new AtomicBoolean(false);
        CountDownLatch executionLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            UUID jobId = invocation.getArgument(0);
            Optional<ScanJobEntity> entity = scanJobRepository.findById(jobId);
            if (entity.isPresent() && "QUEUED".equals(entity.get().getStatus())) {
                jobVisibleDuringExecution.set(true);
            }
            executionLatch.countDown();
            return null;
        }).when(scanPipelineService).executeScanJob(any(UUID.class));

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        ScanJobEntity dispatchedJob = txTemplate.execute(status -> scanJobDispatcher.dispatch(repositoryEntity, "main"));

        assertThat(dispatchedJob).isNotNull();
        assertThat(dispatchedJob.getStatus()).isEqualTo("QUEUED");
        assertThat(dispatchedJob.getStage()).isEqualTo("QUEUED");
        assertThat(dispatchedJob.getWorkerInstanceId()).isEqualTo(scanWorkerInstance.getInstanceId());

        boolean executed = executionLatch.await(5, TimeUnit.SECONDS);
        assertThat(executed).isTrue();
        assertThat(jobVisibleDuringExecution.get()).isTrue();
        verify(scanPipelineService).executeScanJob(dispatchedJob.getId());
    }

    @Test
    @DisplayName("Test 2: Executor rejection after commit transitions job to FAILED with safe message and leaves 0 orphan QUEUED jobs")
    void testExecutorRejectionAfterCommitTransitionsJobToFailed() {
        ThreadPoolTaskExecutor mockExecutor = org.mockito.Mockito.mock(ThreadPoolTaskExecutor.class);
        doThrow(new RejectedExecutionException("Queue full with secret token ghp_secretmarker1234567890"))
                .when(mockExecutor).execute(any(Runnable.class));

        ScanJobDispatcher dispatcherWithMockExecutor = new ScanJobDispatcher(
                scanPipelineService,
                scanJobRepository,
                repositoryRepository,
                scanWorkerInstance,
                mockExecutor
        );

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        ScanJobEntity dispatchedJob = txTemplate.execute(status -> dispatcherWithMockExecutor.dispatch(repositoryEntity, "main"));

        assertThat(dispatchedJob).isNotNull();

        // Verify the job row in DB was updated post-commit to FAILED
        ScanJobEntity persisted = scanJobRepository.findById(dispatchedJob.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo("FAILED");
        assertThat(persisted.getStage()).isEqualTo("FAILED");
        assertThat(persisted.getErrorMessage()).isEqualTo("Scan capacity exceeded, please retry later");
        assertThat(persisted.getErrorMessage()).doesNotContain("ghp_secretmarker1234567890");

        // Verify zero orphan QUEUED jobs remain
        List<ScanJobEntity> queuedJobs = scanJobRepository.findByStatusIn(List.of("QUEUED"));
        assertThat(queuedJobs).isEmpty();
    }

    @Test
    @DisplayName("Test 3: dispatch() prevents duplicate scans and returns existing active job")
    void testDuplicateTriggerReturnsExistingActiveJob() {
        ScanJobEntity activeJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repositoryEntity.getId())
                .branchName("main")
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("RUNNING")
                .stage("SCANNING_SECRETS")
                .createdAt(Instant.now().minusSeconds(10))
                .startedAt(Instant.now().minusSeconds(8))
                .heartbeatAt(Instant.now().minusSeconds(5))
                .workerInstanceId("existing-worker-1")
                .build());

        ScanJobEntity dispatchedJob = scanJobDispatcher.dispatch(repositoryEntity, "main");

        assertThat(dispatchedJob).isNotNull();
        assertThat(dispatchedJob.getId()).isEqualTo(activeJob.getId());
        assertThat(dispatchedJob.getStatus()).isEqualTo("RUNNING");
        assertThat(dispatchedJob.getStage()).isEqualTo("SCANNING_SECRETS");

        // Verify no second row was inserted
        List<ScanJobEntity> allJobs = scanJobRepository.findAll();
        assertThat(allJobs).hasSize(1);
    }

    @Test
    @DisplayName("Test 4: dispatch() is atomic under 10 concurrent threads: exactly 1 active job created, 9 threads receive same job ID")
    void testConcurrentDispatchReturnsSameJobAtomically() throws Exception {
        int threadCount = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<ScanJobEntity>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return scanJobDispatcher.dispatch(repositoryEntity, "main");
            }));
        }

        // Wait for all threads to be ready, then trigger concurrently
        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        List<ScanJobEntity> results = new java.util.ArrayList<>();
        for (java.util.concurrent.Future<ScanJobEntity> future : futures) {
            results.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        assertThat(results).hasSize(threadCount);
        UUID expectedJobId = results.get(0).getId();
        assertThat(expectedJobId).isNotNull();

        // All 10 threads received the exact same active job ID
        for (ScanJobEntity result : results) {
            assertThat(result.getId()).isEqualTo(expectedJobId);
            assertThat(result.getRepositoryId()).isEqualTo(repositoryEntity.getId());
        }

        // Exactly 1 job exists in the database
        List<ScanJobEntity> dbJobs = scanJobRepository.findAll();
        assertThat(dbJobs).hasSize(1);
        assertThat(dbJobs.get(0).getId()).isEqualTo(expectedJobId);
        assertThat(dbJobs.get(0).getStatus()).isIn("QUEUED", "RUNNING");
        assertThat(dbJobs.get(0).getWorkerInstanceId()).isEqualTo(scanWorkerInstance.getInstanceId());
        assertThat(dbJobs.get(0).getHeartbeatAt()).isNotNull();
    }
}
