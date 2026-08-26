package com.scanpilot.scanner.detector.gitleaks;

import java.nio.file.Path;

/**
 * Scan request parameters for Gitleaks detector adapter.
 */
public record GitleaksScanRequest(
    Path targetPath,
    boolean isGitScan,
    String commitRange,
    String logOpts,
    Integer overrideTimeoutSeconds
) {
    public GitleaksScanRequest(Path targetPath, boolean isGitScan, String commitRange, String logOpts) {
        this(targetPath, isGitScan, commitRange, logOpts, null);
    }

    /**
     * Creates a snapshot scan request for the given directory path.
     */
    public static GitleaksScanRequest forSnapshot(Path targetPath) {
        return new GitleaksScanRequest(targetPath, false, null, null, null);
    }

    /**
     * Creates a snapshot scan request with an override timeout in seconds.
     */
    public static GitleaksScanRequest forSnapshot(Path targetPath, Integer overrideTimeoutSeconds) {
        return new GitleaksScanRequest(targetPath, false, null, null, overrideTimeoutSeconds);
    }

    /**
     * Creates a Git history scan request for the given repository path and commit range.
     */
    public static GitleaksScanRequest forGitHistory(Path targetPath, String commitRange) {
        return new GitleaksScanRequest(targetPath, true, commitRange, null, null);
    }

    /**
     * Creates a Git history scan request with an override timeout in seconds.
     */
    public static GitleaksScanRequest forGitHistory(Path targetPath, String commitRange, Integer overrideTimeoutSeconds) {
        return new GitleaksScanRequest(targetPath, true, commitRange, null, overrideTimeoutSeconds);
    }
}
