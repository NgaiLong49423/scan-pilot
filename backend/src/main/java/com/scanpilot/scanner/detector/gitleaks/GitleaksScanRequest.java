package com.scanpilot.scanner.detector.gitleaks;

import java.nio.file.Path;

/**
 * Scan request parameters for Gitleaks detector adapter.
 */
public record GitleaksScanRequest(
    Path targetPath,
    boolean isGitScan,
    String commitRange,
    String logOpts
) {
    /**
     * Creates a snapshot scan request for the given directory path.
     */
    public static GitleaksScanRequest forSnapshot(Path targetPath) {
        return new GitleaksScanRequest(targetPath, false, null, null);
    }

    /**
     * Creates a Git history scan request for the given repository path and commit range.
     */
    public static GitleaksScanRequest forGitHistory(Path targetPath, String commitRange) {
        return new GitleaksScanRequest(targetPath, true, commitRange, null);
    }
}
