package com.scanpilot.scanner.workspace;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Encapsulates an isolated disposable workspace for scanning operations.
 */
public record GitWorkspace(
    UUID repositoryId,
    Path workspacePath
) {}
