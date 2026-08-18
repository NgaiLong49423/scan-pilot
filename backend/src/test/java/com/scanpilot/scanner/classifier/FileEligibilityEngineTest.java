package com.scanpilot.scanner.classifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileEligibilityEngineTest {

    private ContentClassifierService classifierService;
    private FileEligibilityEngine eligibilityEngine;

    @BeforeEach
    void setUp() {
        classifierService = new ContentClassifierService();
        eligibilityEngine = new FileEligibilityEngine(classifierService);
    }

    @Nested
    @DisplayName("Continuous Monitoring Size Boundary (10 MiB / FR-037)")
    class ContinuousMonitoringSizeTests {

        private final ClassificationResult textResult = ClassificationResult.text("text/x-java-source", 1.0, "Java code");

        @Test
        @DisplayName("10 MiB - 1 byte is SCANNED in Continuous Monitoring")
        void testBelow10MiB() {
            long size = FileEligibilityEngine.CONTINUOUS_MONITORING_LIMIT_BYTES - 1; // 10,485,759 bytes
            CoverageItem item = eligibilityEngine.evaluate("src/Large1.java", size, textResult, ScanMode.CONTINUOUS_MONITORING);

            assertThat(item.status()).isEqualTo(CoverageStatus.SCANNED);
            assertThat(item.reasonCode()).isNull();
            assertThat(item.impact()).isEqualTo(CoverageImpact.COMPLETE);
            assertThat(item.classification()).isEqualTo(ContentClassification.TEXT);
        }

        @Test
        @DisplayName("Exactly 10 MiB (10,485,760 bytes) is SCANNED in Continuous Monitoring")
        void testExact10MiB() {
            long size = FileEligibilityEngine.CONTINUOUS_MONITORING_LIMIT_BYTES; // 10,485,760 bytes
            CoverageItem item = eligibilityEngine.evaluate("src/Large2.java", size, textResult, ScanMode.CONTINUOUS_MONITORING);

            assertThat(item.status()).isEqualTo(CoverageStatus.SCANNED);
            assertThat(item.reasonCode()).isNull();
            assertThat(item.impact()).isEqualTo(CoverageImpact.COMPLETE);
        }

        @Test
        @DisplayName("10 MiB + 1 byte is SKIPPED with MONITORING_FILE_SIZE_LIMIT_EXCEEDED")
        void testAbove10MiB() {
            long size = FileEligibilityEngine.CONTINUOUS_MONITORING_LIMIT_BYTES + 1; // 10,485,761 bytes
            CoverageItem item = eligibilityEngine.evaluate("src/Huge.java", size, textResult, ScanMode.CONTINUOUS_MONITORING);

            assertThat(item.status()).isEqualTo(CoverageStatus.SKIPPED);
            assertThat(item.reasonCode()).isEqualTo(SkipReasonCode.MONITORING_FILE_SIZE_LIMIT_EXCEEDED);
            assertThat(item.impact()).isEqualTo(CoverageImpact.PARTIAL);
        }
    }

    @Nested
    @DisplayName("Release Assessment Size Boundary (50 MiB / FR-037)")
    class ReleaseAssessmentSizeTests {

        private final ClassificationResult textResult = ClassificationResult.text("text/plain", 1.0, "Plain text");

        @Test
        @DisplayName("50 MiB - 1 byte is SCANNED in Release Assessment")
        void testBelow50MiB() {
            long size = FileEligibilityEngine.RELEASE_ASSESSMENT_CEILING_BYTES - 1; // 52,428,799 bytes
            CoverageItem item = eligibilityEngine.evaluate("data/dump1.sql", size, textResult, ScanMode.RELEASE_ASSESSMENT);

            assertThat(item.status()).isEqualTo(CoverageStatus.SCANNED);
            assertThat(item.reasonCode()).isNull();
            assertThat(item.impact()).isEqualTo(CoverageImpact.COMPLETE);
        }

        @Test
        @DisplayName("Exactly 50 MiB (52,428,800 bytes) is SCANNED in Release Assessment")
        void testExact50MiB() {
            long size = FileEligibilityEngine.RELEASE_ASSESSMENT_CEILING_BYTES; // 52,428,800 bytes
            CoverageItem item = eligibilityEngine.evaluate("data/dump2.sql", size, textResult, ScanMode.RELEASE_ASSESSMENT);

            assertThat(item.status()).isEqualTo(CoverageStatus.SCANNED);
            assertThat(item.reasonCode()).isNull();
            assertThat(item.impact()).isEqualTo(CoverageImpact.COMPLETE);
        }

        @Test
        @DisplayName("50 MiB + 1 byte is SKIPPED with RELEASE_FILE_SIZE_CEILING_EXCEEDED and INCOMPLETE impact")
        void testAbove50MiB() {
            long size = FileEligibilityEngine.RELEASE_ASSESSMENT_CEILING_BYTES + 1; // 52,428,801 bytes
            CoverageItem item = eligibilityEngine.evaluate("data/massive.sql", size, textResult, ScanMode.RELEASE_ASSESSMENT);

            assertThat(item.status()).isEqualTo(CoverageStatus.SKIPPED);
            assertThat(item.reasonCode()).isEqualTo(SkipReasonCode.RELEASE_FILE_SIZE_CEILING_EXCEEDED);
            assertThat(item.impact()).isEqualTo(CoverageImpact.INCOMPLETE);
        }
    }

    @Nested
    @DisplayName("Binary Documents and Binary Files (FR-034)")
    class BinaryPolicyTests {

        @Test
        @DisplayName("Binary documents (PDF, DOCX, XLSX, PPTX) are SKIPPED with UNSUPPORTED_BINARY_DOCUMENT")
        void testBinaryDocuments() {
            ClassificationResult pdfResult = ClassificationResult.binary("application/pdf", true, 1.0, "PDF document");
            CoverageItem pdfItem = eligibilityEngine.evaluate("docs/spec.pdf", 2048L, pdfResult, ScanMode.CONTINUOUS_MONITORING);

            assertThat(pdfItem.status()).isEqualTo(CoverageStatus.SKIPPED);
            assertThat(pdfItem.reasonCode()).isEqualTo(SkipReasonCode.UNSUPPORTED_BINARY_DOCUMENT);
            assertThat(pdfItem.impact()).isEqualTo(CoverageImpact.COMPLETE);

            ClassificationResult docxResult = ClassificationResult.binary("application/vnd.openxmlformats", true, 1.0, "DOCX document");
            CoverageItem docxItem = eligibilityEngine.evaluate("docs/notes.docx", 4096L, docxResult, ScanMode.RELEASE_ASSESSMENT);

            assertThat(docxItem.status()).isEqualTo(CoverageStatus.SKIPPED);
            assertThat(docxItem.reasonCode()).isEqualTo(SkipReasonCode.UNSUPPORTED_BINARY_DOCUMENT);
            assertThat(docxItem.impact()).isEqualTo(CoverageImpact.COMPLETE);
        }

        @Test
        @DisplayName("General binary files (PNG, EXE, ZIP) are SKIPPED with UNSUPPORTED_BINARY_FILE")
        void testGeneralBinaryFiles() {
            ClassificationResult imageResult = ClassificationResult.binary("image/png", false, 1.0, "PNG image");
            CoverageItem imgItem = eligibilityEngine.evaluate("assets/logo.png", 1024L, imageResult, ScanMode.CONTINUOUS_MONITORING);

            assertThat(imgItem.status()).isEqualTo(CoverageStatus.SKIPPED);
            assertThat(imgItem.reasonCode()).isEqualTo(SkipReasonCode.UNSUPPORTED_BINARY_FILE);
            assertThat(imgItem.impact()).isEqualTo(CoverageImpact.COMPLETE);

            ClassificationResult zipResult = ClassificationResult.binary("application/zip", false, 1.0, "ZIP archive");
            CoverageItem zipItem = eligibilityEngine.evaluate("build/app.zip", 8192L, zipResult, ScanMode.RELEASE_ASSESSMENT);

            assertThat(zipItem.status()).isEqualTo(CoverageStatus.SKIPPED);
            assertThat(zipItem.reasonCode()).isEqualTo(SkipReasonCode.UNSUPPORTED_BINARY_FILE);
        }

        @Test
        @DisplayName("Undetermined content is SKIPPED with UNDETERMINED_CONTENT_POLICY_SKIP and PARTIAL impact")
        void testUndeterminedContent() {
            ClassificationResult undeterminedResult = ClassificationResult.undetermined("Ambiguous stream");
            CoverageItem item = eligibilityEngine.evaluate("blob.raw", 512L, undeterminedResult, ScanMode.CONTINUOUS_MONITORING);

            assertThat(item.status()).isEqualTo(CoverageStatus.SKIPPED);
            assertThat(item.reasonCode()).isEqualTo(SkipReasonCode.UNDETERMINED_CONTENT_POLICY_SKIP);
            assertThat(item.impact()).isEqualTo(CoverageImpact.PARTIAL);
        }

        @Test
        @DisplayName("Special objects (submodules, broken symlinks) are SKIPPED with UNSUPPORTED_SPECIAL_OBJECT")
        void testSpecialObjects() {
            CoverageItem item = eligibilityEngine.evaluateSpecialObject("external/submodule", 0L, "Git submodule", ScanMode.CONTINUOUS_MONITORING);

            assertThat(item.status()).isEqualTo(CoverageStatus.SKIPPED);
            assertThat(item.reasonCode()).isEqualTo(SkipReasonCode.UNSUPPORTED_SPECIAL_OBJECT);
            assertThat(item.impact()).isEqualTo(CoverageImpact.COMPLETE);
        }
    }

    @Nested
    @DisplayName("Direct Content and File Evaluation")
    class DirectEvaluationTests {

        @Test
        @DisplayName("Evaluates from byte array directly")
        void testEvaluateFromBytes() {
            byte[] javaBytes = "public class Hello {}".getBytes();
            CoverageItem item = eligibilityEngine.evaluate("Hello.java", javaBytes, ScanMode.CONTINUOUS_MONITORING);

            assertThat(item.status()).isEqualTo(CoverageStatus.SCANNED);
            assertThat(item.classification()).isEqualTo(ContentClassification.TEXT);
            assertThat(item.sizeBytes()).isEqualTo(javaBytes.length);
        }

        @Test
        @DisplayName("Evaluates from Path on disk")
        void testClassifiesFromDisk(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("App.java");
            Files.writeString(file, "class App {}");

            CoverageItem item = eligibilityEngine.evaluate(file, ScanMode.CONTINUOUS_MONITORING);
            assertThat(item.status()).isEqualTo(CoverageStatus.SCANNED);
            assertThat(item.classification()).isEqualTo(ContentClassification.TEXT);
        }
    }

    @Nested
    @DisplayName("Coverage Summary and Impact Aggregation")
    class CoverageSummaryTests {

        @Test
        @DisplayName("Empty file list returns COMPLETE summary with zero counts")
        void testEmptySummary() {
            CoverageSummary summary = eligibilityEngine.summarize(List.of());
            assertThat(summary.totalFiles()).isZero();
            assertThat(summary.scannedFiles()).isZero();
            assertThat(summary.skippedFiles()).isZero();
            assertThat(summary.coverageImpact()).isEqualTo(CoverageImpact.COMPLETE);
        }

        @Test
        @DisplayName("All eligible text scanned returns COMPLETE impact")
        void testAllTextScanned() {
            List<CoverageItem> items = List.of(
                CoverageItem.builder().path("A.java").classification(ContentClassification.TEXT).sizeBytes(100).status(CoverageStatus.SCANNED).impact(CoverageImpact.COMPLETE).build(),
                CoverageItem.builder().path("B.yml").classification(ContentClassification.TEXT).sizeBytes(200).status(CoverageStatus.SCANNED).impact(CoverageImpact.COMPLETE).build(),
                CoverageItem.builder().path("logo.png").classification(ContentClassification.BINARY).sizeBytes(500).status(CoverageStatus.SKIPPED).reasonCode(SkipReasonCode.UNSUPPORTED_BINARY_FILE).impact(CoverageImpact.COMPLETE).build(),
                CoverageItem.builder().path("doc.pdf").classification(ContentClassification.BINARY).sizeBytes(800).status(CoverageStatus.SKIPPED).reasonCode(SkipReasonCode.UNSUPPORTED_BINARY_DOCUMENT).impact(CoverageImpact.COMPLETE).build()
            );

            CoverageSummary summary = eligibilityEngine.summarize(items);

            assertThat(summary.totalFiles()).isEqualTo(4);
            assertThat(summary.scannedFiles()).isEqualTo(2);
            assertThat(summary.skippedFiles()).isEqualTo(2);
            assertThat(summary.textFiles()).isEqualTo(2);
            assertThat(summary.binaryFiles()).isEqualTo(2);
            assertThat(summary.undeterminedFiles()).isZero();
            assertThat(summary.totalBytes()).isEqualTo(1600L);
            assertThat(summary.coverageImpact()).isEqualTo(CoverageImpact.COMPLETE);
        }

        @Test
        @DisplayName("Continuous file limit exceeded or undetermined files produce PARTIAL impact")
        void testPartialImpact() {
            List<CoverageItem> items = List.of(
                CoverageItem.builder().path("A.java").classification(ContentClassification.TEXT).sizeBytes(100).status(CoverageStatus.SCANNED).impact(CoverageImpact.COMPLETE).build(),
                CoverageItem.builder().path("Huge.txt").classification(ContentClassification.TEXT).sizeBytes(20_000_000).status(CoverageStatus.SKIPPED).reasonCode(SkipReasonCode.MONITORING_FILE_SIZE_LIMIT_EXCEEDED).impact(CoverageImpact.PARTIAL).build()
            );

            CoverageSummary summary = eligibilityEngine.summarize(items);
            assertThat(summary.coverageImpact()).isEqualTo(CoverageImpact.PARTIAL);
            assertThat(summary.scannedFiles()).isEqualTo(1);
            assertThat(summary.skippedFiles()).isEqualTo(1);
        }

        @Test
        @DisplayName("Release ceiling exceeded produces INCOMPLETE impact")
        void testIncompleteImpact() {
            List<CoverageItem> items = List.of(
                CoverageItem.builder().path("A.java").classification(ContentClassification.TEXT).sizeBytes(100).status(CoverageStatus.SCANNED).impact(CoverageImpact.COMPLETE).build(),
                CoverageItem.builder().path("Huge.txt").classification(ContentClassification.TEXT).sizeBytes(60_000_000).status(CoverageStatus.SKIPPED).reasonCode(SkipReasonCode.RELEASE_FILE_SIZE_CEILING_EXCEEDED).impact(CoverageImpact.INCOMPLETE).build()
            );

            CoverageSummary summary = eligibilityEngine.summarize(items);
            assertThat(summary.coverageImpact()).isEqualTo(CoverageImpact.INCOMPLETE);
        }
    }
}
