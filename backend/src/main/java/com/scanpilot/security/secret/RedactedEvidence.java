package com.scanpilot.security.secret;

/**
 * Safe, normalized evidence record containing masked secrets and HMAC-SHA-256 fingerprints.
 * Safe for persistence, logging, and downstream processing.
 */
public record RedactedEvidence(
    String maskedSecret,
    String redactedSnippet,
    String fingerprint,
    String ruleId,
    int startLine,
    int endLine,
    int startColumn,
    int endColumn
) {}
