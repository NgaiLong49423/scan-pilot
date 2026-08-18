package com.scanpilot.scanner.detector.gitleaks;

import com.scanpilot.security.secret.RedactedEvidence;

/**
 * Normalized and safe secret finding containing redacted evidence and non-sensitive metadata.
 * Safe for persistence, logging, and downstream processing.
 */
public record DetectedSecretFinding(
    String ruleId,
    String file,
    int startLine,
    int endLine,
    int startColumn,
    int endColumn,
    String commit,
    String author,
    String date,
    RedactedEvidence redactedEvidence
) {}
