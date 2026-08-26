package com.scanpilot.scanner.dto;

import java.util.List;
import java.util.UUID;

public record ScanEventsResponse(
    UUID jobId,
    String status,
    String stage,
    long lastSequence,
    boolean hasMore,
    List<ScanEventDto> events
) {}
