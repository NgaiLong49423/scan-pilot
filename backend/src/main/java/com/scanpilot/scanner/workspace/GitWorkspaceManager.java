package com.scanpilot.scanner.workspace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

/**
 * Manages isolated temporary workspaces for scanning operations.
 * Guarantees recursive deletion of workspace files upon disposal (FR-025, DEC-015).
 */
@Slf4j
@Service
public class GitWorkspaceManager {

    /**
     * Creates an isolated temporary directory for repository scanning.
     *
     * @param repositoryId the repository UUID
     * @return GitWorkspace record containing repositoryId and isolated workspace Path
     */
    public GitWorkspace createWorkspace(UUID repositoryId) {
        try {
            String prefix = "scanpilot-ws-" + (repositoryId != null ? repositoryId.toString() : "repo") + "-";
            Path tempDir = Files.createTempDirectory(prefix);
            log.debug("Created temporary workspace at '{}' for repository '{}'", tempDir, repositoryId);
            return new GitWorkspace(repositoryId, tempDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temporary workspace directory", e);
        }
    }

    /**
     * Creates an isolated temporary directory and returns its Path.
     *
     * @param repositoryId the repository UUID
     * @return Path to the created temporary workspace
     */
    public Path createWorkspacePath(UUID repositoryId) {
        return createWorkspace(repositoryId).workspacePath();
    }

    /**
     * Disposes a temporary workspace and guarantees recursive deletion of files.
     *
     * @param workspace the workspace record to dispose
     */
    public void disposeWorkspace(GitWorkspace workspace) {
        if (workspace != null && workspace.workspacePath() != null) {
            disposeWorkspace(workspace.workspacePath());
        }
    }

    /**
     * Disposes a temporary workspace path and guarantees recursive deletion of files.
     *
     * @param workspacePath path to the temporary workspace directory
     */
    public void disposeWorkspace(Path workspacePath) {
        if (workspacePath == null || !Files.exists(workspacePath)) {
            return;
        }

        try {
            // Clear read-only flags on Windows (e.g. .git/objects)
            Files.walkFileTree(workspacePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    file.toFile().setWritable(true, false);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    dir.toFile().setWritable(true, false);
                    return FileVisitResult.CONTINUE;
                }
            });
            boolean deleted = FileSystemUtils.deleteRecursively(workspacePath);
            if (deleted) {
                log.debug("Successfully disposed temporary workspace at '{}'", workspacePath);
            } else {
                log.warn("FileSystemUtils.deleteRecursively returned false for '{}'", workspacePath);
            }
        } catch (Exception e) {
            log.warn("Failed to delete temporary workspace at '{}': {}", workspacePath, e.getMessage());
        }
    }

    /**
     * Recursively copies a source directory into the target workspace directory.
     *
     * @param sourceDir the source directory path
     * @param targetDir the destination workspace path
     * @throws IOException if file copying fails
     */
    public void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        if (sourceDir == null || !Files.exists(sourceDir)) {
            return;
        }

        Files.createDirectories(targetDir);
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(dir);
                Path target = targetDir.resolve(relative);
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(file);
                Path target = targetDir.resolve(relative);
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
