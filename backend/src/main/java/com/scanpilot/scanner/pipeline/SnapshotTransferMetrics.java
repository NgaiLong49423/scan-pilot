package com.scanpilot.scanner.pipeline;

/**
 * Immutable metrics record captured during repository acquisition (Git clone or ZIP download).
 */
public record SnapshotTransferMetrics(
    String mode,
    Long archiveBytes,
    long workspaceBytes,
    int entryCount
) {
    public SnapshotTransferMetrics(long archiveBytes, long workspaceBytes, int entryCount) {
        this("ZIP_DOWNLOAD", archiveBytes, workspaceBytes, entryCount);
    }

    public static SnapshotTransferMetrics forGitClone(long workspaceBytes, int entryCount) {
        return new SnapshotTransferMetrics("GIT_CLONE", null, workspaceBytes, entryCount);
    }

    public static SnapshotTransferMetrics forZipDownload(long archiveBytes, long workspaceBytes, int entryCount) {
        return new SnapshotTransferMetrics("ZIP_DOWNLOAD", archiveBytes, workspaceBytes, entryCount);
    }
}
