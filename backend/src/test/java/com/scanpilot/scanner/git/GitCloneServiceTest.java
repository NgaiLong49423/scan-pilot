package com.scanpilot.scanner.git;

import com.scanpilot.scanner.config.GitCloneProperties;
import com.scanpilot.scanner.exception.ResourceGuardrailExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

@DisplayName("GitCloneService Guardrail, Isolation, and Exact-SHA Command Sequence Tests")
class GitCloneServiceTest {

    private GitCloneProperties properties;
    private GitCloneService gitCloneService;

    @BeforeEach
    void setUp() {
        properties = new GitCloneProperties();
        properties.setGitBinaryPath("git");
        properties.setDefaultDepth(50);
        properties.setMaxDepth(100);
        properties.setTimeoutSeconds(60);
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

    @Test
    @DisplayName("Should build exact-SHA clone command with --no-checkout, depth, and security isolation")
    void testBuildCloneProcessBuilderWithNoCheckout(@TempDir Path tempDir) {
        Path emptyHooks = tempDir.resolve(".empty-hooks");
        ProcessBuilder pb = gitCloneService.buildCloneProcessBuilder(
                "owner/repo",
                "feat-branch",
                true,
                "ghp_testtoken123",
                tempDir,
                emptyHooks
        );

        List<String> command = pb.command();
        assertThat(command).contains("clone", "--no-checkout", "--single-branch", "--branch", "feat-branch", "--no-recurse-submodules", "--no-tags");
        assertThat(command).contains("https://github.com/owner/repo.git");
        assertThat(command).contains(tempDir.toAbsolutePath().normalize().toString());

        // Assert zero token in argv command line
        assertThat(String.join(" ", command)).doesNotContain("ghp_testtoken123");

        // Assert token in GIT_CONFIG environment
        assertThat(pb.environment().get("GIT_CONFIG_VALUE_0")).isEqualTo("Authorization: Bearer ghp_testtoken123");
        assertThat(pb.environment().get("GIT_TERMINAL_PROMPT")).isEqualTo("0");
    }

    @Test
    @DisplayName("Should build fetch process builder for exact commit SHA")
    void testBuildFetchProcessBuilderForSha(@TempDir Path tempDir) {
        Path emptyHooks = tempDir.resolve(".empty-hooks");
        String targetSha = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
        ProcessBuilder pb = gitCloneService.buildFetchProcessBuilder(
                targetSha,
                "ghp_testtoken123",
                tempDir,
                emptyHooks
        );

        List<String> command = pb.command();
        assertThat(command).contains("fetch", "--depth", "origin", targetSha);
        assertThat(String.join(" ", command)).doesNotContain("ghp_testtoken123");
        assertThat(pb.environment().get("GIT_CONFIG_VALUE_0")).isEqualTo("Authorization: Bearer ghp_testtoken123");
    }

    @Test
    @DisplayName("Should build checkout process builder with --detach for exact commit SHA")
    void testBuildCheckoutProcessBuilderForSha(@TempDir Path tempDir) {
        Path emptyHooks = tempDir.resolve(".empty-hooks");
        String targetSha = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
        ProcessBuilder pb = gitCloneService.buildCheckoutProcessBuilder(
                targetSha,
                tempDir,
                emptyHooks
        );

        List<String> command = pb.command();
        assertThat(command).contains("checkout", "--detach", targetSha);
        assertThat(pb.environment().get("GIT_TERMINAL_PROMPT")).isEqualTo("0");
    }

    @Test
    @DisplayName("Should kill process tree and fail closed when rev-parse process times out (R54-B3-02)")
    void testRevParseTimeoutKillsProcessTreeAndThrows(@TempDir Path tempDir) throws Exception {
        // Set git binary to sleep command to simulate hanging rev-parse
        String[] sleepCmd = getSleepCommand(30);
        properties.setGitBinaryPath(sleepCmd[0]);
        gitCloneService = new GitCloneService(properties);

        Instant expiredDeadline = Instant.now().minusSeconds(1);

        assertThatThrownBy(() -> gitCloneService.resolveAndVerifyHeadSha(tempDir, "4b825dc642cb6eb9a060e54bf8d69288fbee4904", expiredDeadline, 1))
                .isInstanceOf(ResourceGuardrailExceededException.class)
                .hasMessageContaining("SCAN_TIMEOUT");
    }

    @Test
    @DisplayName("Should never log or expose raw exception messages, tokens, or paths during execution failure (R54-B3-01)")
    void testZeroDiagnosticLeakageOnExecutionFailure(@TempDir Path tempDir) {
        String forgedSecret = "ghp_forged_secret_token_1234567890abcdef";
        String forgedPath = "C:\\Users\\SecretAdmin\\private\\keys";
        String rawDiagnostic = "Internal network timeout with credentials at " + forgedPath + " token=" + forgedSecret;

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(GitCloneService.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> listAppender = new ch.qos.logback.core.read.ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            GitCloneProperties customProps = new GitCloneProperties();
            customProps.setGitBinaryPath(rawDiagnostic);
            GitCloneService customService = new GitCloneService(customProps);

            assertThatThrownBy(() -> customService.cloneRepository("owner/repo", "main", forgedSecret, tempDir, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Git clone execution failure") // Static safe message only
                    .satisfies(e -> {
                        assertThat(e.getMessage()).doesNotContain(forgedSecret);
                        assertThat(e.getMessage()).doesNotContain(forgedPath);
                    });

            for (ch.qos.logback.classic.spi.ILoggingEvent event : listAppender.list) {
                String formatted = event.getFormattedMessage();
                assertThat(formatted).doesNotContain(forgedSecret);
                assertThat(formatted).doesNotContain(forgedPath);
            }
        } finally {
            logger.detachAppender(listAppender);
        }
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
