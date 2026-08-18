package com.scanpilot.scanner.dto;

import com.scanpilot.persistence.entity.FindingEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a security finding with severity, lifecycle state, remediation quality,
 * and associated locations.
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
    List<FindingLocationDto> locations
) {
    public static FindingDto from(FindingEntity entity, List<FindingLocationDto> locations) {
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
            locations != null ? locations : List.of()
        );
    }
}
