package com.scanpilot.scanner.detector.gitleaks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.security.secret.RedactedEvidence;
import com.scanpilot.security.secret.SecretFingerprintService;
import com.scanpilot.security.secret.SecretRedactionService;
import com.scanpilot.security.secret.SecurityConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitleaksDetectorAdapterTest {

    private static final String TEST_HMAC_KEY = "test-insecure-hmac-key-for-unit-tests-32-chars-long";

    private GitleaksConfigProperties properties;
    private SecretRedactionService redactionService;
    private ObjectMapper objectMapper;
    private GitleaksDetectorAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new GitleaksConfigProperties();
        properties.setBinaryPath("nonexistent-gitleaks-binary");
        properties.setTimeoutSeconds(10);
        properties.setPolicyResourcePath("policies/sp-config-001-gitleaks.toml");

        SecurityConfigProperties securityProperties = new SecurityConfigProperties();
        securityProperties.setHmacSecretKey(TEST_HMAC_KEY);
        SecretFingerprintService fingerprintService = new SecretFingerprintService(securityProperties);
        redactionService = new SecretRedactionService(fingerprintService);
        objectMapper = new ObjectMapper();

        adapter = new GitleaksDetectorAdapter(properties, redactionService, objectMapper);
        adapter.init();
    }

    @Nested
    @DisplayName("Policy & Integrity Tests")
    class PolicyTests {

        @Test
        @DisplayName("Loads canonical SP-CONFIG-001 policy from classpath")
        void shouldLoadPolicyFromClasspath() {
            String policy = adapter.getPolicyContent();
            assertThat(policy).isNotBlank();
            assertThat(policy).contains("SP-CONFIG-001");
            assertThat(policy).contains("google-api-key");
            assertThat(policy).contains("github-pat");
            assertThat(policy).contains("aws-access-key");
            assertThat(policy).contains("private-key");
            assertThat(policy).contains("generic-api-key");
        }

        @Test
        @DisplayName("Computes deterministic 64-character SHA-256 policy digest")
        void shouldComputeDeterministicSha256Digest() {
            String digest1 = adapter.getPolicyDigest();
            String digest2 = adapter.getPolicyDigest();

            assertThat(digest1).isNotBlank();
            assertThat(digest1).hasSize(64);
            assertThat(digest1).matches("^[a-f0-9]{64}$");
            assertThat(digest1).isEqualTo(digest2);
        }

        @Test
        @DisplayName("Handles missing policy gracefully by using fallback")
        void shouldHandleMissingPolicyResourceGracefully() {
            properties.setPolicyResourcePath("nonexistent/path/policy.toml");
            adapter.loadPolicy();

            assertThat(adapter.getPolicyContent()).isNotBlank();
            assertThat(adapter.getPolicyDigest()).hasSize(64);
        }
    }

    @Nested
    @DisplayName("JSON Report Parsing Tests")
    class JsonParsingTests {

        @Test
        @DisplayName("Parses realistic Gitleaks JSON report into GitleaksRawFinding records")
        void shouldParseGitleaksJsonReport() throws IOException {
            String jsonReport = """
                [
                  {
                    "Description": "Google API Key",
                    "StartLine": 12,
                    "EndLine": 12,
                    "StartColumn": 18,
                    "EndColumn": 57,
                    "Match": "AIzaSyD-1234567890abcdef1234567890abcde",
                    "Secret": "AIzaSyD-1234567890abcdef1234567890abcde",
                    "File": "src/config.ts",
                    "SymlinkFile": "",
                    "Commit": "a1b2c3d4e5f6",
                    "Entropy": 3.75,
                    "Author": "Developer",
                    "Email": "dev@example.com",
                    "Date": "2026-08-18T10:00:00Z",
                    "Message": "Add config",
                    "Tags": ["api-key"],
                    "RuleID": "google-api-key",
                    "Fingerprint": "a1b2c3d4e5f6:src/config.ts:google-api-key:12"
                  },
                  {
                    "Description": "AWS Access Key ID",
                    "StartLine": 45,
                    "EndLine": 45,
                    "StartColumn": 15,
                    "EndColumn": 35,
                    "Match": "AKIAIOSFODNN7EXAMPLE",
                    "Secret": "AKIAIOSFODNN7EXAMPLE",
                    "File": "credentials.json",
                    "SymlinkFile": "",
                    "Commit": "b2c3d4e5f6a1",
                    "Entropy": 3.12,
                    "Author": "DevOps",
                    "Email": "ops@example.com",
                    "Date": "2026-08-18T11:00:00Z",
                    "Message": "Add AWS creds",
                    "Tags": ["aws"],
                    "RuleID": "aws-access-key",
                    "Fingerprint": "b2c3d4e5f6a1:credentials.json:aws-access-key:45"
                  }
                ]
                """;

            List<GitleaksRawFinding> findings = adapter.parseJsonReport(jsonReport);

            assertThat(findings).hasSize(2);

            GitleaksRawFinding first = findings.get(0);
            assertThat(first.ruleID()).isEqualTo("google-api-key");
            assertThat(first.description()).isEqualTo("Google API Key");
            assertThat(first.file()).isEqualTo("src/config.ts");
            assertThat(first.startLine()).isEqualTo(12);
            assertThat(first.endLine()).isEqualTo(12);
            assertThat(first.startColumn()).isEqualTo(18);
            assertThat(first.endColumn()).isEqualTo(57);
            assertThat(first.secret()).isEqualTo("AIzaSyD-1234567890abcdef1234567890abcde");
            assertThat(first.commit()).isEqualTo("a1b2c3d4e5f6");
            assertThat(first.author()).isEqualTo("Developer");
            assertThat(first.date()).isEqualTo("2026-08-18T10:00:00Z");

            GitleaksRawFinding second = findings.get(1);
            assertThat(second.ruleID()).isEqualTo("aws-access-key");
            assertThat(second.file()).isEqualTo("credentials.json");
            assertThat(second.startLine()).isEqualTo(45);
            assertThat(second.secret()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
        }

        @Test
        @DisplayName("Handles empty and blank JSON reports gracefully")
        void shouldHandleEmptyJsonReport() throws IOException {
            assertThat(adapter.parseJsonReport("")).isEmpty();
            assertThat(adapter.parseJsonReport("  ")).isEmpty();
            assertThat(adapter.parseJsonReport("[]")).isEmpty();
            assertThat(adapter.parseJsonReport((String) null)).isEmpty();
        }

        @Test
        @DisplayName("Parses report directly from Path")
        void shouldParseReportFromPath(@TempDir Path tempDir) throws IOException {
            Path reportFile = tempDir.resolve("sample-report.json");
            String jsonReport = """
                [
                  {
                    "RuleID": "github-pat",
                    "Description": "GitHub Token",
                    "StartLine": 5,
                    "EndLine": 5,
                    "StartColumn": 10,
                    "EndColumn": 50,
                    "Match": "ghp_123456789012345678901234567890123456",
                    "Secret": "ghp_123456789012345678901234567890123456",
                    "File": "token.env",
                    "Commit": "c3d4e5f6a1b2"
                  }
                ]
                """;
            Files.writeString(reportFile, jsonReport, StandardCharsets.UTF_8);

            List<GitleaksRawFinding> findings = adapter.parseJsonReport(reportFile);
            assertThat(findings).hasSize(1);
            assertThat(findings.get(0).ruleID()).isEqualTo("github-pat");
            assertThat(findings.get(0).secret()).isEqualTo("ghp_123456789012345678901234567890123456");
        }
    }

    @Nested
    @DisplayName("Snapshot Detection & Embedded Scanner Tests")
    class DetectionTests {

        @Test
        @DisplayName("Detects Google API Key, GitHub PAT, AWS Key, Private Key, and generic token")
        void shouldDetectAllCanonicalSpConfigSecrets(@TempDir Path tempDir) throws IOException {
            // Create test fixture with 4 canonical secrets
            Path testFile = tempDir.resolve("SecretsFixture.java");
            String fixtureContent = """
                package com.example;

                public class SecretsFixture {
                    // 1. Google API Key (39 chars)
                    private static final String GOOGLE_KEY = "AIzaSyDb-1234567890abcdef1234567890abcd";

                    // 2. GitHub PAT (classic)
                    private static final String GH_PAT = "ghp_123456789012345678901234567890123456";

                    // 3. AWS Access Key ID
                    private static final String AWS_KEY = "AKIAIOSFODNN7EXAMPLE";

                    // 4. PEM Private Key
                    private static final String PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----";

                    // 5. Generic API Key
                    private static final String API_SECRET = "apiKey = 'abcdef12345678901234567890abcdef'";
                }
                """;
            Files.writeString(testFile, fixtureContent, StandardCharsets.UTF_8);

            GitleaksScanRequest request = GitleaksScanRequest.forSnapshot(tempDir);
            GitleaksScanResult result = adapter.scan(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.exitCode()).isEqualTo(1); // 1 = findings detected
            assertThat(result.findings()).hasSizeGreaterThanOrEqualTo(5);

            List<String> ruleIds = result.findings().stream().map(GitleaksRawFinding::ruleID).toList();
            assertThat(ruleIds).contains(
                "google-api-key",
                "github-pat",
                "aws-access-key",
                "private-key",
                "generic-api-key"
            );

            // Verify lines
            GitleaksRawFinding googleFinding = result.findings().stream()
                .filter(f -> f.ruleID().equals("google-api-key"))
                .findFirst().orElseThrow();
            assertThat(googleFinding.startLine()).isEqualTo(5);
            assertThat(googleFinding.secret()).isEqualTo("AIzaSyDb-1234567890abcdef1234567890abcd");

            GitleaksRawFinding githubFinding = result.findings().stream()
                .filter(f -> f.ruleID().equals("github-pat"))
                .findFirst().orElseThrow();
            assertThat(githubFinding.startLine()).isEqualTo(8);
            assertThat(githubFinding.secret()).isEqualTo("ghp_123456789012345678901234567890123456");

            GitleaksRawFinding awsFinding = result.findings().stream()
                .filter(f -> f.ruleID().equals("aws-access-key"))
                .findFirst().orElseThrow();
            assertThat(awsFinding.startLine()).isEqualTo(11);
            assertThat(awsFinding.secret()).isEqualTo("AKIAIOSFODNN7EXAMPLE");

            GitleaksRawFinding privateKeyFinding = result.findings().stream()
                .filter(f -> f.ruleID().equals("private-key"))
                .findFirst().orElseThrow();
            assertThat(privateKeyFinding.startLine()).isEqualTo(14);
            assertThat(privateKeyFinding.secret()).isEqualTo("-----BEGIN RSA PRIVATE KEY-----");
        }

        @Test
        @DisplayName("Returns zero findings and exit code 0 on clean code directory")
        void shouldReturnCleanResultForCleanRepository(@TempDir Path tempDir) throws IOException {
            Path cleanFile = tempDir.resolve("CleanService.java");
            String cleanContent = """
                package com.example;

                public class CleanService {
                    public String hello(String name) {
                        return "Hello, " + name;
                    }
                }
                """;
            Files.writeString(cleanFile, cleanContent, StandardCharsets.UTF_8);

            GitleaksScanRequest request = GitleaksScanRequest.forSnapshot(tempDir);
            GitleaksScanResult result = adapter.scan(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.exitCode()).isEqualTo(0);
            assertThat(result.findings()).isEmpty();
        }

        @Test
        @DisplayName("Skips ignored directories (.git, node_modules, target)")
        void shouldSkipIgnoredDirectories(@TempDir Path tempDir) throws IOException {
            Path gitDir = Files.createDirectories(tempDir.resolve(".git"));
            Path gitSecretFile = gitDir.resolve("config");
            Files.writeString(gitSecretFile, "secret = AIzaSyDb-1234567890abcdef1234567890", StandardCharsets.UTF_8);

            Path nodeModules = Files.createDirectories(tempDir.resolve("node_modules"));
            Path nmSecretFile = nodeModules.resolve("package.json");
            Files.writeString(nmSecretFile, "AKIAIOSFODNN7EXAMPLE", StandardCharsets.UTF_8);

            GitleaksScanRequest request = GitleaksScanRequest.forSnapshot(tempDir);
            GitleaksScanResult result = adapter.scan(request);

            assertThat(result.findings()).isEmpty();
        }

        @Test
        @DisplayName("Gracefully handles non-existent scan target path")
        void shouldHandleNonExistentPath(@TempDir Path tempDir) {
            Path nonExistent = tempDir.resolve("non-existent-dir");
            GitleaksScanRequest request = GitleaksScanRequest.forSnapshot(nonExistent);

            GitleaksScanResult result = adapter.scan(request);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errorMessage()).contains("does not exist");
        }

        @Test
        @DisplayName("Fallback to embedded scanner works when binary check fails")
        void shouldFallbackToEmbeddedScannerWhenBinaryMissing(@TempDir Path tempDir) throws IOException {
            assertThat(adapter.isBinaryAvailable()).isFalse();

            Path file = tempDir.resolve("key.txt");
            Files.writeString(file, "AKIAIOSFODNN7EXAMPLE", StandardCharsets.UTF_8);

            GitleaksScanRequest request = GitleaksScanRequest.forSnapshot(tempDir);
            GitleaksScanResult result = adapter.scan(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.findings()).hasSize(1);
            assertThat(result.findings().get(0).ruleID()).isEqualTo("aws-access-key");
        }

        @Test
        @DisplayName("Creates and executes Git scan request structure")
        void shouldHandleGitScanRequest(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("key.txt");
            Files.writeString(file, "AKIAIOSFODNN7EXAMPLE", StandardCharsets.UTF_8);

            GitleaksScanRequest gitRequest = GitleaksScanRequest.forGitHistory(tempDir, "HEAD~1..HEAD");
            assertThat(gitRequest.isGitScan()).isTrue();
            assertThat(gitRequest.commitRange()).isEqualTo("HEAD~1..HEAD");

            GitleaksScanResult result = adapter.scan(gitRequest);
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Normalization & Redacted Evidence Pipeline Tests")
    class NormalizationTests {

        @Test
        @DisplayName("Transforms raw findings into safe DetectedSecretFinding with RedactedEvidence")
        void shouldScanAndNormalizeFindings(@TempDir Path tempDir) throws IOException {
            Path testFile = tempDir.resolve("AppConfig.java");
            String content = """
                package com.example;

                public class AppConfig {
                    private static final String GOOGLE_KEY = "AIzaSyDb-1234567890abcdef1234567890abcd";
                    private static final String GITHUB_PAT = "ghp_123456789012345678901234567890123456";
                    private static final String AWS_KEY = "AKIAIOSFODNN7EXAMPLE";
                }
                """;
            Files.writeString(testFile, content, StandardCharsets.UTF_8);

            String repoId = "repo-acme-prod-001";
            GitleaksScanRequest request = GitleaksScanRequest.forSnapshot(tempDir);

            List<DetectedSecretFinding> findings = adapter.scanAndNormalize(repoId, request);

            assertThat(findings).hasSize(3);

            for (DetectedSecretFinding finding : findings) {
                RedactedEvidence evidence = finding.redactedEvidence();
                assertThat(evidence).isNotNull();

                // 1. Secret must be masked with asterisks
                assertThat(evidence.maskedSecret()).contains("*");
                assertThat(evidence.maskedSecret()).doesNotContain("1234567890abcdef");

                // 2. Snippet must have raw secret replaced with [REDACTED_SECRET]
                assertThat(evidence.redactedSnippet()).contains("[REDACTED_SECRET]");

                // 3. Fingerprint must be 64-char HMAC-SHA-256
                assertThat(evidence.fingerprint()).hasSize(64);
                assertThat(evidence.fingerprint()).matches("^[a-f0-9]{64}$");

                // 4. Coordinates preserved
                assertThat(finding.startLine()).isGreaterThan(0);
                assertThat(finding.endLine()).isEqualTo(finding.startLine());
                assertThat(finding.file()).isEqualTo("AppConfig.java");
            }

            // Google Key specifics
            DetectedSecretFinding googleFinding = findings.stream()
                .filter(f -> f.ruleId().equals("google-api-key"))
                .findFirst().orElseThrow();
            assertThat(googleFinding.redactedEvidence().maskedSecret()).startsWith("AIzaSy").endsWith("abcd");


            // GitHub PAT specifics
            DetectedSecretFinding githubFinding = findings.stream()
                .filter(f -> f.ruleId().equals("github-pat"))
                .findFirst().orElseThrow();
            assertThat(githubFinding.redactedEvidence().maskedSecret()).startsWith("ghp_").endsWith("3456");

            // AWS Key specifics
            DetectedSecretFinding awsFinding = findings.stream()
                .filter(f -> f.ruleId().equals("aws-access-key"))
                .findFirst().orElseThrow();
            assertThat(awsFinding.redactedEvidence().maskedSecret()).startsWith("AKIA").endsWith("MPLE");
        }

        @Test
        @DisplayName("Returns empty list when scanning clean repository")
        void shouldReturnEmptyListForCleanRepo(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("Clean.java");
            Files.writeString(file, "public class Clean {}", StandardCharsets.UTF_8);

            List<DetectedSecretFinding> findings = adapter.scanAndNormalize("repo-123", GitleaksScanRequest.forSnapshot(tempDir));
            assertThat(findings).isEmpty();
        }
    }

    @Nested
    @DisplayName("Secure Report Lifecycle & Deletion Tests")
    class SecurityDeletionTests {

        @Test
        @DisplayName("Verifies temporary raw report and config files are deleted after binary execution attempt")
        void shouldEnsureTempReportDeletion(@TempDir Path tempDir) throws IOException {
            Path dummyFile = tempDir.resolve("sample.txt");
            Files.writeString(dummyFile, "clean content", StandardCharsets.UTF_8);

            // Execute scanWithBinary (which will fail to spawn non-existent binary or timeout, but execute finally block)
            GitleaksScanResult result = adapter.scanWithBinary(GitleaksScanRequest.forSnapshot(tempDir));

            // Verify error was caught and temp files are cleaned up
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errorMessage()).contains("Execution failure");
        }
    }
}
