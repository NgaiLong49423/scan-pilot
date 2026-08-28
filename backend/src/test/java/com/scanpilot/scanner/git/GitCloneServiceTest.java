package com.scanpilot.scanner.git;

import com.scanpilot.scanner.config.GitCloneProperties;
import com.scanpilot.scanner.exception.ResourceGuardrailExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("GitCloneService Unit & Security Hardening Tests (AC-01, AC-02, AC-03, AC-04)")
class GitCloneServiceTest {

    private GitCloneProperties properties;
    private GitCloneService gitCloneService;

    @BeforeEach
    void setUp() {
        properties = new GitCloneProperties();
        properties.setDefaultDepth(50);
        properties.setMaxDepth(100);
        properties.setTimeoutSeconds(60);
        properties.setGitBinaryPath("git");
        properties.setOperationalStopThresholdBytes(120 * 1024 * 1024L);
        properties.setPollIntervalMs(50);
        gitCloneService = new GitCloneService(properties);
    }

    @Test
    @DisplayName("AC-02: testCloneCommandOmitsTokenFromProcessArguments - zero token or header in process argv")
    void testCloneCommandOmitsTokenFromProcessArguments(@TempDir Path tempDir) {
        String token = "ghp_superSecretToken1234567890abcdef";
        Path emptyHooks = tempDir.resolve(".empty-hooks");

        ProcessBuilder pb = gitCloneService.buildProcessBuilder("org/repo", "main", token, tempDir, emptyHooks);
        List<String> command = pb.command();

        assertThat(pb.redirectOutput()).isEqualTo(ProcessBuilder.Redirect.DISCARD);
        assertThat(pb.redirectError()).isEqualTo(ProcessBuilder.Redirect.DISCARD);

        assertThat(command).noneMatch(arg -> arg.contains(token));
        assertThat(command).noneMatch(arg -> arg.contains("Authorization"));
        assertThat(command).noneMatch(arg -> arg.contains("http.extraHeader"));
        assertThat(command).noneMatch(arg -> arg.contains("GIT_CONFIG"));
        assertThat(command).contains("clone");
        assertThat(command).contains("--depth");
        assertThat(command).contains("50");
        assertThat(command).contains("--single-branch");
        assertThat(command).contains("--branch");
        assertThat(command).contains("main");
        assertThat(command).contains("--no-recurse-submodules");
        assertThat(command).contains("--no-tags");
        assertThat(command).contains("https://github.com/org/repo.git");
    }

    @Test
    @DisplayName("AC-02: testCloneSetsEnvironmentVariablesCorrectly - credential injected strictly in environment map")
    void testCloneSetsEnvironmentVariablesCorrectly(@TempDir Path tempDir) {
        String token = "ghp_superSecretToken1234567890abcdef";
        Path emptyHooks = tempDir.resolve(".empty-hooks");

        ProcessBuilder pb = gitCloneService.buildProcessBuilder("org/repo", "main", token, tempDir, emptyHooks);
        Map<String, String> env = pb.environment();

        assertThat(env.get("GIT_CONFIG_COUNT")).isEqualTo("1");
        assertThat(env.get("GIT_CONFIG_KEY_0")).isEqualTo("http.extraHeader");
        assertThat(env.get("GIT_CONFIG_VALUE_0")).isEqualTo("Authorization: Bearer " + token);
        assertThat(env.get("GIT_TERMINAL_PROMPT")).isEqualTo("0");
    }

    @Test
    @DisplayName("AC-02: testCloneOmitsEnvironmentWhenTokenNullOrBlank")
    void testCloneOmitsEnvironmentWhenTokenNullOrBlank(@TempDir Path tempDir) {
        Path emptyHooks = tempDir.resolve(".empty-hooks");

        ProcessBuilder pb = gitCloneService.buildProcessBuilder("org/repo", "main", null, tempDir, emptyHooks);
        Map<String, String> env = pb.environment();

        assertThat(env.get("GIT_CONFIG_COUNT")).isNull();
        assertThat(env.get("GIT_CONFIG_KEY_0")).isNull();
        assertThat(env.get("GIT_CONFIG_VALUE_0")).isNull();
        assertThat(env.get("GIT_TERMINAL_PROMPT")).isEqualTo("0");
    }

    @Test
    @DisplayName("AC-03: testCloneConfiguresControlledEmptyHooksDirectory - core.hooksPath points to empty directory")
    void testCloneConfiguresControlledEmptyHooksDirectory(@TempDir Path tempDir) throws IOException {
        Path emptyHooks = tempDir.resolve(".empty-hooks");
        Files.createDirectories(emptyHooks);

        ProcessBuilder pb = gitCloneService.buildProcessBuilder("org/repo", "main", "token", tempDir, emptyHooks);
        List<String> command = pb.command();

        String expectedHooksArg = "core.hooksPath=" + emptyHooks.toAbsolutePath().normalize().toString();
        assertThat(command).contains(expectedHooksArg);
        assertThat(command).contains("core.fsmonitor=false");

        // Verify .empty-hooks directory has 0 files
        try (var stream = Files.list(emptyHooks)) {
            assertThat(stream.count()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("AC-04: testActiveWatchdogKillsProcessOnSizeLimit - aborts and kills process tree when threshold breached")
    void testActiveWatchdogKillsProcessOnSizeLimit(@TempDir Path tempDir) throws Exception {
        properties.setOperationalStopThresholdBytes(1024L); // 1 KiB threshold for test
        gitCloneService = new GitCloneService(properties);

        // Start a long-running mock process (sleep)
        ProcessBuilder pb = new ProcessBuilder(getSleepCommand(30));
        Process process = pb.start();

        try {
            // Write files in background to trigger size limit
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100);
                    byte[] largeData = new byte[2048];
                    Files.write(tempDir.resolve("large_file.dat"), largeData);
                } catch (Exception ignored) {}
            });

            assertThatThrownBy(() -> gitCloneService.monitorAndEnforceGuardrails(process, tempDir, null, 60))
                    .isInstanceOf(ResourceGuardrailExceededException.class)
                    .hasMessageContaining("WORKSPACE_SIZE_EXCEEDED");

            // Process must be killed
            boolean exited = process.waitFor(5, TimeUnit.SECONDS);
            assertThat(exited).isTrue();
            assertThat(process.isAlive()).isFalse();
        } finally {
            gitCloneService.killProcessTree(process);
        }
    }

    @Test
    @DisplayName("AC-04: testActiveWatchdogKillsProcessOnTimeout - aborts when job deadline expires")
    void testActiveWatchdogKillsProcessOnTimeout(@TempDir Path tempDir) throws Exception {
        properties.setPollIntervalMs(50);
        gitCloneService = new GitCloneService(properties);

        ProcessBuilder pb = new ProcessBuilder(getSleepCommand(30));
        Process process = pb.start();

        try {
            Instant deadline = Instant.now().plusMillis(200);

            assertThatThrownBy(() -> gitCloneService.monitorAndEnforceGuardrails(process, tempDir, deadline, 1))
                    .isInstanceOf(ResourceGuardrailExceededException.class)
                    .hasMessageContaining("SCAN_TIMEOUT");

            boolean exited = process.waitFor(5, TimeUnit.SECONDS);
            assertThat(exited).isTrue();
            assertThat(process.isAlive()).isFalse();
        } finally {
            gitCloneService.killProcessTree(process);
        }
    }

    @Test
    @DisplayName("AC-08: testInvalidArgumentsFailClosedImmediately")
    void testInvalidArgumentsFailClosedImmediately(@TempDir Path tempDir) {
        assertThatThrownBy(() -> gitCloneService.cloneRepository(null, "main", null, tempDir, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> gitCloneService.cloneRepository("owner/repo", "main", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AC-02: testCloneSanitizesExceptionMessages - raw diagnostics or tokens not exposed in exception")
    void testCloneSanitizesExceptionMessages(@TempDir Path tempDir) {
        properties.setGitBinaryPath("non_existent_git_binary_for_test");
        gitCloneService = new GitCloneService(properties);

        String secretToken = "ghp_superSecretToken1234567890abcdef";

        assertThatThrownBy(() -> gitCloneService.cloneRepository("org/repo", "main", secretToken, tempDir, null))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(ex -> {
                    assertThat(ex.getMessage()).doesNotContain(secretToken);
                    assertThat(ex.getMessage()).doesNotContain("Authorization");
                    assertThat(ex.getMessage()).isEqualTo("Git clone execution failure");
                });
    }

    private String[] getSleepCommand(int seconds) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"powershell.exe", "-Command", "Start-Sleep -Seconds " + seconds};
        } else {
            return new String[]{"sleep", String.valueOf(seconds)};
        }
    }
}
