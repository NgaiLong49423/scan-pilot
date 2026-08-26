package com.scanpilot.scanner.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for snapshot and workspace resource guardrails (FR-028, FR-031, NFR-001).
 * Enforces bounded limits on remote archive download, uncompressed workspace size, and file entry counts.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "scanpilot.scanner.guardrails")
public class SnapshotGuardrailProperties {

    /**
     * Maximum allowed size in bytes for downloaded snapshot archive (default: 20 MiB = 20,971,520 bytes).
     */
    private long maxArchiveBytes = 20 * 1024 * 1024L;

    /**
     * Maximum allowed cumulative uncompressed size in bytes for extracted workspace (default: 150 MiB = 157,286,400 bytes).
     */
    private long maxWorkspaceBytes = 150 * 1024 * 1024L;

    /**
     * Maximum allowed number of file/directory entries in archive (default: 10,000 entries).
     */
    private int maxEntryCount = 10000;

    /**
     * Maximum allowed cumulative duration in seconds for the entire scan job (default: 180 seconds).
     */
    private int maxScanTimeoutSeconds = 180;
}

