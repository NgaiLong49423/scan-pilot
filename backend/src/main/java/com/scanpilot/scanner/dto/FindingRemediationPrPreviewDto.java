package com.scanpilot.scanner.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for secret-safe Remediation Pull Request preview with masked diff and signed token.
 */
public record FindingRemediationPrPreviewDto(
    UUID findingId,
    UUID repositoryId,
    String filePath,
    int lineNumber,
    String targetCommitSha,
    String targetBranch,
    String remediationBranchName,
    String originalLineMasked,
    String patchedLine,
    String envVariableName,
    String previewToken,
    Instant expiresAt,
    String revocationWarning,
    boolean alreadyLinked,
    Integer existingPrNumber,
    String existingPrUrl,
    String linkState
) {}