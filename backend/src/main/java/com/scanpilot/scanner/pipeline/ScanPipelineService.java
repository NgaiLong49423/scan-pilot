package com.scanpilot.scanner.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.persistence.entity.CoverageItemEntity;
import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.ScanCheckpointEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.ScanCheckpointRepository;
import com.scanpilot.persistence.repository.ScanEventRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.scanner.classifier.CoverageImpact;
import com.scanpilot.scanner.classifier.CoverageItem;
import com.scanpilot.scanner.classifier.CoverageSummary;
import com.scanpilot.scanner.classifier.FileEligibilityEngine;
import com.scanpilot.scanner.classifier.ScanMode;
import com.scanpilot.scanner.config.SnapshotGuardrailProperties;
import com.scanpilot.scanner.detector.gitleaks.DetectedSecretFinding;
import com.scanpilot.scanner.detector.gitleaks.GitleaksDetectorAdapter;
import com.scanpilot.scanner.detector.gitleaks.GitleaksRawFinding;
import com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest;
import com.scanpilot.scanner.detector.gitleaks.GitleaksScanResult;
import com.scanpilot.scanner.exception.ResourceGuardrailExceededException;
import com.scanpilot.scanner.git.GitCloneService;
import com.scanpilot.scanner.lifecycle.FindingLifecycle;
import com.scanpilot.scanner.lifecycle.FindingLifecycleEngine;
import com.scanpilot.scanner.lifecycle.FindingLifecycleResult;
import com.scanpilot.scanner.workspace.GitWorkspace;
import com.scanpilot.scanner.workspace.GitWorkspaceManager;
import com.scanpilot.security.secret.RedactedEvidence;
import com.scanpilot.security.secret.SecretMatch;
import com.scanpilot.security.secret.SecretRedactionService;
import com.scanpilot.scanner.telemetry.ScanEventPayload;
import com.scanpilot.scanner.telemetry.TelemetryPayloadSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Orchestrator service for executing Snapshot and Git History Scan Pipelines
 * with Finding Lifecycle tracking and coverage recording (FR-007, FR-018, FR-019,
 * FR-025, FR-028, FR-029, FR-051, DEC-012, DEC-015).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanPipelineService {

    private final GitWorkspaceManager gitWorkspaceManager;
    private final FileEligibilityEngine fileEligibilityEngine;
    private final GitleaksDetectorAdapter gitleaksDetectorAdapter;
    private final SecretRedactionService secretRedactionService;
    private final FindingLifecycleEngine findingLifecycleEngine;
    private final StreamedSnapshotFetcher streamedSnapshotFetcher;
    private final SnapshotGuardrailProperties snapshotGuardrailProperties;
    private final GitCloneService gitCloneService;

    private final ScanJobRepository scanJobRepository;
    private final ScanEventRepository scanEventRepository;
    private final ScanCheckpointRepository scanCheckpointRepository;
    private final FindingRepository findingRepository;
    private final FindingLocationRepository findingLocationRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final CoverageRecordRepository coverageRecordRepository;
    private final CoverageItemRepository coverageItemRepository;
    private final com.scanpilot.persistence.repository.RepositoryRepository repositoryRepository;
    private final com.scanpilot.persistence.repository.UserSessionRepository userSessionRepository;
    private final TelemetryPayloadSerializer telemetryPayloadSerializer;

    /**
     * Executes an enqueued scan job asynchronously by ID, persisting monotonic real stages.
     *
     * @param jobId the scan job UUID
     * @return completed or failed ScanJobEntity
     */
    public ScanJobEntity executeScanJob(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("Scan job ID is required");
        }

        ScanJobEntity scanJob = scanJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scan job not found: " + jobId));

        UUID repositoryId = scanJob.getRepositoryId();
        String branch = (scanJob.getBranchName() != null && !scanJob.getBranchName().isBlank())
                ? scanJob.getBranchName().trim()
                : "main";

        Instant startTime = Instant.now();
        int maxTimeoutSeconds = snapshotGuardrailProperties.getMaxScanTimeoutSeconds();
        Instant jobDeadline = startTime.plusSeconds(maxTimeoutSeconds);

        scanJob.setStatus("RUNNING");
        scanJob.setStage("FETCHING_SNAPSHOT");
        scanJob.setStartedAt(startTime);
        scanJob.setUpdatedAt(startTime);
        scanJob.setHeartbeatAt(startTime);
        scanJob = scanJobRepository.save(scanJob);

        log.info("Starting async scan job {} for repositoryId={} on branch={}", jobId, repositoryId, branch);

        ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                scanJobRepository.updateHeartbeatForRunningJob(jobId, Instant.now());
            } catch (Exception e) {
                log.warn("Task-scoped heartbeat update failed for jobId={}: errorType={}", jobId, e.getClass().getSimpleName());
            }
        }, 15, 15, TimeUnit.SECONDS);

        GitWorkspace workspace = null;
        try {
            // Stage 1: Fetching Snapshot (or Shallow Git Clone)
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            emitEvent(scanJob.getId(), "FETCHING_SNAPSHOT", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("FETCHING_SNAPSHOT"), 95L);
            workspace = gitWorkspaceManager.createWorkspace(repositoryId);
            Path workspacePath = workspace.workspacePath();
            SnapshotTransferMetrics snapshotMetrics = fetchRemoteRepositorySnapshot(repositoryId, branch, workspacePath, jobDeadline);
            String mode = snapshotMetrics != null && snapshotMetrics.mode() != null ? snapshotMetrics.mode() : "GIT_CLONE";
            Long archiveBytes = snapshotMetrics != null ? snapshotMetrics.archiveBytes() : null;
            long wsBytes = snapshotMetrics != null ? snapshotMetrics.workspaceBytes() : 0L;
            int count = snapshotMetrics != null ? snapshotMetrics.entryCount() : 0;
            emitEvent(scanJob.getId(), "FETCHING_SNAPSHOT", "SNAPSHOT_ACQUIRED", "SNAPSHOT_FETCHED",
                    new ScanEventPayload.SnapshotFetchedPayload(mode, archiveBytes, wsBytes, count), 95L);

            // Stage 2: Classifying Files & Coverage
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            scanJob.setStage("CLASSIFYING_FILES");
            Instant stage2Time = Instant.now();
            scanJob.setUpdatedAt(stage2Time);
            scanJob.setHeartbeatAt(stage2Time);
            scanJob = scanJobRepository.save(scanJob);
            emitEvent(scanJob.getId(), "CLASSIFYING_FILES", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("CLASSIFYING_FILES"), 95L);
            CoverageSummary coverageSummary = recordCoverage(scanJob, repositoryId, branch, workspacePath);
            emitEvent(scanJob.getId(), "CLASSIFYING_FILES", "CLASSIFICATION_SUMMARY", "FILES_CLASSIFIED", new ScanEventPayload.FilesClassifiedPayload(
                    coverageSummary.scannedFiles(),
                    coverageSummary.skippedFiles(),
                    coverageSummary.totalFiles()
            ), 95L);

            // Stage 3: Scanning Secrets (Stage 1 Snapshot + Stage 2 Git History)
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            scanJob.setStage("SCANNING_SECRETS");
            Instant stage3Time = Instant.now();
            scanJob.setUpdatedAt(stage3Time);
            scanJob.setHeartbeatAt(stage3Time);
            scanJob = scanJobRepository.save(scanJob);
            emitEvent(scanJob.getId(), "SCANNING_SECRETS", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("SCANNING_SECRETS"), 95L);
            int snapshotTimeout = computeRemainingTimeoutSeconds(jobDeadline, maxTimeoutSeconds);
            emitEvent(scanJob.getId(), "SCANNING_SECRETS", "SCANNER_LIFECYCLE", "SCANNER_ACTIVE", new ScanEventPayload.ScannerActivePayload(
                    "GITLEAKS_AST",
                    "ACTIVE",
                    snapshotTimeout
            ), 95L);

            GitleaksScanResult snapshotResult = gitleaksDetectorAdapter.scan(GitleaksScanRequest.forSnapshot(workspacePath, snapshotTimeout));
            List<DetectedSecretFinding> snapshotFindings = normalizeFindings(repositoryId, snapshotResult.findings());

            List<DetectedSecretFinding> historyFindings = Collections.emptyList();
            if (Files.exists(workspacePath.resolve(".git"))) {
                checkJobDeadline(jobDeadline, maxTimeoutSeconds);
                int historyTimeout = computeRemainingTimeoutSeconds(jobDeadline, maxTimeoutSeconds);
                GitleaksScanResult historyResult = gitleaksDetectorAdapter.scan(GitleaksScanRequest.forGitHistory(workspacePath, null, historyTimeout));
                historyFindings = normalizeFindings(repositoryId, historyResult.findings());
            }

            List<DetectedSecretFinding> allFindings = new ArrayList<>(snapshotFindings);
            allFindings.addAll(historyFindings);
            int totalFindings = allFindings.size();
            for (int i = 0; i < Math.min(50, totalFindings); i++) {
                DetectedSecretFinding f = allFindings.get(i);
                String ruleId = f.ruleId() != null ? f.ruleId() : "UNKNOWN";
                emitEvent(scanJob.getId(), "SCANNING_SECRETS", "FINDING_DISCOVERED", "FINDING_ALERT", new ScanEventPayload.FindingAlertPayload(
                        ruleId,
                        determineSeverity(ruleId),
                        i + 1
                ), 95L);
            }
            if (totalFindings > 50) {
                emitEvent(scanJob.getId(), "SCANNING_SECRETS", "FINDING_DISCOVERED", "FINDINGS_TRUNCATED", new ScanEventPayload.FindingsTruncatedPayload(
                        totalFindings,
                        50
                ), 95L);
            }

            // Stage 4: Recording Evidence
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            scanJob.setStage("RECORDING_EVIDENCE");
            Instant stage4Time = Instant.now();
            scanJob.setUpdatedAt(stage4Time);
            scanJob.setHeartbeatAt(stage4Time);
            scanJob = scanJobRepository.save(scanJob);
            emitEvent(scanJob.getId(), "RECORDING_EVIDENCE", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("RECORDING_EVIDENCE"), 95L);

            String commitSha = resolveCommitSha(workspacePath);
            if (commitSha == null) {
                commitSha = "HEAD-" + UUID.randomUUID().toString().substring(0, 8);
            }

            processFindings(repositoryId, snapshotFindings, historyFindings, commitSha);

            // Validate coverage completeness and advance checkpoint
            if (coverageSummary.coverageImpact() != CoverageImpact.INCOMPLETE) {
                ScanCheckpointEntity checkpoint = ScanCheckpointEntity.builder()
                        .repositoryId(repositoryId)
                        .branchName(branch)
                        .verifiedCommitSha(commitSha)
                        .scanJobId(scanJob.getId())
                        .createdAt(Instant.now())
                        .build();
                scanCheckpointRepository.save(checkpoint);
                log.info("Advanced scan checkpoint for repo={} at commitSha={}", repositoryId, commitSha);
            } else {
                log.warn("Scan checkpoint not advanced due to INCOMPLETE coverage for repo={}", repositoryId);
            }

            // Stage 5: Completed
            Instant completedTime = Instant.now();
            scanJob.setStatus("COMPLETED");
            scanJob.setStage("COMPLETED");
            scanJob.setCommitSha(commitSha);
            scanJob.setCompletedAt(completedTime);
            scanJob.setUpdatedAt(completedTime);
            scanJob.setHeartbeatAt(completedTime);
            scanJob.setDurationMs(completedTime.toEpochMilli() - startTime.toEpochMilli());
            scanJob = scanJobRepository.save(scanJob);

            emitEvent(scanJob.getId(), "COMPLETED", "SCAN_COMPLETED", "JOB_COMPLETED", new ScanEventPayload.JobCompletedPayload(
                    scanJob.getDurationMs(),
                    totalFindings,
                    coverageSummary.coverageImpact().name()
            ), 100L);

            log.info("Scan job {} completed successfully in {}ms for repositoryId={}",
                    scanJob.getId(), scanJob.getDurationMs(), repositoryId);
            return scanJob;
        } catch (ResourceGuardrailExceededException rge) {
            log.warn("Scan guardrail triggered for repositoryId={}: reasonCode={} observedBytes={} limitHit={}",
                    repositoryId, rge.getReasonCode(), rge.getObservedBytes(), rge.getLimitHitValue());

            CoverageRecordEntity record = coverageRecordRepository.findByScanJobId(scanJob.getId())
                    .orElse(null);
            if (record == null) {
                record = CoverageRecordEntity.builder()
                        .scanJobId(scanJob.getId())
                        .repositoryId(repositoryId)
                        .branchName(branch)
                        .createdAt(Instant.now())
                        .build();
            }
            if (rge.getObservedFiles() > 0 || record.getTotalFiles() == null) {
                record.setTotalFiles(rge.getObservedFiles());
            }
            if (rge.getObservedBytes() > 0 || record.getTotalBytes() == null) {
                record.setTotalBytes(rge.getObservedBytes());
            }
            record.setReasonCode(rge.getReasonCode());
            record.setLimitHitValue(rge.getLimitHitValue());
            record.setCoverageImpact("INCOMPLETE");
            coverageRecordRepository.save(record);

            long observedVal = rge.getObservedBytes() > 0 ? rge.getObservedBytes() : (long) rge.getObservedFiles();
            emitEvent(scanJob.getId(), "GUARDRAIL_TRIGGERED", "GUARDRAIL_TRIGGERED", "GUARDRAIL_LIMIT_HIT", new ScanEventPayload.GuardrailLimitHitPayload(
                    rge.getReasonCode(),
                    observedVal,
                    rge.getLimitHitValue()
            ), 100L);

            Instant completedTime = Instant.now();
            scanJob.setStatus("COMPLETED");
            scanJob.setStage("COMPLETED");
            scanJob.setCompletedAt(completedTime);
            scanJob.setUpdatedAt(completedTime);
            scanJob.setHeartbeatAt(completedTime);
            scanJob.setDurationMs(completedTime.toEpochMilli() - startTime.toEpochMilli());
            return scanJobRepository.save(scanJob);
        } catch (Exception e) {
            String sanitizedMsg = sanitizeErrorMessage(e.getMessage());
            log.error("Scan pipeline execution failed for jobId={} repositoryId={}: errorType={} message={}",
                    jobId, repositoryId, e.getClass().getSimpleName(), sanitizedMsg);
            String errorReasonCode = "UNEXPECTED_SCAN_FAILURE";
            if (e instanceof ResourceGuardrailExceededException) {
                errorReasonCode = "GUARDRAIL_EXCEEDED";
            } else if (e instanceof IOException) {
                errorReasonCode = "IO_ERROR";
            }
            emitEvent(scanJob.getId(), "FAILED", "SCAN_FAILED", "JOB_FAILED", new ScanEventPayload.JobFailedPayload(errorReasonCode), 100L);
            Instant completedTime = Instant.now();
            scanJob.setStatus("FAILED");
            scanJob.setStage("FAILED");
            scanJob.setErrorMessage(sanitizedMsg);
            scanJob.setCompletedAt(completedTime);
            scanJob.setUpdatedAt(completedTime);
            scanJob.setHeartbeatAt(completedTime);
            scanJob.setDurationMs(completedTime.toEpochMilli() - startTime.toEpochMilli());
            return scanJobRepository.save(scanJob);
        } finally {
            heartbeatExecutor.shutdownNow();
            if (workspace != null) {
                gitWorkspaceManager.disposeWorkspace(workspace);
            }
        }
    }

    /**
     * Executes the complete snapshot and history scan pipeline for a repository synchronously.
     *
     * @param repositoryId the target repository UUID
     * @param branchName   the target branch name
     * @param sourcePath   optional local directory to copy into workspace
     * @return completed or failed ScanJobEntity
     */
    public ScanJobEntity executeScan(UUID repositoryId, String branchName, Path sourcePath) {
        if (repositoryId == null) {
            throw new IllegalArgumentException("Repository ID is required");
        }
        String branch = (branchName != null && !branchName.isBlank()) ? branchName.trim() : "main";

        Instant startTime = Instant.now();
        int maxTimeoutSeconds = snapshotGuardrailProperties.getMaxScanTimeoutSeconds();
        Instant jobDeadline = startTime.plusSeconds(maxTimeoutSeconds);
        log.info("Starting scan pipeline for repositoryId={} on branch={}", repositoryId, branch);

        // 1. Create ScanJobEntity (PENDING -> RUNNING -> FETCHING_SNAPSHOT)
        ScanJobEntity scanJob = ScanJobEntity.builder()
            .repositoryId(repositoryId)
            .branchName(branch)
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("RUNNING")
            .stage("FETCHING_SNAPSHOT")
            .createdAt(startTime)
            .updatedAt(startTime)
            .startedAt(startTime)
            .heartbeatAt(startTime)
            .build();
        scanJob = scanJobRepository.save(scanJob);

        GitWorkspace workspace = null;
        try {
            // 2. Create isolated workspace
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            emitEvent(scanJob.getId(), "FETCHING_SNAPSHOT", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("FETCHING_SNAPSHOT"), 95L);
            workspace = gitWorkspaceManager.createWorkspace(repositoryId);
            Path workspacePath = workspace.workspacePath();

            // Copy source files if provided, or download snapshot / clone from remote GitHub repository
            if (sourcePath != null && Files.exists(sourcePath)) {
                gitWorkspaceManager.copyDirectory(sourcePath, workspacePath);
                long workspaceSize = 0L;
                int fileCount = 0;
                try (Stream<Path> stream = Files.walk(workspacePath)) {
                    List<Path> regularFiles = stream.filter(Files::isRegularFile).toList();
                    fileCount = regularFiles.size();
                    for (Path p : regularFiles) {
                        try {
                            workspaceSize += Files.size(p);
                        } catch (IOException ignored) {}
                    }
                }
                emitEvent(scanJob.getId(), "FETCHING_SNAPSHOT", "SNAPSHOT_ACQUIRED", "SNAPSHOT_FETCHED",
                        new ScanEventPayload.SnapshotFetchedPayload("LOCAL_WORKSPACE", null, workspaceSize, fileCount), 95L);
            } else {
                SnapshotTransferMetrics snapshotMetrics = fetchRemoteRepositorySnapshot(repositoryId, branch, workspacePath, jobDeadline);
                String mode = snapshotMetrics != null && snapshotMetrics.mode() != null ? snapshotMetrics.mode() : "GIT_CLONE";
                Long archiveBytes = snapshotMetrics != null ? snapshotMetrics.archiveBytes() : null;
                long wsBytes = snapshotMetrics != null ? snapshotMetrics.workspaceBytes() : 0L;
                int count = snapshotMetrics != null ? snapshotMetrics.entryCount() : 0;
                emitEvent(scanJob.getId(), "FETCHING_SNAPSHOT", "SNAPSHOT_ACQUIRED", "SNAPSHOT_FETCHED",
                        new ScanEventPayload.SnapshotFetchedPayload(mode, archiveBytes, wsBytes, count), 95L);
            }

            // 3. Resolve commit SHA if git repository exists
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            String commitSha = resolveCommitSha(workspacePath);
            if (commitSha == null) {
                commitSha = "HEAD-" + UUID.randomUUID().toString().substring(0, 8);
            }

            // 4. File Eligibility & Coverage recording
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            scanJob.setStage("CLASSIFYING_FILES");
            Instant stage2Time = Instant.now();
            scanJob.setUpdatedAt(stage2Time);
            scanJob.setHeartbeatAt(stage2Time);
            scanJob = scanJobRepository.save(scanJob);
            emitEvent(scanJob.getId(), "CLASSIFYING_FILES", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("CLASSIFYING_FILES"), 95L);
            CoverageSummary coverageSummary = recordCoverage(scanJob, repositoryId, branch, workspacePath);
            emitEvent(scanJob.getId(), "CLASSIFYING_FILES", "CLASSIFICATION_SUMMARY", "FILES_CLASSIFIED", new ScanEventPayload.FilesClassifiedPayload(
                    coverageSummary.scannedFiles(),
                    coverageSummary.skippedFiles(),
                    coverageSummary.totalFiles()
            ), 95L);

            // 5. Stage 1: Snapshot scan of HEAD files (FR-025)
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            scanJob.setStage("SCANNING_SECRETS");
            Instant stage3Time = Instant.now();
            scanJob.setUpdatedAt(stage3Time);
            scanJob.setHeartbeatAt(stage3Time);
            scanJob = scanJobRepository.save(scanJob);
            emitEvent(scanJob.getId(), "SCANNING_SECRETS", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("SCANNING_SECRETS"), 95L);
            int snapshotTimeout = computeRemainingTimeoutSeconds(jobDeadline, maxTimeoutSeconds);
            emitEvent(scanJob.getId(), "SCANNING_SECRETS", "SCANNER_LIFECYCLE", "SCANNER_ACTIVE", new ScanEventPayload.ScannerActivePayload(
                    "GITLEAKS_AST",
                    "ACTIVE",
                    snapshotTimeout
            ), 95L);

            GitleaksScanResult snapshotResult = gitleaksDetectorAdapter.scan(GitleaksScanRequest.forSnapshot(workspacePath, snapshotTimeout));
            List<DetectedSecretFinding> snapshotFindings = normalizeFindings(repositoryId, snapshotResult.findings());

            // 6. Stage 2: Git History scan of reachable commits (FR-025)
            List<DetectedSecretFinding> historyFindings = Collections.emptyList();
            if (Files.exists(workspacePath.resolve(".git"))) {
                checkJobDeadline(jobDeadline, maxTimeoutSeconds);
                int historyTimeout = computeRemainingTimeoutSeconds(jobDeadline, maxTimeoutSeconds);
                GitleaksScanResult historyResult = gitleaksDetectorAdapter.scan(GitleaksScanRequest.forGitHistory(workspacePath, null, historyTimeout));
                historyFindings = normalizeFindings(repositoryId, historyResult.findings());
            }

            List<DetectedSecretFinding> allFindings = new ArrayList<>(snapshotFindings);
            allFindings.addAll(historyFindings);
            int totalFindings = allFindings.size();
            for (int i = 0; i < Math.min(50, totalFindings); i++) {
                DetectedSecretFinding f = allFindings.get(i);
                String ruleId = f.ruleId() != null ? f.ruleId() : "UNKNOWN";
                emitEvent(scanJob.getId(), "SCANNING_SECRETS", "FINDING_DISCOVERED", "FINDING_ALERT", new ScanEventPayload.FindingAlertPayload(
                        ruleId,
                        determineSeverity(ruleId),
                        i + 1
                ), 95L);
            }
            if (totalFindings > 50) {
                emitEvent(scanJob.getId(), "SCANNING_SECRETS", "FINDING_DISCOVERED", "FINDINGS_TRUNCATED", new ScanEventPayload.FindingsTruncatedPayload(
                        totalFindings,
                        50
                ), 95L);
            }

            // 7. Apply Finding Lifecycle Engine & update database records (FR-007, FR-018, FR-019, FR-051, DEC-012)
            checkJobDeadline(jobDeadline, maxTimeoutSeconds);
            scanJob.setStage("RECORDING_EVIDENCE");
            Instant stage4Time = Instant.now();
            scanJob.setUpdatedAt(stage4Time);
            scanJob.setHeartbeatAt(stage4Time);
            scanJob = scanJobRepository.save(scanJob);
            emitEvent(scanJob.getId(), "RECORDING_EVIDENCE", "STAGE_TRANSITION", "STAGE_STARTED", new ScanEventPayload.StageStartedPayload("RECORDING_EVIDENCE"), 95L);
            processFindings(repositoryId, snapshotFindings, historyFindings, commitSha);

            // 8. Validate coverage completeness and advance checkpoint (FR-028, FR-029)
            if (coverageSummary.coverageImpact() != CoverageImpact.INCOMPLETE) {
                ScanCheckpointEntity checkpoint = ScanCheckpointEntity.builder()
                    .repositoryId(repositoryId)
                    .branchName(branch)
                    .verifiedCommitSha(commitSha)
                    .scanJobId(scanJob.getId())
                    .createdAt(Instant.now())
                    .build();
                scanCheckpointRepository.save(checkpoint);
                log.info("Advanced scan checkpoint for repo={} at commitSha={}", repositoryId, commitSha);
            } else {
                log.warn("Scan checkpoint not advanced due to INCOMPLETE coverage for repo={}", repositoryId);
            }

            // 9. Complete Scan Job
            Instant completedTime = Instant.now();
            scanJob.setStatus("COMPLETED");
            scanJob.setStage("COMPLETED");
            scanJob.setCommitSha(commitSha);
            scanJob.setCompletedAt(completedTime);
            scanJob.setUpdatedAt(completedTime);
            scanJob.setHeartbeatAt(completedTime);
            scanJob.setDurationMs(completedTime.toEpochMilli() - startTime.toEpochMilli());
            scanJob = scanJobRepository.save(scanJob);

            emitEvent(scanJob.getId(), "COMPLETED", "SCAN_COMPLETED", "JOB_COMPLETED", new ScanEventPayload.JobCompletedPayload(
                    scanJob.getDurationMs(),
                    totalFindings,
                    coverageSummary.coverageImpact().name()
            ), 100L);

            log.info("Scan job {} completed successfully in {}ms for repositoryId={}",
                scanJob.getId(), scanJob.getDurationMs(), repositoryId);
            return scanJob;
        } catch (ResourceGuardrailExceededException rge) {
            log.warn("Scan guardrail triggered for repositoryId={}: reasonCode={} observedBytes={} limitHit={}",
                    repositoryId, rge.getReasonCode(), rge.getObservedBytes(), rge.getLimitHitValue());

            CoverageRecordEntity record = coverageRecordRepository.findByScanJobId(scanJob.getId())
                    .orElse(null);
            if (record == null) {
                record = CoverageRecordEntity.builder()
                        .scanJobId(scanJob.getId())
                        .repositoryId(repositoryId)
                        .branchName(branch)
                        .createdAt(Instant.now())
                        .build();
            }
            if (rge.getObservedFiles() > 0 || record.getTotalFiles() == null) {
                record.setTotalFiles(rge.getObservedFiles());
            }
            if (rge.getObservedBytes() > 0 || record.getTotalBytes() == null) {
                record.setTotalBytes(rge.getObservedBytes());
            }
            record.setReasonCode(rge.getReasonCode());
            record.setLimitHitValue(rge.getLimitHitValue());
            record.setCoverageImpact("INCOMPLETE");
            coverageRecordRepository.save(record);

            long observedVal = rge.getObservedBytes() > 0 ? rge.getObservedBytes() : (long) rge.getObservedFiles();
            emitEvent(scanJob.getId(), "GUARDRAIL_TRIGGERED", "GUARDRAIL_TRIGGERED", "GUARDRAIL_LIMIT_HIT", new ScanEventPayload.GuardrailLimitHitPayload(
                    rge.getReasonCode(),
                    observedVal,
                    rge.getLimitHitValue()
            ), 100L);

            Instant completedTime = Instant.now();
            scanJob.setStatus("COMPLETED");
            scanJob.setStage("COMPLETED");
            scanJob.setCompletedAt(completedTime);
            scanJob.setUpdatedAt(completedTime);
            scanJob.setHeartbeatAt(completedTime);
            scanJob.setDurationMs(completedTime.toEpochMilli() - startTime.toEpochMilli());
            return scanJobRepository.save(scanJob);
        } catch (Exception e) {
            String sanitizedMsg = sanitizeErrorMessage(e.getMessage());
            log.error("Scan pipeline execution failed for jobId={} repositoryId={}: errorType={} message={}",
                    scanJob.getId(), repositoryId, e.getClass().getSimpleName(), sanitizedMsg);
            String errorReasonCode = "UNEXPECTED_SCAN_FAILURE";
            if (e instanceof ResourceGuardrailExceededException) {
                errorReasonCode = "GUARDRAIL_EXCEEDED";
            } else if (e instanceof IOException) {
                errorReasonCode = "IO_ERROR";
            }
            emitEvent(scanJob.getId(), "FAILED", "SCAN_FAILED", "JOB_FAILED", new ScanEventPayload.JobFailedPayload(errorReasonCode), 100L);
            Instant completedTime = Instant.now();
            scanJob.setStatus("FAILED");
            scanJob.setStage("FAILED");
            scanJob.setErrorMessage(sanitizedMsg);
            scanJob.setCompletedAt(completedTime);
            scanJob.setUpdatedAt(completedTime);
            scanJob.setHeartbeatAt(completedTime);
            scanJob.setDurationMs(completedTime.toEpochMilli() - startTime.toEpochMilli());
            return scanJobRepository.save(scanJob);
        } finally {
            // Mandated strict cleanup in finally block (DEC-015)
            if (workspace != null) {
                gitWorkspaceManager.disposeWorkspace(workspace);
            }
        }
    }

    public String sanitizeErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Scan execution failed";
        }
        String sanitized = rawMessage.replaceAll("(?i)(gh[pousr]_[A-Za-z0-9_]{16,})", "[REDACTED_TOKEN]");
        sanitized = sanitized.replaceAll("(?i)(bearer\\s+)[A-Za-z0-9_.-]+", "$1[REDACTED_TOKEN]");
        sanitized = sanitized.replaceAll("(?i)(password|secret|token)\\s*[=:]\\s*(?!\\[REDACTED)[^\\s,;]+", "$1=[REDACTED]");
        return sanitized;
    }

    /**
     * Records file coverage breakdown and summary for the scan job.
     */
    private CoverageSummary recordCoverage(ScanJobEntity scanJob, UUID repositoryId, String branchName, Path workspacePath) throws IOException {
        List<CoverageItem> coverageItems = new ArrayList<>();
        if (Files.exists(workspacePath)) {
            try (Stream<Path> stream = Files.walk(workspacePath)) {
                List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !isGitInternal(p, workspacePath))
                    .toList();

                for (Path f : files) {
                    CoverageItem item = fileEligibilityEngine.evaluate(f, ScanMode.CONTINUOUS_MONITORING);
                    coverageItems.add(item);
                }
            }
        }

        CoverageSummary summary = fileEligibilityEngine.summarize(coverageItems);

        CoverageRecordEntity record = CoverageRecordEntity.builder()
            .scanJobId(scanJob.getId())
            .repositoryId(repositoryId)
            .branchName(branchName)
            .totalFiles(summary.totalFiles())
            .scannedFiles(summary.scannedFiles())
            .skippedFiles(summary.skippedFiles())
            .textFiles(summary.textFiles())
            .binaryFiles(summary.binaryFiles())
            .undeterminedFiles(summary.undeterminedFiles())
            .totalBytes(summary.totalBytes())
            .coverageImpact(summary.coverageImpact() != null ? summary.coverageImpact().name() : "COMPLETE")
            .createdAt(Instant.now())
            .build();
        record = coverageRecordRepository.save(record);

        List<CoverageItemEntity> itemEntities = new ArrayList<>();
        for (CoverageItem ci : coverageItems) {
            String relPath = ci.path();
            if (relPath != null && relPath.startsWith(workspacePath.toString())) {
                relPath = workspacePath.relativize(Path.of(relPath)).toString().replace('\\', '/');
            }

            CoverageItemEntity cie = CoverageItemEntity.builder()
                .coverageRecordId(record.getId())
                .filePath(relPath)
                .classification(ci.classification() != null ? ci.classification().name() : "UNDETERMINED")
                .sizeBytes(ci.sizeBytes())
                .status(ci.status() != null ? ci.status().name() : "SKIPPED")
                .reasonCode(ci.reasonCode() != null ? ci.reasonCode().name() : null)
                .impact(ci.impact() != null ? ci.impact().name() : "COMPLETE")
                .details(ci.details())
                .build();
            itemEntities.add(cie);
        }
        coverageItemRepository.saveAll(itemEntities);

        return summary;
    }

    /**
     * Processes snapshot and history findings through the FindingLifecycleEngine and persists state.
     */
    @Transactional
    public void processFindings(
        UUID repositoryId,
        List<DetectedSecretFinding> snapshotFindings,
        List<DetectedSecretFinding> historyFindings,
        String currentCommitSha
    ) {
        Instant now = Instant.now();

        // Group findings by fingerprint
        Map<String, List<DetectedSecretFinding>> snapshotByFp = snapshotFindings.stream()
            .filter(f -> f.redactedEvidence() != null && f.redactedEvidence().fingerprint() != null)
            .collect(Collectors.groupingBy(f -> f.redactedEvidence().fingerprint()));

        Map<String, List<DetectedSecretFinding>> historyByFp = historyFindings.stream()
            .filter(f -> f.redactedEvidence() != null && f.redactedEvidence().fingerprint() != null)
            .collect(Collectors.groupingBy(f -> f.redactedEvidence().fingerprint()));

        // Query existing findings from PostgreSQL
        List<FindingEntity> existingFindings = findingRepository.findByRepositoryId(repositoryId);
        Map<String, FindingEntity> existingByFp = existingFindings.stream()
            .collect(Collectors.toMap(FindingEntity::getFingerprint, f -> f, (a, b) -> a));

        // Union of all fingerprints
        Set<String> allFingerprints = new HashSet<>();
        allFingerprints.addAll(snapshotByFp.keySet());
        allFingerprints.addAll(historyByFp.keySet());
        allFingerprints.addAll(existingByFp.keySet());

        for (String fp : allFingerprints) {
            boolean presentAtHead = snapshotByFp.containsKey(fp);
            boolean presentInHistory = historyByFp.containsKey(fp);
            FindingEntity existing = existingByFp.get(fp);

            FindingLifecycleResult lifecycleResult = findingLifecycleEngine.evaluate(existing, presentAtHead, presentInHistory);

            Instant resolvedAt = null;
            if (lifecycleResult.lifecycle() == FindingLifecycle.RESOLVED) {
                resolvedAt = (existing != null && existing.getResolvedAt() != null) ? existing.getResolvedAt() : now;
            }

            DetectedSecretFinding sample = presentAtHead
                ? snapshotByFp.get(fp).get(0)
                : (presentInHistory ? historyByFp.get(fp).get(0) : null);

            String ruleId = sample != null ? sample.ruleId() : (existing != null ? existing.getRuleId() : "SP-CONFIG-001");
            String severity = determineSeverity(ruleId);
            String title = sample != null ? "Exposed secret matching " + ruleId : (existing != null ? existing.getTitle() : "Exposed secret");
            String description = "Potential secret credential detected by rule " + ruleId;

            FindingEntity findingEntity;
            if (existing == null) {
                findingEntity = FindingEntity.builder()
                    .repositoryId(repositoryId)
                    .ruleId(ruleId)
                    .fingerprint(fp)
                    .severity(severity)
                    .title(title)
                    .description(description)
                    .lifecycle(lifecycleResult.lifecycle().name())
                    .remediationQuality(lifecycleResult.remediationQuality().name())
                    .firstSeenAt(now)
                    .lastSeenAt(now)
                    .resolvedAt(resolvedAt)
                    .build();
            } else {
                findingEntity = existing;
                findingEntity.setLifecycle(lifecycleResult.lifecycle().name());
                findingEntity.setRemediationQuality(lifecycleResult.remediationQuality().name());
                if (presentAtHead || presentInHistory) {
                    findingEntity.setLastSeenAt(now);
                }
                findingEntity.setResolvedAt(resolvedAt);
            }
            findingEntity = findingRepository.save(findingEntity);

            // Update Finding Locations if findings were observed in current scan
            if (presentAtHead || presentInHistory) {
                findingLocationRepository.deleteByFindingId(findingEntity.getId());
                List<FindingLocationEntity> locEntities = new ArrayList<>();

                if (presentAtHead) {
                    for (DetectedSecretFinding dsf : snapshotByFp.get(fp)) {
                        locEntities.add(FindingLocationEntity.builder()
                            .findingId(findingEntity.getId())
                            .filePath(dsf.file())
                            .startLine(dsf.startLine())
                            .endLine(dsf.endLine())
                            .startColumn(dsf.startColumn())
                            .endColumn(dsf.endColumn())
                            .commitSha(currentCommitSha != null ? currentCommitSha : dsf.commit())
                            .author(dsf.author())
                            .isCurrentHead(true)
                            .detectedAt(now)
                            .build());
                    }
                }

                if (presentInHistory) {
                    for (DetectedSecretFinding dsf : historyByFp.get(fp)) {
                        boolean isHead = presentAtHead && snapshotByFp.get(fp).stream()
                            .anyMatch(hf -> Objects.equals(hf.file(), dsf.file()) && hf.startLine() == dsf.startLine());
                        if (!isHead) {
                            locEntities.add(FindingLocationEntity.builder()
                                .findingId(findingEntity.getId())
                                .filePath(dsf.file())
                                .startLine(dsf.startLine())
                                .endLine(dsf.endLine())
                                .startColumn(dsf.startColumn())
                                .endColumn(dsf.endColumn())
                                .commitSha(dsf.commit())
                                .author(dsf.author())
                                .isCurrentHead(false)
                                .detectedAt(now)
                                .build());
                        }
                    }
                }
                findingLocationRepository.saveAll(locEntities);

                // Add evidence item
                if (sample != null && sample.redactedEvidence() != null) {
                    EvidenceItemEntity evidence = EvidenceItemEntity.builder()
                        .findingId(findingEntity.getId())
                        .evidenceType("TECHNICAL")
                        .maskedSecret(sample.redactedEvidence().maskedSecret())
                        .redactedSnippet(sample.redactedEvidence().redactedSnippet())
                        .verificationStatus("OBSERVED")
                        .sourceAttribution("GitleaksDetectorAdapter:SP-CONFIG-001")
                        .createdAt(now)
                        .build();
                    evidenceItemRepository.save(evidence);
                }
            } else {
                // Not in current HEAD or history (clean rewrite / purged finding): mark any existing locations as not head
                List<FindingLocationEntity> oldLocs = findingLocationRepository.findByFindingId(findingEntity.getId());
                for (FindingLocationEntity loc : oldLocs) {
                    loc.setIsCurrentHead(false);
                }
                findingLocationRepository.saveAll(oldLocs);
            }
        }
    }

    /**
     * Normalizes raw Gitleaks findings and applies HMAC-SHA256 fingerprinting.
     */
    private List<DetectedSecretFinding> normalizeFindings(UUID repositoryId, List<GitleaksRawFinding> rawFindings) {
        if (rawFindings == null || rawFindings.isEmpty()) {
            return Collections.emptyList();
        }

        List<DetectedSecretFinding> normalized = new ArrayList<>();
        for (GitleaksRawFinding raw : rawFindings) {
            String rawSecret = raw.secret() != null ? raw.secret() : "";
            String match = raw.match() != null ? raw.match() : rawSecret;

            SecretMatch secretMatch = new SecretMatch(
                rawSecret,
                raw.ruleID(),
                raw.startLine(),
                raw.endLine(),
                raw.startColumn(),
                raw.endColumn(),
                match
            );

            RedactedEvidence evidence = secretRedactionService.buildRedactedEvidence(repositoryId.toString(), secretMatch);

            DetectedSecretFinding finding = new DetectedSecretFinding(
                raw.ruleID(),
                raw.file(),
                raw.startLine(),
                raw.endLine(),
                raw.startColumn(),
                raw.endColumn(),
                raw.commit(),
                raw.author(),
                raw.date(),
                evidence
            );
            normalized.add(finding);
        }
        return normalized;
    }

    private String determineSeverity(String ruleId) {
        if (ruleId == null) {
            return "HIGH";
        }
        String lower = ruleId.toLowerCase();
        if (lower.contains("private-key") || lower.contains("aws") || lower.contains("google") || lower.contains("github")) {
            return "CRITICAL";
        }
        return "HIGH";
    }

    private String resolveCommitSha(Path workspacePath) {
        if (workspacePath == null || !Files.exists(workspacePath.resolve(".git"))) {
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(workspacePath.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                if (output.matches("^[0-9a-fA-F]{40}$")) {
                    return output;
                }
            }
        } catch (Exception e) {
            log.debug("Git commit SHA resolution failed via CLI: {}", e.getMessage());
        }

        // Direct .git/HEAD read fallback
        try {
            Path headFile = workspacePath.resolve(".git/HEAD");
            if (Files.exists(headFile)) {
                String content = Files.readString(headFile, StandardCharsets.UTF_8).trim();
                if (content.startsWith("ref: ")) {
                    String refPath = content.substring(5).trim();
                    Path refFile = workspacePath.resolve(".git").resolve(refPath);
                    if (Files.exists(refFile)) {
                        String sha = Files.readString(refFile, StandardCharsets.UTF_8).trim();
                        if (sha.matches("^[0-9a-fA-F]{40}$")) {
                            return sha;
                        }
                    }
                } else if (content.matches("^[0-9a-fA-F]{40}$")) {
                    return content;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private boolean isGitInternal(Path file, Path root) {
        Path relative = root.relativize(file);
        for (Path part : relative) {
            String name = part.toString();
            if (name.equals(".git") || name.equals(".empty-hooks")) {
                return true;
            }
        }
        return false;
    }

    SnapshotTransferMetrics fetchRemoteRepositorySnapshot(UUID repositoryId, String branch, Path workspacePath) {
        return fetchRemoteRepositorySnapshot(repositoryId, branch, workspacePath, null);
    }

    SnapshotTransferMetrics fetchRemoteRepositorySnapshot(UUID repositoryId, String branch, Path workspacePath, Instant jobDeadline) {
        if (repositoryRepository == null || repositoryId == null) {
            throw new IllegalStateException("Repository repository or repository ID is not available");
        }
        com.scanpilot.persistence.entity.RepositoryEntity repo = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalStateException("Repository not found: " + repositoryId));

        String fullName = repo.getFullName();
        if (fullName == null || fullName.isBlank()) {
            if (repo.getOwner() != null && repo.getName() != null) {
                fullName = repo.getOwner() + "/" + repo.getName();
            }
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalStateException("Repository full name could not be determined for repository: " + repositoryId);
        }

        log.info("Fetching remote repository {} on branch {}", fullName, branch);
        String token = null;
        if (repo.getUserId() != null && userSessionRepository != null) {
            List<com.scanpilot.persistence.entity.UserSessionEntity> sessions = userSessionRepository.findByUserId(repo.getUserId());
            if (!sessions.isEmpty()) {
                token = sessions.get(0).getAccessToken();
            }
        }

        if (gitCloneService != null) {
            gitCloneService.cloneRepository(fullName, branch, token, workspacePath, jobDeadline);
            long wsSize = computeDirectorySize(workspacePath);
            int entryCount = countEntries(workspacePath);
            log.info("Successfully cloned repository {} on branch {} via GitCloneService (workspaceBytes={}, entries={})",
                    fullName, branch, wsSize, entryCount);
            // Represent Git clone transfer evidence truthfully: mode=GIT_CLONE, archiveBytes=null
            return SnapshotTransferMetrics.forGitClone(wsSize, entryCount);
        }

        if (streamedSnapshotFetcher != null) {
            java.net.http.HttpClient httpClient = createHttpClient();
            String url = "https://api.github.com/repos/" + fullName + "/zipball/" + branch;

            // Bounded streaming snapshot acquisition and extraction (FR-028, FR-031, NFR-001)
            SnapshotTransferMetrics metrics = streamedSnapshotFetcher.downloadAndExtract(httpClient, url, token, workspacePath, jobDeadline);
            log.info("Successfully fetched and extracted repository snapshot for {}", fullName);
            return metrics;
        }

        throw new IllegalStateException("No repository fetcher service configured for repository: " + fullName);
    }

    private long computeDirectorySize(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private int countEntries(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return (int) stream.count();
        } catch (IOException e) {
            return 0;
        }
    }

    public void emitEvent(UUID jobId, String stage, String eventType, String messageCode, ScanEventPayload payload, long maxLimit) {
        if (scanEventRepository == null || jobId == null) {
            return;
        }
        try {
            String payloadJson = telemetryPayloadSerializer != null ? telemetryPayloadSerializer.serialize(payload) : null;
            if (payload != null && payloadJson == null) {
                log.debug("Event {} ({}) suppressed due to invalid/oversized payload", eventType, messageCode);
                return;
            }
            Optional<Long> allocatedSeq = scanEventRepository.insertEventAtomicCTE(
                    jobId,
                    maxLimit,
                    UUID.randomUUID(),
                    stage,
                    eventType,
                    messageCode,
                    payloadJson,
                    Instant.now()
            );
            if (allocatedSeq.isEmpty()) {
                log.debug("Event {} ({}) dropped/suppressed", eventType, messageCode);
            }
        } catch (Exception e) {
            log.warn("Event persistence error for eventType={}", eventType);
        }
    }

    public void emitEvent(ScanJobEntity job, String stage, String eventType, String messageCode, ScanEventPayload payload) {
        if (job != null && job.getId() != null) {
            emitEvent(job.getId(), stage, eventType, messageCode, payload, 95L);
        }
    }

    protected java.net.http.HttpClient createHttpClient() {
        return java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }

    private void checkJobDeadline(Instant deadline, int maxTimeoutSeconds) {
        if (Instant.now().isAfter(deadline)) {
            throw new ResourceGuardrailExceededException("SCAN_TIMEOUT", 0, 0, maxTimeoutSeconds);
        }
    }

    private int computeRemainingTimeoutSeconds(Instant deadline, int maxTimeoutSeconds) {
        long remaining = Duration.between(Instant.now(), deadline).toSeconds();
        if (remaining <= 0) {
            throw new ResourceGuardrailExceededException("SCAN_TIMEOUT", 0, 0, maxTimeoutSeconds);
        }
        return (int) remaining;
    }
}
