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
    name = "findings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_findings_repo_fingerprint", columnNames = {"repository_id", "fingerprint"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "rule_id", length = 255)
    private String ruleId;

    @Column(name = "fingerprint", length = 64, nullable = false)
    private String fingerprint;

    @Column(name = "severity", length = 64)
    private String severity;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lifecycle", length = 64)
    private String lifecycle;

    @Column(name = "remediation_quality", length = 64)
    private String remediationQuality;

    @Column(name = "first_seen_at")
    private Instant firstSeenAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
