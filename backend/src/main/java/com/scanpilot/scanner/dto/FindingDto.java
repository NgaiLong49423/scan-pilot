package com.scanpilot.scanner.dto;

import com.scanpilot.persistence.entity.FindingEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a security finding with severity, lifecycle state, remediation quality,
 * associated locations, and linked GitHub issue metadata.
 */
public record FindingDto(
    UUID id,
    UUID repositoryId,
    String ruleId,
    String fingerprint,
    String severity,
    String title,
    String description,
    String lifecycle,
    String remediationQuality,
    Instant firstSeenAt,
    Instant lastSeenAt,
    Instant resolvedAt,
    List<FindingLocationDto> locations,
    Integer githubIssueNumber,
    String githubIssueUrl,
    String issueLinkState
) {
    public static FindingDto from(FindingEntity entity, List<FindingLocationDto> locations) {
        return from(entity, locations, null, null, null);
    }

    public static FindingDto from(
        FindingEntity entity,
        List<FindingLocationDto> locations,
        Integer githubIssueNumber,
        String githubIssueUrl,
        String issueLinkState
    ) {
        if (entity == null) {
            return null;
        }
        return new FindingDto(
            entity.getId(),
            entity.getRepositoryId(),
            entity.getRuleId(),
            entity.getFingerprint(),
            entity.getSeverity(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getLifecycle(),
            entity.getRemediationQuality(),
            entity.getFirstSeenAt(),
            entity.getLastSeenAt(),
            entity.getResolvedAt(),
            locations != null ? locations : List.of(),
            githubIssueNumber,
            githubIssueUrl,
            issueLinkState
        );
    }
}
