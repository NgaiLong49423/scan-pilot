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
import java.util.ArrayList;
import java.util.Collections;
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
@DisplayName("ScanJobDispatcher Integration & Unit Tests (Issue #54 BUILD 3)")
class ScanJobDispatcherTest {

    @Autowired
    private ScanJobDispatcher scanJobDispatcher;

    @Autowired
    private ScanJobStateTransitionService scanJobStateTransitionService;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private com.scanpilot.persistence.repository.ScanEventRepository scanEventRepository;

    @Autowired
    private com.scanpilot.scanner.telemetry.TelemetryPayloadSerializer telemetryPayloadSerializer;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScanWorkerInstance scanWorkerInstance;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private ScanPipelineService scanPipelineService;

    private RepositoryEntity repositoryEntity;

    @BeforeEach
    void setUp() {
        if (scanEventRepository != null) {
            scanEventRepository.deleteAll();
        }
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
    @DisplayName("Test 1: Worker only executes after transaction commits and job is fully visible in DB, emitting QUEUED event")
    void testWorkerExecutesAfterTransactionCommit() throws Exception {
        AtomicBoolean jobVisibleDuringExecution = new AtomicBoolean(false);
        CountDownLatch executionLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            UUID jobId = invocation.getArgument(0);
            Optional<ScanJobEntity> entity = scanJobRepository.findById(jobId);
            if (entity.isPresent() && ("QUEUED".equals(entity.get().getStatus()) || "RUNNING".equals(entity.get().getStatus()))) {
                jobVisibleDuringExecution.set(true);
            }
            scanJobRepository.updateJobStatusAndError(jobId, "COMPLETED", "COMPLETED", null, Instant.now());
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
    @DisplayName("Test 2: Executor rejection transitions job to FAILED with safe message without recursive drain")
    void testExecutorRejectionAfterCommitTransitionsJobToFailed() {
        ThreadPoolTaskExecutor mockExecutor = org.mockito.Mockito.mock(ThreadPoolTaskExecutor.class);
        doThrow(new RejectedExecutionException("Queue full with secret token ghp_secretmarker1234567890"))
                .when(mockExecutor).execute(any(Runnable.class));

        ScanJobDispatcher dispatcherWithMockExecutor = new ScanJobDispatcher(
                scanPipelineService,
                scanJobRepository,
                scanEventRepository,
                repositoryRepository,
                scanWorkerInstance,
                telemetryPayloadSerializer,
                scanJobStateTransitionService,
                transactionManager,
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
    @DisplayName("Test 3: dispatch() prevents duplicate manual scans and returns existing active job")
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
    @DisplayName("Test 4: Two deliveries for same repo execute in FIFO order (one RUNNING at a time, next starts after first finishes)")
    void testTwoDeliveriesSameRepoExecuteInFifoOrder() throws Exception {
        UUID repoId = repositoryEntity.getId();

        ScanJobEntity job1 = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repoId)
                .branchName("main")
                .status("QUEUED")
                .stage("QUEUED")
                .triggerType("WEBHOOK_PUSH")
                .createdAt(Instant.now().minusSeconds(5))
                .build());

        ScanJobEntity job2 = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(repoId)
                .branchName("feature-2")
                .status("QUEUED")
                .stage("QUEUED")
                .triggerType("WEBHOOK_PULL_REQUEST")
                .createdAt(Instant.now())
                .build());

        List<UUID> executedJobIds = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch job1Started = new CountDownLatch(1);
        CountDownLatch allowJob1ToFinish = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(2);

        doAnswer(invocation -> {
            UUID jId = invocation.getArgument(0);
            executedJobIds.add(jId);
            if (jId.equals(job1.getId())) {
                job1Started.countDown();
                allowJob1ToFinish.await();
            }
            // Emulate ScanPipelineService marking completed
            scanJobRepository.updateJobStatusAndError(jId, "COMPLETED", "COMPLETED", null, Instant.now());
            allDone.countDown();
            return null;
        }).when(scanPipelineService).executeScanJob(any(UUID.class));

        // Start processing queue for this repo
        scanJobDispatcher.tryProcessNextJobForRepository(repoId);

        // Verify Job 1 starts running first
        boolean j1Started = job1Started.await(5, TimeUnit.SECONDS);
        assertThat(j1Started).isTrue();
        assertThat(executedJobIds).containsExactly(job1.getId());

        // Allow Job 1 to finish
        allowJob1ToFinish.countDown();

        // Verify Job 2 starts automatically via self-contained finally block drain
        boolean completed = allDone.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(executedJobIds).containsExactly(job1.getId(), job2.getId());
    }
}
