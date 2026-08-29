package com.scanpilot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "webhook_deliveries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_webhook_deliveries_delivery_id", columnNames = {"delivery_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDeliveryEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "delivery_id", length = 128, nullable = false, unique = true)
    private String deliveryId;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(name = "repository_id")
    private UUID repositoryId;

    @Column(name = "github_repo_id")
    private Long githubRepoId;

    @Column(name = "installation_id")
    private Long installationId;

    @Column(name = "branch", length = 255)
    private String branch;

    @Column(name = "default_branch", length = 255)
    private String defaultBranch;

    @Column(name = "base_branch", length = 255)
    private String baseBranch;

    @Column(name = "head_branch", length = 255)
    private String headBranch;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "base_sha", length = 64)
    private String baseSha;

    @Column(name = "pr_number")
    private Integer prNumber;

    @Column(name = "pr_action", length = 64)
    private String prAction;

    @Builder.Default
    @Column(name = "is_fork", nullable = false)
    private Boolean isFork = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Builder.Default
    @Column(name = "is_merged", nullable = false)
    private Boolean isMerged = false;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "reason_code", length = 64, nullable = false)
    private String reasonCode;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
