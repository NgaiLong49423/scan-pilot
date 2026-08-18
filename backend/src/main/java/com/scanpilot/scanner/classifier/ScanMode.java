package com.scanpilot.scanner.classifier;

/**
 * Scan modes supported by Scan Pilot.
 */
public enum ScanMode {
    /**
     * Continuous monitoring mode with standard latency and bounded file size limits (10 MiB).
     */
    CONTINUOUS_MONITORING,

    /**
     * Release assessment mode with comprehensive file verification up to ceiling (50 MiB).
     */
    RELEASE_ASSESSMENT
}
