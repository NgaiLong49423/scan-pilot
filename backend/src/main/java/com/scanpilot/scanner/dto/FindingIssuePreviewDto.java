package com.scanpilot.scanner.dto;

import java.util.UUID;

/**
 * DTO representing a reviewable GitHub issue draft preview with an expiring signed preview token.
 */
public record FindingIssuePreviewDto(
    UUID findingId,
    String title,
    String body,
    String previewToken,
    String linkState,
    boolean alreadyLinked,
    Integer existingIssueNumber,
    String existingIssueUrl
) {}
