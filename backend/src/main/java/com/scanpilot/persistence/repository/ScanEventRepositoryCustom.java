package com.scanpilot.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ScanEventRepositoryCustom {

    Optional<Long> insertEventAtomicCTE(
        UUID jobId,
        long maxLimit,
        UUID eventId,
        String stage,
        String eventType,
        String messageCode,
        String payloadJson,
        Instant createdAt
    );
}
