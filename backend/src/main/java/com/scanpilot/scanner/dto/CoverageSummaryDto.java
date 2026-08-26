package com.scanpilot.scanner.dto;

import com.scanpilot.persistence.entity.CoverageRecordEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing repository coverage summary and skipped files breakdown.
 */
public record CoverageSummaryDto(
    UUID id,
    UUID scanJobId,
    UUID repositoryId,
    String branchName,
    Integer totalFiles,
    Integer scannedFiles,
    Integer skippedFiles,
    Integer textFiles,
    Integer binaryFiles,
    Integer undeterminedFiles,
    Long totalBytes,
    String coverageImpact,
    String reasonCode,
    Long limitHitValue,
    Instant createdAt,
    List<CoverageItemDto> skippedItems,
    List<CoverageItemDto> items
) {
    public static CoverageSummaryDto from(CoverageRecordEntity record, List<CoverageItemDto> items) {
        if (record == null) {
            return null;
        }
        List<CoverageItemDto> allItems = items != null ? items : List.of();
        List<CoverageItemDto> skipped = allItems.stream()
            .filter(i -> "SKIPPED".equalsIgnoreCase(i.status()))
            .toList();

        return new CoverageSummaryDto(
            record.getId(),
            record.getScanJobId(),
            record.getRepositoryId(),
            record.getBranchName(),
            record.getTotalFiles(),
            record.getScannedFiles(),
            record.getSkippedFiles(),
            record.getTextFiles(),
            record.getBinaryFiles(),
            record.getUndeterminedFiles(),
            record.getTotalBytes(),
            record.getCoverageImpact(),
            record.getReasonCode(),
            record.getLimitHitValue(),
            record.getCreatedAt(),
            skipped,
            allItems
        );
    }
}
