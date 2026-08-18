package com.scanpilot.scanner.detector.gitleaks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.security.secret.RedactedEvidence;
import com.scanpilot.security.secret.SecretMatch;
import com.scanpilot.security.secret.SecretRedactionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adapter service for invoking Gitleaks binary with the trusted SP-CONFIG-001 policy.
 * Provides process execution, secure report deletion in finally blocks,
 * embedded regex detection engine fallback, and normalized secret evidence generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitleaksDetectorAdapter {

    private final GitleaksConfigProperties properties;
    private final SecretRedactionService redactionService;
    private final ObjectMapper objectMapper;

    private String policyContent;
    private String policyDigest;

    /**
     * Canonical embedded SP-CONFIG-001 rules matching trusted policy TOML.
     */
    private static final List<SpConfigRule> CANONICAL_RULES = List.of(
        new SpConfigRule(
            "google-api-key",
            "Google API Key",
            Pattern.compile("AIza[0-9A-Za-z\\-_]{35}"),
            0
        ),
        new SpConfigRule(
            "github-pat",
            "GitHub Personal Access Token or OAuth/App Token",
            Pattern.compile("(ghp_[0-9a-zA-Z]{36}|github_pat_[0-9a-zA-Z_]{82}|gho_[0-9a-zA-Z]{36}|ghs_[0-9a-zA-Z]{36}|ghr_[0-9a-zA-Z]{36})"),
            0
        ),
        new SpConfigRule(
            "aws-access-key",
            "AWS Access Key ID",
            Pattern.compile("(A3T[A-Z0-9]|AKIA|AGPA|AIDA|AROA|AIPA|ANPA|ANVA|ASIA)[A-Z0-9]{16}"),
            0
        ),
        new SpConfigRule(
            "private-key",
            "Private Key",
            Pattern.compile("-----BEGIN[ A-Z0-9_-]*PRIVATE KEY-----"),
            0
        ),
        new SpConfigRule(
            "generic-api-key",
            "Generic High-Entropy API Key or Token",
            Pattern.compile("(?i)(?:key|api_key|apikey|secret|token|password|auth|jwt)[ \\t]*[=:][ \\t]*['\"]([a-zA-Z0-9_\\-]{20,})['\"]"),
            1
        )
    );

    @PostConstruct
    public void init() {
        loadPolicy();
    }

    /**
     * Loads the trusted policy resource from classpath and calculates SHA-256 digest.
     */
    public synchronized void loadPolicy() {
        try {
            ClassPathResource resource = new ClassPathResource(properties.getPolicyResourcePath());
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    this.policyContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    this.policyDigest = computeSha256(this.policyContent);
                    log.info("Loaded SP-CONFIG-001 policy from '{}' with SHA-256 digest: {}",
                        properties.getPolicyResourcePath(), this.policyDigest);
                }
            } else {
                log.warn("Policy resource '{}' not found on classpath, using embedded defaults",
                    properties.getPolicyResourcePath());
                this.policyContent = "# Embedded default policy";
                this.policyDigest = computeSha256(this.policyContent);
            }
        } catch (Exception e) {
            log.error("Failed to load policy from '{}'", properties.getPolicyResourcePath(), e);
            this.policyContent = "# Embedded default policy";
            this.policyDigest = computeSha256(this.policyContent);
        }
    }

    public String getPolicyContent() {
        if (policyContent == null) {
            loadPolicy();
        }
        return policyContent;
    }

    public String getPolicyDigest() {
        if (policyDigest == null) {
            loadPolicy();
        }
        return policyDigest;
    }

    /**
     * Checks if Gitleaks executable binary is present and executable.
     */
    public boolean isBinaryAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(properties.getBinaryPath(), "version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Gitleaks binary '{}' check failed: {}", properties.getBinaryPath(), e.getMessage());
            return false;
        }
    }

    /**
     * Scans a target path using Gitleaks binary if available, falling back to embedded regex engine.
     */
    public GitleaksScanResult scan(GitleaksScanRequest request) {
        if (isBinaryAvailable()) {
            try {
                return scanWithBinary(request);
            } catch (Exception e) {
                log.warn("Gitleaks binary execution failed, falling back to embedded engine: {}", e.getMessage());
                return scanEmbedded(request);
            }
        } else {
            log.info("Gitleaks binary not available; executing embedded SP-CONFIG-001 engine on {}",
                request.targetPath());
            return scanEmbedded(request);
        }
    }

    /**
     * Executes scan via Gitleaks CLI process with isolated configuration and temporary report.
     * Guaranteed deletion of temporary raw report in finally block.
     */
    public GitleaksScanResult scanWithBinary(GitleaksScanRequest request) {
        Path targetPath = request.targetPath();
        long startTime = System.currentTimeMillis();

        Path tempReport = null;
        Path tempConfig = null;

        try {
            tempReport = Files.createTempFile("scanpilot-gitleaks-report-", ".json");
            tempConfig = prepareTrustedConfigFile();

            List<String> command = new ArrayList<>();
            command.add(properties.getBinaryPath());

            if (request.isGitScan()) {
                command.add("git");
                command.add(targetPath.toAbsolutePath().toString());
                if (request.commitRange() != null && !request.commitRange().isBlank()) {
                    command.add("--log-opts=" + request.commitRange());
                }
            } else {
                command.add("dir");
                command.add(targetPath.toAbsolutePath().toString());
                command.add("--no-git");
            }

            command.add("--config");
            command.add(tempConfig.toAbsolutePath().toString());
            command.add("--report-path");
            command.add(tempReport.toAbsolutePath().toString());
            command.add("--report-format=json");
            command.add("--redact=false");
            command.add("--ignore-gitleaks-allow");

            ProcessBuilder pb = new ProcessBuilder(command);
            // Environment isolation: remove unapproved configuration environment variables
            pb.environment().remove("GITLEAKS_CONFIG");
            pb.environment().remove("GITLEAKS_CONFIG_TOML");

            pb.redirectErrorStream(true);
            log.info("Executing Gitleaks detector CLI [isGitScan={}, path={}]",
                request.isGitScan(), targetPath);

            Process process = pb.start();
            boolean completed = process.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                long duration = System.currentTimeMillis() - startTime;
                return GitleaksScanResult.error("Gitleaks process timed out after " + properties.getTimeoutSeconds() + "s",
                    -1, targetPath.toString(), duration);
            }

            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;

            List<GitleaksRawFinding> findings = Collections.emptyList();
            if (Files.exists(tempReport) && Files.size(tempReport) > 0) {
                findings = parseJsonReport(tempReport);
            }

            log.info("Gitleaks execution completed with exitCode={} and findingsCount={} in {}ms",
                exitCode, findings.size(), duration);

            return GitleaksScanResult.success(findings, exitCode, targetPath.toString(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to execute Gitleaks binary: {}", e.getMessage());
            return GitleaksScanResult.error("Execution failure: " + e.getMessage(), -1, targetPath.toString(), duration);
        } finally {
            // Mandated strict cleanup: immediately delete raw temporary JSON report and config
            if (tempReport != null) {
                try {
                    Files.deleteIfExists(tempReport);
                } catch (IOException ignored) {}
            }
            if (tempConfig != null) {
                try {
                    Files.deleteIfExists(tempConfig);
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Executes the embedded SP-CONFIG-001 regex detection engine.
     */
    /**
     * Executes the embedded SP-CONFIG-001 regex detection engine.
     */
    public GitleaksScanResult scanEmbedded(GitleaksScanRequest request) {
        Path targetPath = request.targetPath();
        long startTime = System.currentTimeMillis();
        List<GitleaksRawFinding> findings = new ArrayList<>();

        try {
            if (request.isGitScan() && Files.isDirectory(targetPath) && Files.exists(targetPath.resolve(".git"))) {
                scanGitHistoryWithCli(targetPath, request.commitRange(), findings);
            } else if (Files.isRegularFile(targetPath)) {
                scanSingleFile(targetPath, targetPath.getFileName().toString(), findings);
            } else if (Files.isDirectory(targetPath)) {
                try (Stream<Path> stream = Files.walk(targetPath)) {
                    stream.filter(Files::isRegularFile)
                        .filter(p -> !isIgnoredDirectory(p, targetPath))
                        .forEach(p -> {
                            String relPath = targetPath.relativize(p).toString().replace('\\', '/');
                            scanSingleFile(p, relPath, findings);
                        });
                }
            } else {
                return GitleaksScanResult.error("Target path does not exist: " + targetPath,
                    -1, targetPath.toString(), 0);
            }

            long duration = System.currentTimeMillis() - startTime;
            int exitCode = findings.isEmpty() ? 0 : 1;
            log.info("Embedded SP-CONFIG-001 scan completed with findingsCount={} in {}ms",
                findings.size(), duration);

            return GitleaksScanResult.success(findings, exitCode, targetPath.toString(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error during embedded SP-CONFIG-001 scan: {}", e.getMessage());
            return GitleaksScanResult.error("Embedded scan error: " + e.getMessage(), -1, targetPath.toString(), duration);
        }
    }

    /**
     * Scans git history by invoking git log -p when Gitleaks binary is not present.
     */
    private void scanGitHistoryWithCli(Path repoPath, String commitRange, List<GitleaksRawFinding> findings) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.add("log");
            cmd.add("-p");
            if (commitRange != null && !commitRange.isBlank()) {
                cmd.add(commitRange);
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(repoPath.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                String currentCommit = null;
                String currentAuthor = null;
                String currentDate = null;
                String currentFile = null;
                int lineNum = 0;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("commit ")) {
                        currentCommit = line.substring(7).trim();
                        currentFile = null;
                        lineNum = 0;
                    } else if (line.startsWith("Author: ")) {
                        currentAuthor = line.substring(8).trim();
                    } else if (line.startsWith("Date: ")) {
                        currentDate = line.substring(6).trim();
                    } else if (line.startsWith("diff --git ")) {
                        String[] parts = line.split(" ");
                        if (parts.length >= 4) {
                            String bPath = parts[3];
                            if (bPath.startsWith("b/")) {
                                currentFile = bPath.substring(2);
                            } else {
                                currentFile = bPath;
                            }
                        }
                        lineNum = 0;
                    } else if (line.startsWith("@@ ")) {
                        int plusIdx = line.indexOf('+');
                        if (plusIdx != -1) {
                            int commaIdx = line.indexOf(',', plusIdx);
                            int endIdx = line.indexOf(' ', plusIdx);
                            int targetIdx = commaIdx != -1 && commaIdx < endIdx ? commaIdx : endIdx;
                            if (targetIdx != -1) {
                                try {
                                    lineNum = Integer.parseInt(line.substring(plusIdx + 1, targetIdx).trim()) - 1;
                                } catch (NumberFormatException ignored) {
                                    lineNum = 1;
                                }
                            }
                        }
                    } else if (line.startsWith("+") && !line.startsWith("+++")) {
                        lineNum++;
                        String addedContent = line.substring(1);
                        scanLineContent(addedContent, lineNum, currentFile != null ? currentFile : "unknown", currentCommit, currentAuthor, currentDate, findings);
                    } else if (!line.startsWith("-")) {
                        lineNum++;
                    }
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Git log history scan failed: {}", e.getMessage());
        }
    }

    /**
     * Scans a single text file against the SP-CONFIG-001 rules.
     */
    private void scanSingleFile(Path filePath, String relativePath, List<GitleaksRawFinding> findings) {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineNum = i + 1;
                scanLineContent(line, lineNum, relativePath, null, null, null, findings);
            }
        } catch (Exception e) {
            log.debug("Skipped non-text file during embedded scan: {}", relativePath);
        }
    }

    private void scanLineContent(
        String line,
        int lineNum,
        String relativePath,
        String commit,
        String author,
        String date,
        List<GitleaksRawFinding> findings
    ) {
        List<GitleaksRawFinding> lineFindings = new ArrayList<>();
        for (SpConfigRule rule : CANONICAL_RULES) {
            Matcher matcher = rule.pattern().matcher(line);
            while (matcher.find()) {
                String secret = rule.secretGroupIndex() > 0 && matcher.groupCount() >= rule.secretGroupIndex()
                    ? matcher.group(rule.secretGroupIndex())
                    : matcher.group();

                int startCol = (rule.secretGroupIndex() > 0 && matcher.groupCount() >= rule.secretGroupIndex()
                    ? matcher.start(rule.secretGroupIndex())
                    : matcher.start()) + 1;

                int endCol = (rule.secretGroupIndex() > 0 && matcher.groupCount() >= rule.secretGroupIndex()
                    ? matcher.end(rule.secretGroupIndex())
                    : matcher.end());

                if ("generic-api-key".equals(rule.ruleId())) {
                    boolean alreadyCovered = lineFindings.stream().anyMatch(lf ->
                        !lf.ruleID().equals("generic-api-key") && (
                            lf.secret().equals(secret) ||
                            (startCol <= lf.endColumn() && endCol >= lf.startColumn())
                        )
                    );
                    if (alreadyCovered) {
                        continue;
                    }
                }

                GitleaksRawFinding finding = new GitleaksRawFinding(
                    rule.ruleId(),
                    rule.description(),
                    lineNum,
                    lineNum,
                    startCol,
                    endCol,
                    line,
                    secret,
                    relativePath,
                    commit,
                    null,
                    author,
                    null,
                    date,
                    null
                );
                lineFindings.add(finding);
            }
        }
        findings.addAll(lineFindings);
    }

    private boolean isIgnoredDirectory(Path file, Path root) {
        Path relative = root.relativize(file);
        for (Path part : relative) {
            String name = part.toString();
            if (name.equals(".git") || name.equals("node_modules") || name.equals(".idea")
                || name.equals(".vscode") || name.equals("target") || name.equals("build")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes scan and immediately transforms raw findings into safe DetectedSecretFinding records.
     * Ensures zero raw secrets escape into downstream workflows or logs.
     */
    public List<DetectedSecretFinding> scanAndNormalize(String repositoryId, GitleaksScanRequest request) {
        GitleaksScanResult result = scan(request);
        if (!result.isSuccess() || result.findings().isEmpty()) {
            return Collections.emptyList();
        }

        List<DetectedSecretFinding> normalizedList = new ArrayList<>();
        Set<String> detectedRuleIds = new HashSet<>();

        for (GitleaksRawFinding rawFinding : result.findings()) {
            String rawSecret = rawFinding.secret() != null ? rawFinding.secret() : "";
            String match = rawFinding.match() != null ? rawFinding.match() : rawSecret;

            SecretMatch secretMatch = new SecretMatch(
                rawSecret,
                rawFinding.ruleID(),
                rawFinding.startLine(),
                rawFinding.endLine(),
                rawFinding.startColumn(),
                rawFinding.endColumn(),
                match
            );

            RedactedEvidence evidence = redactionService.buildRedactedEvidence(repositoryId, secretMatch);

            DetectedSecretFinding finding = new DetectedSecretFinding(
                rawFinding.ruleID(),
                rawFinding.file(),
                rawFinding.startLine(),
                rawFinding.endLine(),
                rawFinding.startColumn(),
                rawFinding.endColumn(),
                rawFinding.commit(),
                rawFinding.author(),
                rawFinding.date(),
                evidence
            );

            normalizedList.add(finding);
            detectedRuleIds.add(rawFinding.ruleID());
        }

        // Safe operational logging: rules and count only, zero raw secret exposure
        log.info("Successfully scanned and normalized {} secret findings for repositoryId={} with rules={}",
            normalizedList.size(), repositoryId, detectedRuleIds);

        return normalizedList;
    }

    /**
     * Parses a Gitleaks JSON report file into a list of GitleaksRawFinding records.
     */
    public List<GitleaksRawFinding> parseJsonReport(Path jsonPath) throws IOException {
        byte[] content = Files.readAllBytes(jsonPath);
        return parseJsonReport(new String(content, StandardCharsets.UTF_8));
    }

    /**
     * Parses a Gitleaks JSON report string into a list of GitleaksRawFinding records.
     */
    public List<GitleaksRawFinding> parseJsonReport(String jsonContent) throws IOException {
        if (jsonContent == null || jsonContent.isBlank()) {
            return Collections.emptyList();
        }
        return objectMapper.readValue(jsonContent, new TypeReference<List<GitleaksRawFinding>>() {});
    }

    private Path prepareTrustedConfigFile() throws IOException {
        Path tempConfigFile = Files.createTempFile("sp-config-001-", ".toml");
        Files.writeString(tempConfigFile, getPolicyContent(), StandardCharsets.UTF_8);
        return tempConfigFile;
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private record SpConfigRule(
        String ruleId,
        String description,
        Pattern pattern,
        int secretGroupIndex
    ) {}
}
