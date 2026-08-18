package com.scanpilot.scanner.classifier;

import lombok.Builder;

/**
 * Coverage record for an individual repository file.
 *
 * @param path           Repository-relative path of the file.
 * @param classification Content classification (TEXT, BINARY, UNDETERMINED).
 * @param sizeBytes      Size of the file in bytes.
 * @param status         Coverage status (CONSIDERED, SCANNED, SKIPPED).
 * @param reasonCode     Skip reason code when skipped (null when scanned).
 * @param impact         Coverage impact for this item.
 * @param details        Descriptive rationale or detail.
 */
@Builder
public record CoverageItem(
    String path,
    ContentClassification classification,
    long sizeBytes,
    CoverageStatus status,
    SkipReasonCode reasonCode,
    CoverageImpact impact,
    String details
) {}
