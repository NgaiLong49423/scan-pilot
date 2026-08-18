package com.scanpilot.scanner.classifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Engine for determining repository file eligibility and computing coverage metrics
 * according to Scan Pilot policy (FR-031, FR-034, FR-035, FR-037).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileEligibilityEngine {

    /**
     * Continuous monitoring file size limit (10 MiB = 10,485,760 bytes) per FR-037.
     */
    public static final long CONTINUOUS_MONITORING_LIMIT_BYTES = 10L * 1024 * 1024; // 10,485,760 bytes

    /**
     * Release assessment file size ceiling (50 MiB = 52,428,800 bytes) per FR-037.
     */
    public static final long RELEASE_ASSESSMENT_CEILING_BYTES = 50L * 1024 * 1024; // 52,428,800 bytes

    private final ContentClassifierService classifierService;

    /**
     * Evaluates eligibility for a file given its classification result and size.
     */
    public CoverageItem evaluate(String path, long sizeBytes, ClassificationResult classificationResult, ScanMode scanMode) {
        if (classificationResult == null) {
            classificationResult = ClassificationResult.undetermined("No classification result provided");
        }

        ContentClassification classification = classificationResult.classification();

        switch (classification) {
            case BINARY -> {
                if (classificationResult.isBinaryDocument()) {
                    return CoverageItem.builder()
                        .path(path)
                        .classification(ContentClassification.BINARY)
                        .sizeBytes(sizeBytes)
                        .status(CoverageStatus.SKIPPED)
                        .reasonCode(SkipReasonCode.UNSUPPORTED_BINARY_DOCUMENT)
                        .impact(CoverageImpact.COMPLETE)
                        .details(classificationResult.detail() != null ? classificationResult.detail() : "Unsupported binary document")
                        .build();
                } else {
                    return CoverageItem.builder()
                        .path(path)
                        .classification(ContentClassification.BINARY)
                        .sizeBytes(sizeBytes)
                        .status(CoverageStatus.SKIPPED)
                        .reasonCode(SkipReasonCode.UNSUPPORTED_BINARY_FILE)
                        .impact(CoverageImpact.COMPLETE)
                        .details(classificationResult.detail() != null ? classificationResult.detail() : "Unsupported binary file")
                        .build();
                }
            }
            case UNDETERMINED -> {
                return CoverageItem.builder()
                    .path(path)
                    .classification(ContentClassification.UNDETERMINED)
                    .sizeBytes(sizeBytes)
                    .status(CoverageStatus.SKIPPED)
                    .reasonCode(SkipReasonCode.UNDETERMINED_CONTENT_POLICY_SKIP)
                    .impact(CoverageImpact.PARTIAL)
                    .details(classificationResult.detail() != null ? classificationResult.detail() : "Undetermined content skipped per policy")
                    .build();
            }
            case TEXT -> {
                if (scanMode == ScanMode.CONTINUOUS_MONITORING) {
                    if (sizeBytes > CONTINUOUS_MONITORING_LIMIT_BYTES) {
                        return CoverageItem.builder()
                            .path(path)
                            .classification(ContentClassification.TEXT)
                            .sizeBytes(sizeBytes)
                            .status(CoverageStatus.SKIPPED)
                            .reasonCode(SkipReasonCode.MONITORING_FILE_SIZE_LIMIT_EXCEEDED)
                            .impact(CoverageImpact.PARTIAL)
                            .details(String.format("File size (%d bytes) exceeds continuous monitoring limit of %d bytes (10 MiB)",
                                sizeBytes, CONTINUOUS_MONITORING_LIMIT_BYTES))
                            .build();
                    } else {
                        return CoverageItem.builder()
                            .path(path)
                            .classification(ContentClassification.TEXT)
                            .sizeBytes(sizeBytes)
                            .status(CoverageStatus.SCANNED)
                            .reasonCode(null)
                            .impact(CoverageImpact.COMPLETE)
                            .details("Eligible text content scanned")
                            .build();
                    }
                } else { // RELEASE_ASSESSMENT
                    if (sizeBytes > RELEASE_ASSESSMENT_CEILING_BYTES) {
                        return CoverageItem.builder()
                            .path(path)
                            .classification(ContentClassification.TEXT)
                            .sizeBytes(sizeBytes)
                            .status(CoverageStatus.SKIPPED)
                            .reasonCode(SkipReasonCode.RELEASE_FILE_SIZE_CEILING_EXCEEDED)
                            .impact(CoverageImpact.INCOMPLETE)
                            .details(String.format("File size (%d bytes) exceeds release assessment ceiling of %d bytes (50 MiB)",
                                sizeBytes, RELEASE_ASSESSMENT_CEILING_BYTES))
                            .build();
                    } else {
                        return CoverageItem.builder()
                            .path(path)
                            .classification(ContentClassification.TEXT)
                            .sizeBytes(sizeBytes)
                            .status(CoverageStatus.SCANNED)
                            .reasonCode(null)
                            .impact(CoverageImpact.COMPLETE)
                            .details("Eligible text content scanned for release assessment")
                            .build();
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected classification: " + classification);
        }
    }

    /**
     * Evaluates a file given its path and byte content.
     */
    public CoverageItem evaluate(String path, byte[] content, ScanMode scanMode) {
        long size = content != null ? content.length : 0L;
        ClassificationResult result = classifierService.classify(path, content);
        return evaluate(path, size, result, scanMode);
    }

    /**
     * Evaluates a file on disk.
     */
    public CoverageItem evaluate(Path filePath, ScanMode scanMode) throws IOException {
        String pathStr = filePath.toString();
        if (!Files.exists(filePath)) {
            return CoverageItem.builder()
                .path(pathStr)
                .classification(ContentClassification.UNDETERMINED)
                .sizeBytes(0L)
                .status(CoverageStatus.SKIPPED)
                .reasonCode(SkipReasonCode.UNDETERMINED_CONTENT_POLICY_SKIP)
                .impact(CoverageImpact.PARTIAL)
                .details("File does not exist: " + filePath)
                .build();
        }
        long sizeBytes = Files.size(filePath);
        ClassificationResult result = classifierService.classify(filePath);
        return evaluate(pathStr, sizeBytes, result, scanMode);
    }

    /**
     * Evaluates a special repository object (submodule, broken symlink, etc.).
     */
    public CoverageItem evaluateSpecialObject(String path, long sizeBytes, String detail, ScanMode scanMode) {
        return CoverageItem.builder()
            .path(path)
            .classification(ContentClassification.UNDETERMINED)
            .sizeBytes(sizeBytes)
            .status(CoverageStatus.SKIPPED)
            .reasonCode(SkipReasonCode.UNSUPPORTED_SPECIAL_OBJECT)
            .impact(CoverageImpact.COMPLETE)
            .details(detail != null ? detail : "Unsupported special repository object")
            .build();
    }

    /**
     * Summarizes coverage across a list of evaluated coverage items.
     */
    public CoverageSummary summarize(List<CoverageItem> items) {
        if (items == null || items.isEmpty()) {
            return CoverageSummary.builder()
                .totalFiles(0)
                .scannedFiles(0)
                .skippedFiles(0)
                .textFiles(0)
                .binaryFiles(0)
                .undeterminedFiles(0)
                .totalBytes(0L)
                .coverageImpact(CoverageImpact.COMPLETE)
                .items(Collections.emptyList())
                .build();
        }

        int totalFiles = items.size();
        int scannedFiles = 0;
        int skippedFiles = 0;
        int textFiles = 0;
        int binaryFiles = 0;
        int undeterminedFiles = 0;
        long totalBytes = 0L;

        boolean hasIncomplete = false;
        boolean hasPartial = false;

        for (CoverageItem item : items) {
            totalBytes += item.sizeBytes();

            if (item.status() == CoverageStatus.SCANNED) {
                scannedFiles++;
            } else if (item.status() == CoverageStatus.SKIPPED) {
                skippedFiles++;
            }

            if (item.classification() == ContentClassification.TEXT) {
                textFiles++;
            } else if (item.classification() == ContentClassification.BINARY) {
                binaryFiles++;
            } else if (item.classification() == ContentClassification.UNDETERMINED) {
                undeterminedFiles++;
            }

            if (item.impact() == CoverageImpact.INCOMPLETE || item.reasonCode() == SkipReasonCode.RELEASE_FILE_SIZE_CEILING_EXCEEDED) {
                hasIncomplete = true;
            } else if (item.impact() == CoverageImpact.PARTIAL
                || item.reasonCode() == SkipReasonCode.MONITORING_FILE_SIZE_LIMIT_EXCEEDED
                || item.reasonCode() == SkipReasonCode.UNDETERMINED_CONTENT_POLICY_SKIP) {
                hasPartial = true;
            }
        }

        CoverageImpact overallImpact;
        if (hasIncomplete) {
            overallImpact = CoverageImpact.INCOMPLETE;
        } else if (hasPartial) {
            overallImpact = CoverageImpact.PARTIAL;
        } else {
            overallImpact = CoverageImpact.COMPLETE;
        }

        return CoverageSummary.builder()
            .totalFiles(totalFiles)
            .scannedFiles(scannedFiles)
            .skippedFiles(skippedFiles)
            .textFiles(textFiles)
            .binaryFiles(binaryFiles)
            .undeterminedFiles(undeterminedFiles)
            .totalBytes(totalBytes)
            .coverageImpact(overallImpact)
            .items(new ArrayList<>(items))
            .build();
    }
}
