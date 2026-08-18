package com.scanpilot.scanner.detector.gitleaks;

import java.util.Collections;
import java.util.List;

/**
 * Result of executing a Gitleaks or embedded detection scan.
 */
public record GitleaksScanResult(
    List<GitleaksRawFinding> findings,
    int exitCode,
    String scannedPath,
    long durationMs,
    String errorMessage
) {
    public GitleaksScanResult {
        if (findings == null) {
            findings = Collections.emptyList();
        }
    }

    public boolean isSuccess() {
        return errorMessage == null;
    }

    public static GitleaksScanResult success(List<GitleaksRawFinding> findings, int exitCode, String scannedPath, long durationMs) {
        return new GitleaksScanResult(findings, exitCode, scannedPath, durationMs, null);
    }

    public static GitleaksScanResult error(String errorMessage, int exitCode, String scannedPath, long durationMs) {
        return new GitleaksScanResult(Collections.emptyList(), exitCode, scannedPath, durationMs, errorMessage);
    }
}
