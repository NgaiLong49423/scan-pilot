package com.scanpilot.scanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for confirming and creating a GitHub Remediation Pull Request from a previewed finding.
 * The client strictly sends only the signed preview token. Extra properties cause a Bad Request (400).
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateFindingRemediationPrRequest(
    @NotBlank(message = "previewToken must not be blank")
    String previewToken
) {}