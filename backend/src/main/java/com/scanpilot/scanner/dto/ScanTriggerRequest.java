package com.scanpilot.scanner.dto;

import java.util.UUID;

/**
 * Request payload to trigger a repository scan.
 */
public record ScanTriggerRequest(
    UUID repositoryId,
    String branchName,
    String sourcePath
) {}
