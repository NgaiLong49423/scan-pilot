package com.scanpilot.github.service;

import com.scanpilot.github.dto.GitHubWebhookPayloadDto;
import com.scanpilot.github.dto.WebhookDeliveryResponseDto;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.WebhookDeliveryEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.persistence.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("GitHubWebhookService Integration Tests")
class GitHubWebhookServiceTest {

    @Autowired
    private GitHubWebhookService gitHubWebhookService;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.scanpilot.persistence.repository.ScanJobRepository scanJobRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        long uniqueGithubUserId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        testUser = userRepository.save(UserEntity.builder()
                .githubUserId(uniqueGithubUserId)
                .login("test-owner-" + uniqueGithubUserId)
                .name("Test Owner")
                .email("owner@test.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Should resolve unmonitored repository to IGNORED_UNMONITORED and preserve payload identifiers")
    void testUnmonitoredRepoResolvesToIgnoredUnmonitored() {
        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(999999L)
                .installationId(888888L)
                .branch("main")
                .defaultBranch("main")
                .commitSha("0123456789abcdef0123456789abcdef01234567")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_UNMONITORED");
        assertThat(response.reason()).isEqualTo("REPOSITORY_NOT_MONITORED");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("IGNORED_UNMONITORED");
        assertThat(entity.getReasonCode()).isEqualTo("REPOSITORY_NOT_MONITORED");
        assertThat(entity.getGithubRepoId()).isEqualTo(999999L);
        assertThat(entity.getInstallationId()).isEqualTo(888888L);
        assertThat(entity.getRepositoryId()).isNull();
    }

    @Test
    @DisplayName("Should resolve ambiguous repository match to IGNORED_AMBIGUOUS without fan-out")
    void testMultipleActiveMatchesOfSameTupleResolvesToIgnoredAmbiguous() {
        long uniqueGithubUserId2 = Math.abs(UUID.randomUUID().getMostSignificantBits());
        UserEntity user2 = userRepository.save(UserEntity.builder()
                .githubUserId(uniqueGithubUserId2)
                .login("test-owner-2-" + uniqueGithubUserId2)
                .createdAt(Instant.now())
                .build());

        repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo-1")
                .status("ACTIVE")
                .build());

        repositoryRepository.save(RepositoryEntity.builder()
                .userId(user2.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo-2")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("main")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_AMBIGUOUS");
        assertThat(response.reason()).isEqualTo("MULTIPLE_ACTIVE_REPOSITORIES_MATCHED");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("IGNORED_AMBIGUOUS");
        assertThat(entity.getRepositoryId()).isNull();
    }

    @Test
    @DisplayName("Should not ambiguate when same githubRepoId exists under different installationId")
    void testSameGithubRepoIdUnderDifferentInstallationDoesNotAmbiguate() {
        RepositoryEntity repoTarget = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("target/repo")
                .status("ACTIVE")
                .build());

        long uniqueGithubUserId3 = Math.abs(UUID.randomUUID().getMostSignificantBits());
        UserEntity otherUser = userRepository.save(UserEntity.builder()
                .githubUserId(uniqueGithubUserId3)
                .login("other-owner-" + uniqueGithubUserId3)
                .createdAt(Instant.now())
                .build());

        repositoryRepository.save(RepositoryEntity.builder()
                .userId(otherUser.getId())
                .githubRepoId(12345L)
                .installationId(99999L)
                .fullName("other/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("main")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.reason()).isEqualTo("ROUTED_ACTIVE_MONITORED_REPOSITORY");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("ACCEPTED");
        assertThat(entity.getRepositoryId()).isEqualTo(repoTarget.getId());
    }

    @Test
    @DisplayName("Should resolve branch deletion on push to IGNORED_DELETED_REF")
    void testBranchDeletedResolvesToIgnoredDeletedRef() {
        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("feature/old")
                .isDeleted(true)
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_DELETED_REF");
        assertThat(response.reason()).isEqualTo("BRANCH_DELETED");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("IGNORED_DELETED_REF");
        assertThat(entity.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should resolve fork event to IGNORED_FORK")
    void testForkRepoResolvesToIgnoredFork() {
        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("main")
                .isFork(true)
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_FORK");
        assertThat(response.reason()).isEqualTo("FORK_NOT_SUPPORTED");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("IGNORED_FORK");
        assertThat(entity.getIsFork()).isTrue();
    }

    @Test
    @DisplayName("Should resolve PR closed unmerged to IGNORED_UNSUPPORTED_EVENT (PR_CLOSED_UNMERGED)")
    void testPrClosedUnmergedResolvesToIgnored() {
        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .prAction("closed")
                .isMerged(false)
                .prNumber(42)
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "pull_request", payload);

        assertThat(response.status()).isEqualTo("IGNORED_UNSUPPORTED_EVENT");
        assertThat(response.reason()).isEqualTo("PR_CLOSED_UNMERGED");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("IGNORED_UNSUPPORTED_EVENT");
        assertThat(entity.getReasonCode()).isEqualTo("PR_CLOSED_UNMERGED");
        assertThat(entity.getIsMerged()).isFalse();
    }

    @Test
    @DisplayName("Should resolve PR closed merged to ACCEPTED with is_merged true")
    void testPrClosedMergedTrueResolvesToAccepted() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .prAction("closed")
                .isMerged(true)
                .prNumber(42)
                .baseBranch("main")
                .headBranch("feature/auth")
                .commitSha("1111111111111111111111111111111111111111")
                .baseSha("2222222222222222222222222222222222222222")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "pull_request", payload);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.reason()).isEqualTo("ROUTED_ACTIVE_MONITORED_REPOSITORY");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("ACCEPTED");
        assertThat(entity.getRepositoryId()).isEqualTo(repo.getId());
        assertThat(entity.getIsMerged()).isTrue();
    }

    @Test
    @DisplayName("Should resolve supported push event to ACCEPTED with default branch populated")
    void testSupportedPushEventResolvesToAccepted() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("main")
                .defaultBranch("main")
                .commitSha("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .baseSha("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.reason()).isEqualTo("ROUTED_ACTIVE_MONITORED_REPOSITORY");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("ACCEPTED");
        assertThat(entity.getRepositoryId()).isEqualTo(repo.getId());
        assertThat(entity.getDefaultBranch()).isEqualTo("main");
    }

    @Test
    @DisplayName("Should resolve supported PR event to ACCEPTED with prNumber, SHAs and branches populated")
    void testSupportedPrEventResolvesToAccepted() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .prAction("opened")
                .prNumber(101)
                .baseBranch("main")
                .headBranch("feature/scanner")
                .commitSha("cccccccccccccccccccccccccccccccccccccccc")
                .baseSha("dddddddddddddddddddddddddddddddddddddddd")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "pull_request", payload);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.reason()).isEqualTo("ROUTED_ACTIVE_MONITORED_REPOSITORY");

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("ACCEPTED");
        assertThat(entity.getRepositoryId()).isEqualTo(repo.getId());
        assertThat(entity.getPrNumber()).isEqualTo(101);
        assertThat(entity.getPrAction()).isEqualTo("opened");
        assertThat(entity.getBaseBranch()).isEqualTo("main");
        assertThat(entity.getHeadBranch()).isEqualTo("feature/scanner");
        assertThat(entity.getCommitSha()).isEqualTo("cccccccccccccccccccccccccccccccccccccccc");
        assertThat(entity.getBaseSha()).isEqualTo("dddddddddddddddddddddddddddddddddddddddd");
    }

    @Test
    @DisplayName("Should detect duplicate delivery GUID and return IGNORED_DUPLICATE")
    void testDuplicateDeliveryIgnored() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("main")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto first = gitHubWebhookService.processWebhook(deliveryId, "push", payload);
        assertThat(first.status()).isEqualTo("ACCEPTED");

        WebhookDeliveryResponseDto duplicate = gitHubWebhookService.processWebhook(deliveryId, "push", payload);
        assertThat(duplicate.status()).isEqualTo("IGNORED_DUPLICATE");
        assertThat(duplicate.reason()).isEqualTo("DUPLICATE_DELIVERY");
    }

    @Test
    @DisplayName("Should provision QUEUED ScanJobEntity with trigger provenance when webhook is ACCEPTED")
    void testAcceptedDeliveryProvisionsScanJobAndEnqueuesAsync() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("feature/auth")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);
        assertThat(response.status()).isEqualTo("ACCEPTED");

        WebhookDeliveryEntity delivery = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(delivery.getRepositoryId()).isEqualTo(repo.getId());

        com.scanpilot.persistence.entity.ScanJobEntity job = scanJobRepository.findByWebhookDeliveryId(delivery.getId()).orElseThrow();
        assertThat(job.getRepositoryId()).isEqualTo(repo.getId());
        assertThat(job.getBranchName()).isEqualTo("feature/auth");
        assertThat(job.getTriggerType()).isEqualTo("WEBHOOK_PUSH");
        assertThat(job.getExpectedCommitSha()).isEqualTo("1111222233334444555566667777888899990000");
        assertThat(job.getStatus()).isEqualTo("QUEUED");
        assertThat(job.getStage()).isEqualTo("QUEUED");
    }

    @Test
    @DisplayName("Should return IGNORED_CAPACITY / DISPATCH_QUEUE_FULL when repository queue already contains 10 QUEUED jobs")
    void testQueueFullRejectsWithDispatchQueueFullOutcome() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        // Fill queue to capacity (10 jobs)
        for (int i = 0; i < 10; i++) {
            scanJobRepository.save(com.scanpilot.persistence.entity.ScanJobEntity.builder()
                    .repositoryId(repo.getId())
                    .branchName("main")
                    .status("QUEUED")
                    .stage("QUEUED")
                    .triggerType("WEBHOOK_PUSH")
                    .createdAt(Instant.now().minusSeconds(10 - i))
                    .build());
        }

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("feature/11th")
                .commitSha("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_CAPACITY");
        assertThat(response.reason()).isEqualTo("DISPATCH_QUEUE_FULL");

        WebhookDeliveryEntity delivery = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("IGNORED_CAPACITY");
        assertThat(delivery.getReasonCode()).isEqualTo("DISPATCH_QUEUE_FULL");

        // Assert 11th ScanJobEntity was NOT created
        assertThat(scanJobRepository.findByWebhookDeliveryId(delivery.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should return IGNORED_INVALID_REF / TARGET_REF_INVALID and create zero ScanJob when push has blank branch (R54-B3-01)")
    void testPushWithBlankBranchRejectsFailClosedWithoutFallbackToMain() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("   ")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(response.reason()).isEqualTo("TARGET_REF_INVALID");

        WebhookDeliveryEntity delivery = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(delivery.getReasonCode()).isEqualTo("TARGET_REF_INVALID");
        assertThat(scanJobRepository.findByWebhookDeliveryId(delivery.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should return IGNORED_INVALID_REF / TARGET_REF_INVALID and create zero ScanJob when PR has null headBranch (R54-B3-01)")
    void testPrWithNullHeadBranchRejectsFailClosed() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .prAction("opened")
                .prNumber(10)
                .headBranch(null)
                .baseBranch("main")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "pull_request", payload);

        assertThat(response.status()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(response.reason()).isEqualTo("TARGET_REF_INVALID");

        WebhookDeliveryEntity delivery = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(scanJobRepository.findByWebhookDeliveryId(delivery.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should return IGNORED_INVALID_REF / TARGET_REF_INVALID and create zero ScanJob when merged PR has blank baseBranch (R54-B3-01)")
    void testMergedPrWithBlankBaseBranchRejectsFailClosed() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .prAction("closed")
                .isMerged(true)
                .prNumber(10)
                .headBranch("feature")
                .baseBranch("  ")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "pull_request", payload);

        assertThat(response.status()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(response.reason()).isEqualTo("TARGET_REF_INVALID");

        WebhookDeliveryEntity delivery = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(scanJobRepository.findByWebhookDeliveryId(delivery.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should return IGNORED_INVALID_COMMIT_SHA / COMMIT_SHA_INVALID when commit SHA is missing or invalid (R54-B3-01)")
    void testMissingOrInvalidCommitShaRejectsFailClosed() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("feature")
                .commitSha("invalid-short-sha")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_INVALID_COMMIT_SHA");
        assertThat(response.reason()).isEqualTo("COMMIT_SHA_INVALID");

        WebhookDeliveryEntity delivery = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("IGNORED_INVALID_COMMIT_SHA");
        assertThat(delivery.getReasonCode()).isEqualTo("COMMIT_SHA_INVALID");
        assertThat(scanJobRepository.findByWebhookDeliveryId(delivery.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should return IGNORED_INVALID_REF for path traversal branch '../main' (R54-B3-02)")
    void testPathTraversalBranchRejectsFailClosed() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("../main")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(response.reason()).isEqualTo("TARGET_REF_INVALID");
        assertThat(scanJobRepository.countByRepositoryIdAndStatus(repo.getId(), "QUEUED")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return IGNORED_INVALID_REF for double-dot branch 'main..evil' (R54-B3-02)")
    void testDoubleDotBranchRejectsFailClosed() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("main..evil")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(response.reason()).isEqualTo("TARGET_REF_INVALID");
        assertThat(scanJobRepository.countByRepositoryIdAndStatus(repo.getId(), "QUEUED")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return IGNORED_INVALID_REF for backslash branch 'refs\\heads\\main' (R54-B3-02)")
    void testBackslashBranchRejectsFailClosed() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("refs\\heads\\main")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(response.reason()).isEqualTo("TARGET_REF_INVALID");
        assertThat(scanJobRepository.countByRepositoryIdAndStatus(repo.getId(), "QUEUED")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return IGNORED_INVALID_REF for whitespace-containing branch 'main branch' (R54-B3-02)")
    void testWhitespaceBranchRejectsFailClosed() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("main branch")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("IGNORED_INVALID_REF");
        assertThat(response.reason()).isEqualTo("TARGET_REF_INVALID");
        assertThat(scanJobRepository.countByRepositoryIdAndStatus(repo.getId(), "QUEUED")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should accept valid multi-slash branch 'feature/security/fix' and create ScanJob (R54-B3-02)")
    void testValidMultiSlashBranchAccepted() {
        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(12345L)
                .installationId(54321L)
                .fullName("owner/repo")
                .status("ACTIVE")
                .build());

        String deliveryId = UUID.randomUUID().toString();
        GitHubWebhookPayloadDto payload = GitHubWebhookPayloadDto.builder()
                .githubRepoId(12345L)
                .installationId(54321L)
                .branch("feature/security/fix")
                .commitSha("1111222233334444555566667777888899990000")
                .build();

        WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(deliveryId, "push", payload);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.reason()).isEqualTo("ROUTED_ACTIVE_MONITORED_REPOSITORY");

        WebhookDeliveryEntity delivery = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("ACCEPTED");

        com.scanpilot.persistence.entity.ScanJobEntity job = scanJobRepository.findByWebhookDeliveryId(delivery.getId()).orElseThrow();
        assertThat(job.getBranchName()).isEqualTo("feature/security/fix");
        assertThat(job.getStatus()).isEqualTo("QUEUED");
    }
}
