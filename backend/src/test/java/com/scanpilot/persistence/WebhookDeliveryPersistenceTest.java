package com.scanpilot.persistence;

import com.scanpilot.github.dto.GitHubWebhookPayloadDto;
import com.scanpilot.github.service.GitHubWebhookService;
import com.scanpilot.persistence.entity.WebhookDeliveryEntity;
import com.scanpilot.persistence.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("WebhookDeliveryPersistence & Concurrency Tests")
class WebhookDeliveryPersistenceTest {

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Autowired
    private GitHubWebhookService gitHubWebhookService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        webhookDeliveryRepository.deleteAll();
    }

    @Test
    @DisplayName("Should ensure exactly 1 successful insert among concurrent executions on H2")
    void testConcurrentDeliveriesInsertSingleRecordOnH2() throws Exception {
        int threadCount = 4;
        String deliveryId = UUID.randomUUID().toString();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                startLatch.await();
                boolean result = webhookDeliveryRepository.insertIfAbsent(
                        UUID.randomUUID(),
                        deliveryId,
                        "push",
                        Instant.now()
                );
                if (result) {
                    successCount.incrementAndGet();
                }
                return result;
            });
        }

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(executor.submit(task));
        }

        startLatch.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(webhookDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should leave zero dangling PROCESSING records when transaction rolls back")
    void testRollbackOnFailureLeavesZeroDanglingProcessingRows() {
        String deliveryId = UUID.randomUUID().toString();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> {
            txTemplate.execute(status -> {
                webhookDeliveryRepository.insertIfAbsent(
                        UUID.randomUUID(),
                        deliveryId,
                        "push",
                        Instant.now()
                );
                // Simulate runtime failure before completion
                throw new RuntimeException("Forced database rollback simulation");
            });
        }).isInstanceOf(RuntimeException.class).hasMessageContaining("Forced database rollback");

        assertThat(webhookDeliveryRepository.findByDeliveryId(deliveryId)).isEmpty();
        assertThat(webhookDeliveryRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Direct entity inspection confirms 0 raw JSON, 0 secrets, and 0 stack traces in database columns")
    void testZeroRawJsonOrSecretInDatabase() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(67890L)
                .branch("main")
                .defaultBranch("main")
                .commitSha("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .build();

        gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();

        // Inspect all fields to ensure no JSON blob or unapproved column exists
        for (Field field : WebhookDeliveryEntity.class.getDeclaredFields()) {
            field.setAccessible(true);
            Object val = field.get(entity);
            if (val instanceof String s) {
                assertThat(s).doesNotContain("{");
                assertThat(s).doesNotContain("}");
                assertThat(s).doesNotContain("ghp_");
                assertThat(s).doesNotContain("Exception");
            }
        }
    }
}
