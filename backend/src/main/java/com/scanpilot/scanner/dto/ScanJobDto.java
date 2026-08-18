package com.scanpilot.scanner.dto;

import com.scanpilot.persistence.entity.ScanJobEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing scan job metadata, status, and telemetry.
 */
public record ScanJobDto(
    UUID id,
    UUID repositoryId,
    String branchName,
    String scanMode,
    String status,
    String commitSha,
    Long durationMs,
    String errorMessage,
    Instant startedAt,
    Instant completedAt
) {
    public static ScanJobDto from(ScanJobEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ScanJobDto(
            entity.getId(),
            entity.getRepositoryId(),
            entity.getBranchName(),
            entity.getScanMode(),
            entity.getStatus(),
            entity.getCommitSha(),
            entity.getDurationMs(),
            entity.getErrorMessage(),
            entity.getStartedAt(),
            entity.getCompletedAt()
        );
    }
}
