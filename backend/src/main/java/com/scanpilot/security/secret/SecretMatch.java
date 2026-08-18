package com.scanpilot.security.secret;

/**
 * Represents raw secret match details extracted during detection before normalization and redaction.
 * Temporary sensitive record maintained strictly within the trusted normalization boundary.
 */
public record SecretMatch(
    String rawSecret,
    String ruleId,
    int startLine,
    int endLine,
    int startColumn,
    int endColumn,
    String snippet
) {}
