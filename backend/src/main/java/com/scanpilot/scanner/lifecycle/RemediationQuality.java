package com.scanpilot.scanner.lifecycle;

/**
 * Finding remediation quality levels according to FR-018, FR-019, FR-051, and FINDING-TRACKING.md.
 */
public enum RemediationQuality {
    ACTION_REQUIRED,
    RISK_CONTAINED,
    VERIFIED_COMPLETE,
    NOT_ASSESSED
}
