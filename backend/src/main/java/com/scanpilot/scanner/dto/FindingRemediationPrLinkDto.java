package com.scanpilot.scanner.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing the persisted state of a Finding to GitHub Remediation PR link.
 */
public record FindingRemediationPrLinkDto(
    UUID id,
    UUID findingId,
    UUID repositoryId,
    String sourceRevisionCommit,
    String targetBranch,
    String headBranch,
    String state,
    Integer githubPrNumber,
    String githubPrUrl,
    String idempotencyMarker,
    String failureReason,
    Instant createdAt,
    Instant updatedAt
) {}