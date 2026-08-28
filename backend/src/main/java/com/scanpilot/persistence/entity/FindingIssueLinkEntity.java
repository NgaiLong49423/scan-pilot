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
    name = "finding_issue_links",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_issue_links_finding", columnNames = {"finding_id"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FindingIssueLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "state", length = 32, nullable = false)
    private String state;

    @Column(name = "github_issue_number")
    private Integer githubIssueNumber;

    @Column(name = "github_issue_url", length = 1024)
    private String githubIssueUrl;

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
