package com.scanpilot.github.service;

import com.scanpilot.github.dto.GitHubWebhookPayloadDto;
import com.scanpilot.github.dto.WebhookDeliveryResponseDto;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.WebhookDeliveryEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return updateAndSaveDelivery(delivery, matchedRepo.getId(), "ACCEPTED", "ROUTED_ACTIVE_MONITORED_REPOSITORY");
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
}
