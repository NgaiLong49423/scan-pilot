package com.scanpilot.github.service;

import com.scanpilot.github.dto.GitHubWebhookPayloadDto;
import com.scanpilot.github.dto.WebhookDeliveryResponseDto;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.WebhookDeliveryEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.WebhookDeliveryRepository;
import com.scanpilot.scanner.config.ScanExecutorConfig;
import com.scanpilot.scanner.dispatcher.ScanJobDispatcher;
import com.scanpilot.scanner.dispatcher.ScanTriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubWebhookService {

    private static final Set<String> MONITORED_PR_ACTIONS = Set.of(
            "opened",
            "synchronize",
            "reopened",
            "closed"
    );

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final RepositoryRepository repositoryRepository;
    private final ScanJobRepository scanJobRepository;
    private final ScanJobDispatcher scanJobDispatcher;

    @Transactional
    public WebhookDeliveryResponseDto processWebhook(String deliveryId, String eventType, GitHubWebhookPayloadDto payload) {
        Instant now = Instant.now();
        UUID entityId = UUID.randomUUID();

        boolean inserted = webhookDeliveryRepository.insertIfAbsent(entityId, deliveryId, eventType, now);
        if (!inserted) {
            log.info("Duplicate webhook delivery ignored: deliveryId={}", deliveryId);
            return new WebhookDeliveryResponseDto(deliveryId, "IGNORED_DUPLICATE", "DUPLICATE_DELIVERY");
        }

        WebhookDeliveryEntity delivery = webhookDeliveryRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Webhook delivery record not found for deliveryId: " + deliveryId));

        if (payload != null) {
            delivery.setGithubRepoId(payload.githubRepoId());
            delivery.setInstallationId(payload.installationId());
            delivery.setBranch(payload.branch());
            delivery.setDefaultBranch(payload.defaultBranch());
            delivery.setBaseBranch(payload.baseBranch());
            delivery.setHeadBranch(payload.headBranch());
            delivery.setCommitSha(payload.commitSha());
            delivery.setBaseSha(payload.baseSha());
            delivery.setPrNumber(payload.prNumber());
            delivery.setPrAction(payload.prAction());
            delivery.setIsFork(Boolean.TRUE.equals(payload.isFork()));
            delivery.setIsDeleted(Boolean.TRUE.equals(payload.isDeleted()));
            delivery.setIsMerged(Boolean.TRUE.equals(payload.isMerged()));
        }
        delivery.setProcessedAt(now);

        // 1. Ping event
        if ("ping".equalsIgnoreCase(eventType)) {
            return updateAndSaveDelivery(delivery, null, "IGNORED_UNSUPPORTED_EVENT", "EVENT_PING_ACKNOWLEDGED");
        }

        // 2. Branch deletion on push
        if (payload != null && Boolean.TRUE.equals(payload.isDeleted())) {
            return updateAndSaveDelivery(delivery, null, "IGNORED_DELETED_REF", "BRANCH_DELETED");
        }

        // 3. Fork repository or PR
        if (payload != null && Boolean.TRUE.equals(payload.isFork())) {
            return updateAndSaveDelivery(delivery, null, "IGNORED_FORK", "FORK_NOT_SUPPORTED");
        }

        // 4. Pull request event filtering
        if ("pull_request".equalsIgnoreCase(eventType)) {
            String prAction = payload != null ? payload.prAction() : null;
            boolean isMerged = payload != null && Boolean.TRUE.equals(payload.isMerged());

            if ("closed".equalsIgnoreCase(prAction) && !isMerged) {
                return updateAndSaveDelivery(delivery, null, "IGNORED_UNSUPPORTED_EVENT", "PR_CLOSED_UNMERGED");
            }

            if (prAction == null || !MONITORED_PR_ACTIONS.contains(prAction.toLowerCase())) {
                return updateAndSaveDelivery(delivery, null, "IGNORED_UNSUPPORTED_EVENT", "ACTION_NOT_MONITORED");
            }
        } else if (!"push".equalsIgnoreCase(eventType)) {
            // Non-monitored event type
            return updateAndSaveDelivery(delivery, null, "IGNORED_UNSUPPORTED_EVENT", "EVENT_NOT_MONITORED");
        }

        // 5. Multi-Tenant Route Resolution
        if (payload == null || payload.githubRepoId() == null || payload.installationId() == null) {
            return updateAndSaveDelivery(delivery, null, "IGNORED_UNMONITORED", "REPOSITORY_NOT_MONITORED");
        }

        List<RepositoryEntity> matchedRepos = repositoryRepository.findByGithubRepoIdAndInstallationIdAndStatus(
                payload.githubRepoId(),
                payload.installationId(),
                "ACTIVE"
        );

        if (matchedRepos.isEmpty()) {
            return updateAndSaveDelivery(delivery, null, "IGNORED_UNMONITORED", "REPOSITORY_NOT_MONITORED");
        }

        if (matchedRepos.size() > 1) {
            log.warn("Multiple active repositories matched for githubRepoId={}, installationId={}",
                    payload.githubRepoId(), payload.installationId());
            return updateAndSaveDelivery(delivery, null, "IGNORED_AMBIGUOUS", "MULTIPLE_ACTIVE_REPOSITORIES_MATCHED");
        }

        RepositoryEntity matchedRepo = matchedRepos.get(0);
        UUID repoId = matchedRepo.getId();

        // 6. Pessimistic lock on repository row for queue capacity and dispatch consistency
        repositoryRepository.findByIdForUpdate(repoId);

        // Check per-repository queue capacity limit
        long queuedCount = scanJobRepository.countByRepositoryIdAndStatus(repoId, "QUEUED");
        if (queuedCount >= ScanExecutorConfig.MAX_QUEUED_JOBS_PER_REPOSITORY) {
            log.warn("Per-repository queue capacity full (count={}) for repoId={}", queuedCount, repoId);
            return updateAndSaveDelivery(delivery, repoId, "IGNORED_CAPACITY", "DISPATCH_QUEUE_FULL");
        }

        // 7. Determine trigger type, target branch, and expected commit SHA
        String triggerType;
        String targetBranch;
        String expectedCommitSha = delivery.getCommitSha();
        Integer prNumber = null;

        if ("pull_request".equalsIgnoreCase(eventType)) {
            if ("closed".equalsIgnoreCase(delivery.getPrAction()) && Boolean.TRUE.equals(delivery.getIsMerged())) {
                triggerType = ScanTriggerType.WEBHOOK_MERGE.name();
                targetBranch = delivery.getBaseBranch();
            } else {
                triggerType = ScanTriggerType.WEBHOOK_PULL_REQUEST.name();
                targetBranch = delivery.getHeadBranch();
            }
            prNumber = delivery.getPrNumber();
        } else {
            triggerType = ScanTriggerType.WEBHOOK_PUSH.name();
            targetBranch = delivery.getBranch();
        }

        // Validate target branch: fail-closed for invalid refs (R54-B3-01, R54-B3-02)
        if (!isValidGitRef(targetBranch)) {
            return updateAndSaveDelivery(delivery, repoId, "IGNORED_INVALID_REF", "TARGET_REF_INVALID");
        }

        // Validate commit SHA: must be valid non-blank 40-hex SHA verified in BUILD 2 (R54-B3-01)
        if (expectedCommitSha == null || !expectedCommitSha.matches("^[0-9a-fA-F]{40}$")) {
            return updateAndSaveDelivery(delivery, repoId, "IGNORED_INVALID_COMMIT_SHA", "COMMIT_SHA_INVALID");
        }

        // 8. Provision queued ScanJobEntity linked to delivery
        ScanJobEntity scanJob = ScanJobEntity.builder()
                .repositoryId(repoId)
                .branchName(targetBranch.trim())
                .scanMode("SNAPSHOT_AND_HISTORY")
                .status("QUEUED")
                .stage("QUEUED")
                .triggerType(triggerType)
                .webhookDeliveryId(delivery.getId())
                .expectedCommitSha(expectedCommitSha)
                .prNumber(prNumber)
                .createdAt(now)
                .updatedAt(now)
                .heartbeatAt(now)
                .build();

        scanJobRepository.saveAndFlush(scanJob);

        // 9. Register post-commit trigger on ScanJobDispatcher
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scanJobDispatcher.tryProcessNextJobForRepository(repoId);
                }
            });
        } else {
            scanJobDispatcher.tryProcessNextJobForRepository(repoId);
        }

        return updateAndSaveDelivery(delivery, repoId, "ACCEPTED", "ROUTED_ACTIVE_MONITORED_REPOSITORY");
    }

    private WebhookDeliveryResponseDto updateAndSaveDelivery(
            WebhookDeliveryEntity delivery,
            UUID repositoryId,
            String status,
            String reasonCode
    ) {
        delivery.setRepositoryId(repositoryId);
        delivery.setStatus(status);
        delivery.setReasonCode(reasonCode);
        delivery.setProcessedAt(Instant.now());
        webhookDeliveryRepository.save(delivery);
        return new WebhookDeliveryResponseDto(delivery.getDeliveryId(), status, reasonCode);
    }

    /**
     * Validates that a branch or ref adheres to Git and GitHub ref formatting rules (R54-B3-02).
     * Rejects control chars, whitespace, backslashes, "..", "@{", "//", leading/trailing "/",
     * leading/trailing ".", ".lock", and forbidden Git characters (~, ^, :, ?, *, [, ]).
     */
    public static boolean isValidGitRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return false;
        }
        for (int i = 0; i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (c <= 31 || c == 127 || Character.isWhitespace(c)) {
                return false;
            }
            if (c == '\\' || c == '~' || c == '^' || c == ':' || c == '?' || c == '*' || c == '[' || c == ']') {
                return false;
            }
        }
        if (ref.contains("..") || ref.contains("//") || ref.contains("@{")) {
            return false;
        }
        if (ref.startsWith("/") || ref.endsWith("/") || ref.startsWith(".") || ref.endsWith(".")) {
            return false;
        }
        if (ref.endsWith(".lock")) {
            return false;
        }
        String[] segments = ref.split("/");
        for (String segment : segments) {
            if (segment.isEmpty() || segment.startsWith(".") || segment.endsWith(".lock") || "@".equals(segment)) {
                return false;
            }
        }
        return true;
    }
}
