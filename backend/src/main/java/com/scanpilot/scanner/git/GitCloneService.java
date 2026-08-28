package com.scanpilot.scanner.git;

import com.scanpilot.scanner.config.GitCloneProperties;
import com.scanpilot.scanner.exception.ResourceGuardrailExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Service encapsulating secure, authenticated shallow Git clone process execution,
 * environment-only credential transport, controlled empty-hooks isolation,
 * and active workspace watchdog monitoring (FR-025, DEC-012, DEC-015).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitCloneService {

    private final GitCloneProperties properties;

    /**
     * Executes shallow git clone for the given repository and branch into an isolated workspace.
     *
     * @param repoFullName   repository full name (e.g. "owner/repo")
     * @param branch         target branch name (e.g. "main")
     * @param token          GitHub access token for environment injection (or null for public)
     * @param workspacePath  isolated ephemeral directory on disk
     * @param jobDeadline    cumulative scan deadline timestamp (or null if unconstrained)
     */
    public void cloneRepository(
            String repoFullName,
            String branch,
            String token,
            Path workspacePath,
            Instant jobDeadline
    ) {
        if (repoFullName == null || repoFullName.isBlank()) {
            throw new IllegalArgumentException("Repository full name is required for git clone");
        }
        if (branch == null || branch.isBlank()) {
            branch = "main";
        }
        if (workspacePath == null) {
            throw new IllegalArgumentException("Workspace path is required for git clone");
        }

        try {
            Files.createDirectories(workspacePath);
            Path emptyHooksDir = workspacePath.resolve(".empty-hooks");
            Files.createDirectories(emptyHooksDir);

            int effectiveTimeout = properties.getTimeoutSeconds();
            if (jobDeadline != null) {
                long remainingSeconds = Duration.between(Instant.now(), jobDeadline).toSeconds();
                if (remainingSeconds <= 0) {
                    throw new ResourceGuardrailExceededException("SCAN_TIMEOUT", 0, 0, properties.getTimeoutSeconds());
                }
                effectiveTimeout = Math.min((int) remainingSeconds, properties.getTimeoutSeconds());
            }

            ProcessBuilder pb = buildProcessBuilder(repoFullName, branch, token, workspacePath, emptyHooksDir);

            log.info("Executing shallow git clone [repo={}, branch={}, depth={}]",
                    repoFullName, branch, Math.min(properties.getDefaultDepth(), properties.getMaxDepth()));

            Process process = pb.start();

            monitorAndEnforceGuardrails(process, workspacePath, jobDeadline, effectiveTimeout);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("Git clone process exited with code {} for repository {}", exitCode, repoFullName);
                throw new IllegalStateException("Git clone failed with exit code " + exitCode);
            }

            log.info("Git clone completed successfully for repository {} on branch {}", repoFullName, branch);
        } catch (ResourceGuardrailExceededException rge) {
            throw rge;
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            log.warn("Git clone execution failure for repository {}", repoFullName);
            throw new IllegalStateException("Git clone execution failure");
        }
    }

    /**
     * Builds the ProcessBuilder with security hardening flags and environment credential transport.
     * Guaranteed zero token exposure in command-line arguments (argv) and safe output redirection.
     */
    public ProcessBuilder buildProcessBuilder(
            String repoFullName,
            String branch,
            String token,
            Path workspacePath,
            Path emptyHooksDir
    ) {
        List<String> command = new ArrayList<>();
        command.add(properties.getGitBinaryPath());
        command.add("-c");
        command.add("core.hooksPath=" + emptyHooksDir.toAbsolutePath().normalize().toString());
        command.add("-c");
        command.add("core.fsmonitor=false");
        command.add("clone");
        command.add("--depth");
        command.add(String.valueOf(Math.min(properties.getDefaultDepth(), properties.getMaxDepth())));
        command.add("--single-branch");
        command.add("--branch");
        command.add(branch);
        command.add("--no-recurse-submodules");
        command.add("--no-tags");
        command.add("https://github.com/" + repoFullName + ".git");
        command.add(workspacePath.toAbsolutePath().normalize().toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Map<String, String> env = pb.environment();

        if (token != null && !token.isBlank() && !token.startsWith("mock-")) {
            env.put("GIT_CONFIG_COUNT", "1");
            env.put("GIT_CONFIG_KEY_0", "http.extraHeader");
            env.put("GIT_CONFIG_VALUE_0", "Authorization: Bearer " + token);
        }
        env.put("GIT_TERMINAL_PROMPT", "0");

        return pb;
    }

    /**
     * Actively polls directory size and execution duration while the clone process is running.
     * Forcibly terminates the process tree if size threshold or deadline is exceeded.
     */
    public void monitorAndEnforceGuardrails(
            Process process,
            Path workspacePath,
            Instant jobDeadline,
            int effectiveTimeout
    ) throws InterruptedException {
        Instant startTime = Instant.now();

        while (process.isAlive()) {
            long currentBytes = computeDirectorySize(workspacePath);
            int entryCount = countEntries(workspacePath);

            if (currentBytes > properties.getOperationalStopThresholdBytes()) {
                log.warn("Active watchdog: workspace size {} bytes exceeded operational threshold {} bytes",
                        currentBytes, properties.getOperationalStopThresholdBytes());
                killProcessTree(process);
                throw new ResourceGuardrailExceededException(
                        "WORKSPACE_SIZE_EXCEEDED",
                        currentBytes,
                        entryCount,
                        150 * 1024 * 1024L
                );
            }

            boolean deadlineExceeded = (jobDeadline != null && Instant.now().isAfter(jobDeadline));
            boolean timeoutExceeded = Duration.between(startTime, Instant.now()).toSeconds() > effectiveTimeout;

            if (deadlineExceeded || timeoutExceeded) {
                log.warn("Active watchdog: git clone exceeded deadline or timeout (effectiveTimeout={}s)", effectiveTimeout);
                killProcessTree(process);
                throw new ResourceGuardrailExceededException(
                        "SCAN_TIMEOUT",
                        currentBytes,
                        entryCount,
                        effectiveTimeout
                );
            }

            long pollInterval = Math.max(50, properties.getPollIntervalMs());
            Thread.sleep(pollInterval);
        }

        // Post-execution boundary check
        long finalBytes = computeDirectorySize(workspacePath);
        int finalEntries = countEntries(workspacePath);
        if (finalBytes > properties.getOperationalStopThresholdBytes()) {
            throw new ResourceGuardrailExceededException(
                    "WORKSPACE_SIZE_EXCEEDED",
                    finalBytes,
                    finalEntries,
                    150 * 1024 * 1024L
            );
        }
    }

    /**
     * Forcibly kills the process and all of its descendants.
     */
    public void killProcessTree(Process process) {
        if (process == null) {
            return;
        }
        try {
            process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Error during git clone process tree termination: {}", e.getMessage());
        }
    }

    /**
     * Computes the cumulative size in bytes of all regular files within a directory tree.
     */
    public long computeDirectorySize(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Counts the total number of entries (files and directories) within a directory tree.
     */
    public int countEntries(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return (int) stream.count();
        } catch (IOException e) {
            return 0;
        }
    }
}
