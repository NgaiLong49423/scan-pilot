package com.scanpilot.scanner.workspace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Git Workspace Manager Tests")
class GitWorkspaceManagerTest {

    private final GitWorkspaceManager workspaceManager = new GitWorkspaceManager();

    @Test
    @DisplayName("Should create isolated workspace and recursively dispose it")
    void shouldCreateAndDisposeWorkspace() throws IOException {
        UUID repositoryId = UUID.randomUUID();
        GitWorkspace workspace = workspaceManager.createWorkspace(repositoryId);

        assertThat(workspace).isNotNull();
        assertThat(workspace.repositoryId()).isEqualTo(repositoryId);
        assertThat(workspace.workspacePath()).isNotNull();
        assertThat(Files.exists(workspace.workspacePath())).isTrue();

        // Create test nested files
        Path subDir = Files.createDirectory(workspace.workspacePath().resolve("src"));
        Path sampleFile = Files.writeString(subDir.resolve("App.java"), "public class App {}");
        assertThat(Files.exists(sampleFile)).isTrue();

        // Dispose workspace
        workspaceManager.disposeWorkspace(workspace);

        assertThat(Files.exists(workspace.workspacePath())).isFalse();
        assertThat(Files.exists(sampleFile)).isFalse();
    }

    @Test
    @DisplayName("Should recursively copy directory into workspace")
    void shouldCopyDirectoryRecursively(@TempDir Path tempSrc) throws IOException {
        Path subDir = Files.createDirectory(tempSrc.resolve("package"));
        Files.writeString(subDir.resolve("Config.json"), "{\"apiKey\": \"secret\"}");

        UUID repositoryId = UUID.randomUUID();
        GitWorkspace workspace = workspaceManager.createWorkspace(repositoryId);

        try {
            workspaceManager.copyDirectory(tempSrc, workspace.workspacePath());
            Path copied = workspace.workspacePath().resolve("package/Config.json");
            assertThat(Files.exists(copied)).isTrue();
            assertThat(Files.readString(copied)).contains("apiKey");
        } finally {
            workspaceManager.disposeWorkspace(workspace);
        }
    }
}
