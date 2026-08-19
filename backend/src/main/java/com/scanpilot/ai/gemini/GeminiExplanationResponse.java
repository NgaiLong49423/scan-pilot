package com.scanpilot.ai.gemini;

import java.util.List;

/**
 * Structured security explanation and remediation guidance response.
 */
public record GeminiExplanationResponse(
    String summary,
    String riskImpact,
    String evidenceLimits,
    List<String> remediationSteps,
    String remediationDiff,
    String revocationCommandHint,
    String sourceAttribution
) {}
