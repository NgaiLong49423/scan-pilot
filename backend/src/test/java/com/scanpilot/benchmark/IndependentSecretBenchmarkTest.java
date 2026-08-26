package com.scanpilot.benchmark;

import com.scanpilot.scanner.detector.gitleaks.GitleaksDetectorAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Independent Secret Detector Benchmark Tests (DEC-049, SP-CONFIG-001)")
class IndependentSecretBenchmarkTest {

    @Autowired
    private GitleaksDetectorAdapter gitleaksDetectorAdapter;

    private final SafeSecretBenchmarkSuite benchmarkSuite = new SafeSecretBenchmarkSuite();

    @Test
    @DisplayName("Executes full ground truth battery and asserts >= 95% Precision, Recall, F1, and Specificity")
    void shouldMeetBenchmarkAccuracyThresholds(@TempDir Path tempDir) throws IOException {
        SafeSecretBenchmarkSuite.BenchmarkMetrics metrics = benchmarkSuite.runBenchmark(gitleaksDetectorAdapter, tempDir);

        // 1. Minimum test battery size requirement (50+ cases)
        assertThat(metrics.totalCases())
            .as("Ground truth dataset must contain at least 50 test cases")
            .isGreaterThanOrEqualTo(50);

        assertThat(metrics.positiveCases())
            .as("Must contain substantial positive true secret cases")
            .isGreaterThanOrEqualTo(25);

        assertThat(metrics.negativeCases())
            .as("Must contain substantial negative benign noise cases")
            .isGreaterThanOrEqualTo(25);

        // 2. Statistical threshold requirements (>= 95.0%)
        assertThat(metrics.precision())
            .as("Detector precision must be >= 95%")
            .isGreaterThanOrEqualTo(0.95);

        assertThat(metrics.recall())
            .as("Detector recall must be >= 95%")
            .isGreaterThanOrEqualTo(0.95);

        assertThat(metrics.f1Score())
            .as("Detector F1-Score must be >= 95%")
            .isGreaterThanOrEqualTo(0.95);

        assertThat(metrics.specificity())
            .as("Detector specificity must be >= 95%")
            .isGreaterThanOrEqualTo(0.95);

        assertThat(metrics.accuracy())
            .as("Detector overall accuracy must be >= 95%")
            .isGreaterThanOrEqualTo(0.95);

        // 3. Perfect classification on synthetic baseline
        assertThat(metrics.falsePositives())
            .as("False positives should be 0 on curated synthetic dataset")
            .isZero();

        assertThat(metrics.falseNegatives())
            .as("False negatives should be 0 on curated synthetic dataset")
            .isZero();
    }

    @Test
    @DisplayName("Generates and persists formal benchmark report to docs/research/benchmarks/BENCHMARK-RESULTS-SP-CONFIG-001.md")
    void shouldGenerateAndSaveBenchmarkReport(@TempDir Path tempDir) throws IOException {
        SafeSecretBenchmarkSuite.BenchmarkMetrics metrics = benchmarkSuite.runBenchmark(gitleaksDetectorAdapter, tempDir);
        String policyDigest = gitleaksDetectorAdapter.getPolicyDigest();

        String reportMarkdown = benchmarkSuite.generateMarkdownReport(metrics, policyDigest);

        assertThat(reportMarkdown)
            .contains("# SP-CONFIG-001 Secret Detector Benchmark Results")
            .contains("Precision")
            .contains("Recall")
            .contains("F1-Score")
            .contains("Confusion Matrix")
            .contains("Detailed Test Case Evaluation Manifest")
            .contains(policyDigest);

        // Write/update benchmark markdown file in tempDir by default, or to docs if persist property is true
        Path reportPath = Boolean.getBoolean("scanpilot.benchmark.persist")
                ? resolveReportPath()
                : tempDir.resolve("BENCHMARK-RESULTS-SP-CONFIG-001.md");

        if (reportPath != null) {
            if (reportPath.getParent() != null) {
                Files.createDirectories(reportPath.getParent());
            }
            Files.writeString(reportPath, reportMarkdown, StandardCharsets.UTF_8);
            assertThat(Files.exists(reportPath)).isTrue();
            assertThat(Files.size(reportPath)).isGreaterThan(500);
        }
    }

    @Nested
    @DisplayName("Category Specific Validation Tests")
    class CategoryValidationTests {

        @Test
        @DisplayName("Validates Google API Key positive detection")
        void shouldDetectGoogleApiKeys(@TempDir Path tempDir) throws IOException {
            List<SafeSecretBenchmarkSuite.BenchmarkTestCase> googleCases = SafeSecretBenchmarkSuite.getGroundTruthDataset().stream()
                .filter(tc -> "Google API Key".equals(tc.category()))
                .toList();

            assertThat(googleCases).isNotEmpty();
            for (SafeSecretBenchmarkSuite.BenchmarkTestCase tc : googleCases) {
                Path f = tempDir.resolve("test_" + tc.id() + ".txt");
                Files.writeString(f, tc.content());
                var res = gitleaksDetectorAdapter.scan(com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest.forSnapshot(f));
                assertThat(res.findings())
                    .as("Expected positive match for " + tc.id())
                    .isNotEmpty();
                Files.deleteIfExists(f);
            }
        }

        @Test
        @DisplayName("Validates GitHub Token positive detection")
        void shouldDetectGitHubTokens(@TempDir Path tempDir) throws IOException {
            List<SafeSecretBenchmarkSuite.BenchmarkTestCase> githubCases = SafeSecretBenchmarkSuite.getGroundTruthDataset().stream()
                .filter(tc -> "GitHub Token".equals(tc.category()))
                .toList();

            assertThat(githubCases).isNotEmpty();
            for (SafeSecretBenchmarkSuite.BenchmarkTestCase tc : githubCases) {
                Path f = tempDir.resolve("test_" + tc.id() + ".txt");
                Files.writeString(f, tc.content());
                var res = gitleaksDetectorAdapter.scan(com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest.forSnapshot(f));
                assertThat(res.findings())
                    .as("Expected positive match for " + tc.id())
                    .isNotEmpty();
                Files.deleteIfExists(f);
            }
        }

        @Test
        @DisplayName("Validates AWS Access Key positive detection")
        void shouldDetectAwsAccessKeys(@TempDir Path tempDir) throws IOException {
            List<SafeSecretBenchmarkSuite.BenchmarkTestCase> awsCases = SafeSecretBenchmarkSuite.getGroundTruthDataset().stream()
                .filter(tc -> "AWS Access Key".equals(tc.category()))
                .toList();

            assertThat(awsCases).isNotEmpty();
            for (SafeSecretBenchmarkSuite.BenchmarkTestCase tc : awsCases) {
                Path f = tempDir.resolve("test_" + tc.id() + ".txt");
                Files.writeString(f, tc.content());
                var res = gitleaksDetectorAdapter.scan(com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest.forSnapshot(f));
                assertThat(res.findings())
                    .as("Expected positive match for " + tc.id())
                    .isNotEmpty();
                Files.deleteIfExists(f);
            }
        }

        @Test
        @DisplayName("Validates Private Key positive detection")
        void shouldDetectPrivateKeys(@TempDir Path tempDir) throws IOException {
            List<SafeSecretBenchmarkSuite.BenchmarkTestCase> pkCases = SafeSecretBenchmarkSuite.getGroundTruthDataset().stream()
                .filter(tc -> "Private Key".equals(tc.category()))
                .toList();

            assertThat(pkCases).isNotEmpty();
            for (SafeSecretBenchmarkSuite.BenchmarkTestCase tc : pkCases) {
                Path f = tempDir.resolve("test_" + tc.id() + ".txt");
                Files.writeString(f, tc.content());
                var res = gitleaksDetectorAdapter.scan(com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest.forSnapshot(f));
                assertThat(res.findings())
                    .as("Expected positive match for " + tc.id())
                    .isNotEmpty();
                Files.deleteIfExists(f);
            }
        }

        @Test
        @DisplayName("Validates Benign Noise negative rejection")
        void shouldRejectBenignNoise(@TempDir Path tempDir) throws IOException {
            List<SafeSecretBenchmarkSuite.BenchmarkTestCase> negativeCases = SafeSecretBenchmarkSuite.getGroundTruthDataset().stream()
                .filter(tc -> !tc.expectedSecret())
                .toList();

            assertThat(negativeCases).isNotEmpty();
            for (SafeSecretBenchmarkSuite.BenchmarkTestCase tc : negativeCases) {
                Path f = tempDir.resolve("test_" + tc.id() + ".txt");
                Files.writeString(f, tc.content());
                var res = gitleaksDetectorAdapter.scan(com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest.forSnapshot(f));
                assertThat(res.findings())
                    .as("Expected NO match for benign test case " + tc.id() + " [" + tc.category() + "]")
                    .isEmpty();
                Files.deleteIfExists(f);
            }
        }
    }

    private Path resolveReportPath() {
        Path p1 = Path.of("docs", "research", "benchmarks", "BENCHMARK-RESULTS-SP-CONFIG-001.md");
        if (Files.exists(p1.getParent())) {
            return p1;
        }
        Path p2 = Path.of("..", "docs", "research", "benchmarks", "BENCHMARK-RESULTS-SP-CONFIG-001.md");
        if (Files.exists(p2.getParent())) {
            return p2;
        }
        return p2;
    }
}
