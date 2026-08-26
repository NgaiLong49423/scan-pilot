package com.scanpilot.scanner.pipeline;

/**
 * Immutable metrics record captured during snapshot download and archive extraction.
 */
public record SnapshotTransferMetrics(
    long archiveBytes,
    long workspaceBytes,
    int entryCount
) {}
