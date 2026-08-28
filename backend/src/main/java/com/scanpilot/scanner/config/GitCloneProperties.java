package com.scanpilot.scanner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for authenticated shallow git clone operations and active watchdog guardrails (FR-025, DEC-015).
 */
@Data
@Component
@ConfigurationProperties(prefix = "scanpilot.scanner.git")
public class GitCloneProperties {

    /**
     * Default shallow clone depth (default: 50 commits).
     */
    private int defaultDepth = 50;

    /**
     * Maximum allowed clone depth (default: 100 commits).
     */
    private int maxDepth = 100;

    /**
     * Maximum timeout in seconds for git clone execution (default: 60 seconds).
     */
    private int timeoutSeconds = 60;

    /**
     * Binary path or executable name for Git (default: "git").
     */
    private String gitBinaryPath = "git";

    /**
     * Operational stop threshold in bytes for active workspace size watchdog (default: 120 MiB = 125,829,120 bytes).
     * Represents 80% watermark of the hard 150 MiB workspace limit.
     */
    private long operationalStopThresholdBytes = 120 * 1024 * 1024L;

    /**
     * Polling interval in milliseconds for the active watchdog directory size monitor (default: 250 ms).
     */
    private long pollIntervalMs = 250;
}
