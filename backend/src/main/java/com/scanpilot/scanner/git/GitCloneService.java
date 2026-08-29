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
     */
    public void cloneRepository(
            String repoFullName,
            String branch,
            String token,
            Path workspacePath,
            Instant jobDeadline
    ) {
        cloneRepository(repoFullName, branch, null, token, workspacePath, jobDeadline);
    }

    /**
     * Executes shallow git clone and optional detached exact-SHA checkout into an isolated workspace (Issue #54).
     */
    public void cloneRepository(
            String repoFullName,
            String branch,
            String expectedCommitSha,
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

            boolean exactShaMode = (expectedCommitSha != null && !expectedCommitSha.isBlank());

            // 1. Clone repository (with --no-checkout if in exact-SHA mode)
            ProcessBuilder clonePb = buildCloneProcessBuilder(repoFullName, branch, exactShaMode, token, workspacePath, emptyHooksDir);

            log.info("Executing shallow git clone [repo={}, branch={}, exactShaMode={}, depth={}]",
                    repoFullName, branch, exactShaMode, Math.min(properties.getDefaultDepth(), properties.getMaxDepth()));

            runMonitoredProcess(clonePb, workspacePath, jobDeadline, effectiveTimeout, "Git clone");

            if (exactShaMode) {
                // 2. Fetch specific commit SHA from origin
                ProcessBuilder fetchPb = buildFetchProcessBuilder(expectedCommitSha, token, workspacePath, emptyHooksDir);
                log.info("Executing git fetch for exact SHA {} [repo={}]", expectedCommitSha, repoFullName);
                runMonitoredProcess(fetchPb, workspacePath, jobDeadline, effectiveTimeout, "Git fetch SHA");

                // 3. Detached checkout of target commit SHA
                ProcessBuilder checkoutPb = buildCheckoutProcessBuilder(expectedCommitSha, workspacePath, emptyHooksDir);
                log.info("Executing git checkout --detach {} [repo={}]", expectedCommitSha, repoFullName);
                runMonitoredProcess(checkoutPb, workspacePath, jobDeadline, effectiveTimeout, "Git checkout --detach");

                // 4. Verify absolute equality: rev-parse HEAD == expectedCommitSha
                String verifiedHead = resolveAndVerifyHeadSha(workspacePath, expectedCommitSha, jobDeadline, effectiveTimeout);
                log.info("Git clone & exact SHA checkout verified successfully for {} at commit {}", repoFullName, verifiedHead);
            } else {
                log.info("Git clone completed successfully for repository {} on branch {}", repoFullName, branch);
            }
        } catch (ResourceGuardrailExceededException rge) {
            throw rge;
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            log.warn("Git clone execution failure for repository {}", repoFullName);
            throw new IllegalStateException("Git clone execution failure", e);
        }
    }

    private void runMonitoredProcess(
            ProcessBuilder pb,
            Path workspacePath,
            Instant jobDeadline,
            int effectiveTimeout,
            String stageName
    ) throws Exception {
        Process process = pb.start();
        monitorAndEnforceGuardrails(process, workspacePath, jobDeadline, effectiveTimeout);
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("{} process exited with code {}", stageName, exitCode);
            throw new IllegalStateException(stageName + " failed with exit code " + exitCode);
        }
    }

    public String resolveAndVerifyHeadSha(Path workspacePath, String expectedCommitSha) throws Exception {
        return resolveAndVerifyHeadSha(workspacePath, expectedCommitSha, null, properties.getTimeoutSeconds());
    }

    public String resolveAndVerifyHeadSha(
            Path workspacePath,
            String expectedCommitSha,
            Instant jobDeadline,
            int effectiveTimeout
    ) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(properties.getGitBinaryPath(), "rev-parse", "HEAD");
        pb.directory(workspacePath.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try {
            long timeoutSeconds = 5L;
            if (jobDeadline != null) {
                long remainingMillis = java.time.Duration.between(Instant.now(), jobDeadline).toMillis();
                if (remainingMillis <= 0) {
                    killProcessTree(process);
                    throw new ResourceGuardrailExceededException("SCAN_TIMEOUT", 0L, 0, effectiveTimeout);
                }
                timeoutSeconds = Math.min(timeoutSeconds, Math.max(1, remainingMillis / 1000));
            } else if (effectiveTimeout > 0) {
                timeoutSeconds = Math.min(timeoutSeconds, effectiveTimeout);
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                killProcessTree(process);
                throw new IllegalStateException("Git rev-parse HEAD process timed out");
            }
            if (process.exitValue() != 0) {
                killProcessTree(process);
                throw new IllegalStateException("Failed to resolve git rev-parse HEAD");
            }
            String resolvedSha = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            if (!resolvedSha.equalsIgnoreCase(expectedCommitSha)) {
                log.error("Commit SHA mismatch: expected {} but resolved {}", expectedCommitSha, resolvedSha);
                throw new IllegalStateException("Commit SHA mismatch: expected " + expectedCommitSha + " but was " + resolvedSha);
            }
            return resolvedSha;
        } catch (Exception e) {
            killProcessTree(process);
            throw e;
        }
    }

    public ProcessBuilder buildCloneProcessBuilder(
            String repoFullName,
            String branch,
            boolean noCheckout,
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
        if (noCheckout) {
            command.add("--no-checkout");
        }
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

    public ProcessBuilder buildFetchProcessBuilder(
            String expectedCommitSha,
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
        command.add("fetch");
        command.add("--depth");
        command.add(String.valueOf(Math.min(properties.getDefaultDepth(), properties.getMaxDepth())));
        command.add("origin");
        command.add(expectedCommitSha);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workspacePath.toFile());
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

    public ProcessBuilder buildCheckoutProcessBuilder(
            String expectedCommitSha,
            Path workspacePath,
            Path emptyHooksDir
    ) {
        List<String> command = new ArrayList<>();
        command.add(properties.getGitBinaryPath());
        command.add("-c");
        command.add("core.hooksPath=" + emptyHooksDir.toAbsolutePath().normalize().toString());
        command.add("-c");
        command.add("core.fsmonitor=false");
        command.add("checkout");
        command.add("--detach");
        command.add(expectedCommitSha);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workspacePath.toFile());
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.environment().put("GIT_TERMINAL_PROMPT", "0");

        return pb;
    }

    public ProcessBuilder buildProcessBuilder(
            String repoFullName,
            String branch,
            String token,
            Path workspacePath,
            Path emptyHooksDir
    ) {
        return buildCloneProcessBuilder(repoFullName, branch, false, token, workspacePath, emptyHooksDir);
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
            log.warn("Error during git clone process tree termination");
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
