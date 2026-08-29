package com.scanpilot.scanner.dispatcher;

/**
 * Typed provenance sources for scan jobs (FR-003, Issue #54).
 */
public enum ScanTriggerType {
    MANUAL,
    WEBHOOK_PUSH,
    WEBHOOK_PULL_REQUEST,
    WEBHOOK_MERGE
}
