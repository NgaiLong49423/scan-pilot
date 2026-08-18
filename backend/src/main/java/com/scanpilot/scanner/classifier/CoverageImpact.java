package com.scanpilot.scanner.classifier;

/**
 * Coverage impact assessment for scanned items or overall repository coverage.
 */
public enum CoverageImpact {
    /**
     * All eligible content was completely scanned.
     */
    COMPLETE,

    /**
     * Some eligible content was skipped (e.g. file size limit in continuous monitoring or undetermined content).
     */
    PARTIAL,

    /**
     * Mandatory release coverage ceiling was exceeded or critical content was omitted.
     */
    INCOMPLETE
}
