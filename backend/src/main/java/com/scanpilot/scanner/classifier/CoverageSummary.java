package com.scanpilot.scanner.classifier;

import lombok.Builder;

import java.util.List;

/**
 * Aggregated scan coverage summary for a repository scan execution.
 *
 * @param totalFiles        Total number of files considered.
 * @param scannedFiles      Number of files eligible and scanned.
 * @param skippedFiles      Number of files skipped.
 * @param textFiles         Number of text files.
 * @param binaryFiles       Number of binary files.
 * @param undeterminedFiles Number of undetermined files.
 * @param totalBytes        Total byte count across all considered files.
 * @param coverageImpact    Overall coverage impact (COMPLETE, PARTIAL, INCOMPLETE).
 * @param items             Detailed list of coverage items.
 */
@Builder
public record CoverageSummary(
    int totalFiles,
    int scannedFiles,
    int skippedFiles,
    int textFiles,
    int binaryFiles,
    int undeterminedFiles,
    long totalBytes,
    CoverageImpact coverageImpact,
    List<CoverageItem> items
) {}
