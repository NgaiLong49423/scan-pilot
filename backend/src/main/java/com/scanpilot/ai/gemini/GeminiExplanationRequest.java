package com.scanpilot.ai.gemini;

import java.util.UUID;

/**
 * Request payload containing secret-redacted evidence context for AI explanation.
 */
public record GeminiExplanationRequest(
    UUID findingId,
    UUID repositoryId,
    String ruleId,
    String filePath,
    String lineRange,
    String maskedSecret,
    String redactedSnippet,
    String lifecycle,
    String remediationQuality
) {}
