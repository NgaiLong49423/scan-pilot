package com.scanpilot.scanner.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for confirming and creating a GitHub issue from a previewed finding.
 * The client only sends the signed preview token.
 */
public record CreateFindingIssueRequest(
    @NotBlank(message = "previewToken must not be blank")
    String previewToken
) {}
