package com.scanpilot.scanner.dto;

import com.scanpilot.persistence.entity.FindingLocationEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing an observed location for a finding.
 */
public record FindingLocationDto(
    UUID id,
    String filePath,
    Integer startLine,
    Integer endLine,
    Integer startColumn,
    Integer endColumn,
    String commitSha,
    String author,
    Boolean isCurrentHead,
    Instant detectedAt
) {
    public static FindingLocationDto from(FindingLocationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FindingLocationDto(
            entity.getId(),
            entity.getFilePath(),
            entity.getStartLine(),
            entity.getEndLine(),
            entity.getStartColumn(),
            entity.getEndColumn(),
            entity.getCommitSha(),
            entity.getAuthor(),
            entity.getIsCurrentHead(),
            entity.getDetectedAt()
        );
    }
}
