package com.scanpilot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "finding_remediation_pr_links",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_remediation_pr_finding_revision", columnNames = {"finding_id", "source_revision_commit"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FindingRemediationPrLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "source_revision_commit", length = 64, nullable = false)
    private String sourceRevisionCommit;

    @Column(name = "target_branch", length = 255, nullable = false)
    private String targetBranch;

    @Column(name = "head_branch", length = 255, nullable = false)
    private String headBranch;

    @Column(name = "state", length = 32, nullable = false)
    private String state;

    @Column(name = "github_pr_number")
    private Integer githubPrNumber;

    @Column(name = "github_pr_url", length = 1024)
    private String githubPrUrl;

    @Column(name = "idempotency_marker", length = 128, nullable = false)
    private String idempotencyMarker;

    @Column(name = "failure_reason", length = 64)
    private String failureReason;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}