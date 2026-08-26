package com.scanpilot.scanner.exception;

import lombok.Getter;

/**
 * Exception thrown when a resource guardrail threshold is breached during scan execution
 * (e.g. archive size > 20 MiB, workspace size > 150 MiB, entry count > 10,000, detector timeout).
 */
@Getter
public class ResourceGuardrailExceededException extends RuntimeException {

    private final String reasonCode;
    private final long observedBytes;
    private final int observedFiles;
    private final long limitHitValue;

    public ResourceGuardrailExceededException(
            String reasonCode,
            long observedBytes,
            int observedFiles,
            long limitHitValue
    ) {
        super(String.format(
            "Resource guardrail exceeded: reasonCode=%s, observedBytes=%d, observedFiles=%d, limitHitValue=%d",
            reasonCode, observedBytes, observedFiles, limitHitValue
        ));
        this.reasonCode = reasonCode;
        this.observedBytes = observedBytes;
        this.observedFiles = observedFiles;
        this.limitHitValue = limitHitValue;
    }
}
