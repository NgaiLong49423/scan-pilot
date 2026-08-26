package com.scanpilot.scanner.telemetry;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface ScanEventPayload permits
        ScanEventPayload.StageStartedPayload,
        ScanEventPayload.SnapshotFetchedPayload,
        ScanEventPayload.FilesClassifiedPayload,
        ScanEventPayload.ScannerActivePayload,
        ScanEventPayload.FindingAlertPayload,
        ScanEventPayload.FindingsTruncatedPayload,
        ScanEventPayload.GuardrailLimitHitPayload,
        ScanEventPayload.JobCompletedPayload,
        ScanEventPayload.JobFailedPayload {

    record StageStartedPayload(String stage) implements ScanEventPayload {}

    record SnapshotFetchedPayload(long archiveBytes, long workspaceBytes, int entryCount) implements ScanEventPayload {}

    record FilesClassifiedPayload(int eligibleFiles, int skippedFiles, int totalFiles) implements ScanEventPayload {}

    record ScannerActivePayload(String engine, String status, int timeoutSeconds) implements ScanEventPayload {}

    record FindingAlertPayload(String ruleId, String severity, int findingIndex) implements ScanEventPayload {}

    record FindingsTruncatedPayload(int totalFindings, int reportedFindings) implements ScanEventPayload {}

    record GuardrailLimitHitPayload(String reasonCode, long observedValue, long limitHitValue) implements ScanEventPayload {}

    record JobCompletedPayload(long durationMs, int findingsCount, String coverageImpact) implements ScanEventPayload {}

    record JobFailedPayload(String errorReason) implements ScanEventPayload {
        private static final Set<String> ALLOWED_REASONS = Set.of(
                "DISPATCH_CAPACITY_EXCEEDED",
                "UNEXPECTED_SCAN_FAILURE",
                "GUARDRAIL_EXCEEDED",
                "IO_ERROR",
                "AUTH_ERROR"
        );

        public JobFailedPayload(String errorReason) {
            this.errorReason = (errorReason != null && ALLOWED_REASONS.contains(errorReason))
                    ? errorReason
                    : "UNEXPECTED_SCAN_FAILURE";
        }
    }
}
