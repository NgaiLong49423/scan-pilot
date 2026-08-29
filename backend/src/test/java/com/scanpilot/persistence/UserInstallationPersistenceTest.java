package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserInstallationEntity;
import com.scanpilot.persistence.repository.UserInstallationRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("UserInstallationPersistence Tests")
class UserInstallationPersistenceTest {

    @Autowired
    private UserInstallationRepository userInstallationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        long randomGithubId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        user = userRepository.save(UserEntity.builder()
                .githubUserId(randomGithubId)
                .login("test-user-" + randomGithubId)
                .name("Test User")
                .email("test@user.local")
                .createdAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("AC-03: Should persist and retrieve verified user installation association")
    void testPersistAndRetrieveUserInstallation() {
        Long installationId = 998811L;

        UserInstallationEntity entity = UserInstallationEntity.builder()
                .userId(user.getId())
                .githubUserId(user.getGithubUserId())
                .installationId(installationId)
                .accountLogin("test-org")
                .accountType("Organization")
                .verifiedAt(Instant.now())
                .build();

        UserInstallationEntity saved = userInstallationRepository.save(entity);
        assertThat(saved.getId()).isNotNull();

        Optional<UserInstallationEntity> retrieved = userInstallationRepository.findByUserIdAndInstallationId(user.getId(), installationId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAccountLogin()).isEqualTo("test-org");
    }

    @Test
    @DisplayName("AC-03: Should atomically upsert concurrently from multiple threads without exceptions and result in exactly 1 row")
    void testConcurrentCallbacksCreateSingleAssociation() throws Exception {
        Long installationId = 998822L;
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        List<Future<Integer>> futures = new ArrayList<>();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    return txTemplate.execute(status -> {
                        return userInstallationRepository.upsertUserInstallation(
                                UUID.randomUUID(),
                                user.getId(),
                                user.getGithubUserId(),
                                installationId,
                                "test-org-" + index,
                                "Organization",
                                Instant.now()
                        );
                    });
                } catch (Throwable t) {
                    errors.add(t);
                    throw t;
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(finished).isTrue();
        assertThat(errors).isEmpty();

        for (Future<Integer> f : futures) {
            Integer rowsAffected = f.get();
            assertThat(rowsAffected).isGreaterThanOrEqualTo(1);
        }

        List<UserInstallationEntity> all = userInstallationRepository.findByUserId(user.getId());
        List<UserInstallationEntity> matching = all.stream()
                .filter(u -> installationId.equals(u.getInstallationId()))
                .toList();

        assertThat(matching).hasSize(1);
        assertThat(matching.get(0).getUserId()).isEqualTo(user.getId());
        assertThat(matching.get(0).getInstallationId()).isEqualTo(installationId);
    }

    @Test
    @DisplayName("AC-03: Should enforce unique constraint uq_user_installations_user_inst on direct duplicate entity insertion")
    void testUniqueUserInstallationConstraint() {
        Long installationId = 998833L;
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        txTemplate.execute(status -> {
            userInstallationRepository.saveAndFlush(UserInstallationEntity.builder()
                    .userId(user.getId())
                    .githubUserId(user.getGithubUserId())
                    .installationId(installationId)
                    .accountLogin("org-a")
                    .accountType("Organization")
                    .verifiedAt(Instant.now())
                    .build());
            return null;
        });

        assertThatThrownBy(() -> {
            txTemplate.execute(status -> {
                userInstallationRepository.saveAndFlush(UserInstallationEntity.builder()
                        .userId(user.getId())
                        .githubUserId(user.getGithubUserId())
                        .installationId(installationId)
                        .accountLogin("org-b")
                        .accountType("Organization")
                        .verifiedAt(Instant.now())
                        .build());
                return null;
            });
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
