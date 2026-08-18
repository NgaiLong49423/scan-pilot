package com.scanpilot.scanner.dto;

import com.scanpilot.persistence.entity.CoverageItemEntity;

import java.util.UUID;

/**
 * DTO representing an evaluated file coverage breakdown item.
 */
public record CoverageItemDto(
    UUID id,
    String filePath,
    String classification,
    Long sizeBytes,
    String status,
    String reasonCode,
    String impact,
    String details
) {
    public static CoverageItemDto from(CoverageItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CoverageItemDto(
            entity.getId(),
            entity.getFilePath(),
            entity.getClassification(),
            entity.getSizeBytes(),
            entity.getStatus(),
            entity.getReasonCode(),
            entity.getImpact(),
            entity.getDetails()
        );
    }
}
