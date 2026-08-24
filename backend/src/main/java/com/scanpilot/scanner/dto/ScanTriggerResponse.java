package com.scanpilot.scanner.dto;

import java.util.UUID;

/**
 * Response returned after triggering a repository scan.
 */
public record ScanTriggerResponse(
    UUID jobId,
    UUID repositoryId,
    String branchName,
    String status,
    String stage,
    String message
) {
    public ScanTriggerResponse(UUID jobId, UUID repositoryId, String branchName, String status, String message) {
        this(jobId, repositoryId, branchName, status, status, message);
    }
}
