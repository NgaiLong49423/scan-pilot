package com.scanpilot.scanner.classifier;

/**
 * Lifecycle status of a repository item during scan coverage evaluation.
 */
public enum CoverageStatus {
    /**
     * Item has been discovered and considered by the eligibility policy.
     */
    CONSIDERED,

    /**
     * Item is eligible and scanned.
     */
    SCANNED,

    /**
     * Item was considered but skipped according to policy or technical boundary.
     */
    SKIPPED
}
