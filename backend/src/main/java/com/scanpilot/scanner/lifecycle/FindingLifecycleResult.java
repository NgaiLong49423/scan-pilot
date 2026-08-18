package com.scanpilot.scanner.lifecycle;

/**
 * Immutable evaluation result of finding lifecycle and remediation quality.
 */
public record FindingLifecycleResult(
    FindingLifecycle lifecycle,
    RemediationQuality remediationQuality
) {}
