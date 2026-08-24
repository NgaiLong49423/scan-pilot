package com.scanpilot.scanner.config;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Encapsulates the unique JVM worker instance identity (FR-002, Issue #52).
 * Constant across the lifetime of this JVM process.
 */
@Component
public class ScanWorkerInstance {

    public static final String INSTANCE_ID = UUID.randomUUID().toString();

    public String getInstanceId() {
        return INSTANCE_ID;
    }
}
