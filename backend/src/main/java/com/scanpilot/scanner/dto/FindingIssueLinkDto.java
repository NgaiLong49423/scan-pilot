package com.scanpilot.scanner.dto;

import com.scanpilot.persistence.entity.FindingIssueLinkEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing the persisted linkage between a finding and a GitHub issue.
 */
public record FindingIssueLinkDto(
    UUID id,
    UUID findingId,
    UUID repositoryId,
    String state,
    Integer githubIssueNumber,
    String githubIssueUrl,
    Instant createdAt
) {
    public static FindingIssueLinkDto from(FindingIssueLinkEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FindingIssueLinkDto(
            entity.getId(),
            entity.getFindingId(),
            entity.getRepositoryId(),
            entity.getState(),
            entity.getGithubIssueNumber(),
            entity.getGithubIssueUrl(),
            entity.getCreatedAt()
        );
    }
}
