package com.scanpilot.scanner.dto;

import com.scanpilot.persistence.entity.ScanEventEntity;

import java.time.Instant;
import java.util.UUID;

public record ScanEventDto(
    UUID id,
    UUID scanJobId,
    long sequenceNumber,
    String stage,
    String eventType,
    String messageCode,
    String payloadJson,
    Instant createdAt
) {
    public static ScanEventDto from(ScanEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ScanEventDto(
            entity.getId(),
            entity.getScanJobId(),
            entity.getSequenceNumber(),
            entity.getStage(),
            entity.getEventType(),
            entity.getMessageCode(),
            entity.getPayloadJson(),
            entity.getCreatedAt()
        );
    }
}
