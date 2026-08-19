package com.scanpilot.scanner.pipeline;

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
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.scanner.classifier.CoverageImpact;
import com.scanpilot.scanner.classifier.CoverageItem;
import com.scanpilot.scanner.classifier.CoverageSummary;
import com.scanpilot.scanner.classifier.FileEligibilityEngine;
import com.scanpilot.scanner.classifier.ScanMode;
import com.scanpilot.scanner.detector.gitleaks.DetectedSecretFinding;
import com.scanpilot.scanner.detector.gitleaks.GitleaksDetectorAdapter;
import com.scanpilot.scanner.detector.gitleaks.GitleaksRawFinding;
import com.scanpilot.scanner.detector.gitleaks.GitleaksScanRequest;
import com.scanpilot.scanner.detector.gitleaks.GitleaksScanResult;
import com.scanpilot.scanner.lifecycle.FindingLifecycle;
import com.scanpilot.scanner.lifecycle.FindingLifecycleEngine;
import com.scanpilot.scanner.lifecycle.FindingLifecycleResult;
import com.scanpilot.scanner.workspace.GitWorkspace;
import com.scanpilot.scanner.workspace.GitWorkspaceManager;
import com.scanpilot.security.secret.RedactedEvidence;
import com.scanpilot.security.secret.SecretMatch;
import com.scanpilot.security.secret.SecretRedactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Orchestrator service for executing Snapshot and Git History Scan Pipelines
 * with Finding Lifecycle tracking and coverage recording (FR-007, FR-018, FR-019,
 * FR-025, FR-028, FR-029, FR-051, DEC-012).
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

    private final ScanJobRepository scanJobRepository;
    private final ScanCheckpointRepository scanCheckpointRepository;
    private final FindingRepository findingRepository;
    private final FindingLocationRepository findingLocationRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final CoverageRecordRepository coverageRecordRepository;
    private final CoverageItemRepository coverageItemRepository;
    private final com.scanpilot.persistence.repository.RepositoryRepository repositoryRepository;
    private final com.scanpilot.persistence.repository.UserSessionRepository userSessionRepository;

    /**
     * Executes the complete snapshot and history scan pipeline for a repository.
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
        log.info("Starting scan pipeline for repositoryId={} on branch={}", repositoryId, branch);

        // 1. Create ScanJobEntity (PENDING -> RUNNING)
        ScanJobEntity scanJob = ScanJobEntity.builder()
            .repositoryId(repositoryId)
            .branchName(branch)
            .scanMode("SNAPSHOT_AND_HISTORY")
            .status("RUNNING")
            .startedAt(startTime)
            .build();
        scanJob = scanJobRepository.save(scanJob);

        GitWorkspace workspace = null;
        try {
            // 2. Create isolated workspace
            workspace = gitWorkspaceManager.createWorkspace(repositoryId);
            Path workspacePath = workspace.workspacePath();

            // Copy source files if provided, or download snapshot from remote GitHub repository
            if (sourcePath != null && Files.exists(sourcePath)) {
                gitWorkspaceManager.copyDirectory(sourcePath, workspacePath);
            } else {
                fetchRemoteRepositorySnapshot(repositoryId, branch, workspacePath);
            }

            // 3. Resolve commit SHA if git repository exists
            String commitSha = resolveCommitSha(workspacePath);
            if (commitSha == null) {
                commitSha = "HEAD-" + UUID.randomUUID().toString().substring(0, 8);
            }

            // 4. File Eligibility & Coverage recording
            CoverageSummary coverageSummary = recordCoverage(scanJob, repositoryId, branch, workspacePath);

            // 5. Stage 1: Snapshot scan of HEAD files (FR-025)
            GitleaksScanResult snapshotResult = gitleaksDetectorAdapter.scan(GitleaksScanRequest.forSnapshot(workspacePath));
            List<DetectedSecretFinding> snapshotFindings = normalizeFindings(repositoryId, snapshotResult.findings());

            // 6. Stage 2: Git History scan of reachable commits (FR-025)
            List<DetectedSecretFinding> historyFindings = Collections.emptyList();
            if (Files.exists(workspacePath.resolve(".git"))) {
                GitleaksScanResult historyResult = gitleaksDetectorAdapter.scan(GitleaksScanRequest.forGitHistory(workspacePath, null));
                historyFindings = normalizeFindings(repositoryId, historyResult.findings());
            }

            // 7. Apply Finding Lifecycle Engine & update database records (FR-007, FR-018, FR-019, FR-051, DEC-012)
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
            scanJob.setCommitSha(commitSha);
            scanJob.setCompletedAt(completedTime);
            scanJob.setDurationMs(completedTime.toEpochMilli() - startTime.toEpochMilli());
            scanJob = scanJobRepository.save(scanJob);

            log.info("Scan job {} completed successfully in {}ms for repositoryId={}",
                scanJob.getId(), scanJob.getDurationMs(), repositoryId);
            return scanJob;
        } catch (Exception e) {
            log.error("Scan pipeline failed for repositoryId={}: {}", repositoryId, e.getMessage(), e);
            Instant completedTime = Instant.now();
            scanJob.setStatus("FAILED");
            scanJob.setErrorMessage(e.getMessage());
            scanJob.setCompletedAt(completedTime);
            scanJob.setDurationMs(completedTime.toEpochMilli() - startTime.toEpochMilli());
            return scanJobRepository.save(scanJob);
        } finally {
            // Mandated strict cleanup in finally block (DEC-015)
            if (workspace != null) {
                gitWorkspaceManager.disposeWorkspace(workspace);
            }
        }
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
            if (part.toString().equals(".git")) {
                return true;
            }
        }
        return false;
    }

    private void fetchRemoteRepositorySnapshot(UUID repositoryId, String branch, Path workspacePath) {
        if (repositoryRepository == null || repositoryId == null) {
            return;
        }
        repositoryRepository.findById(repositoryId).ifPresent(repo -> {
            String fullName = repo.getFullName();
            if (fullName == null || fullName.isBlank()) {
                if (repo.getOwner() != null && repo.getName() != null) {
                    fullName = repo.getOwner() + "/" + repo.getName();
                }
            }
            if (fullName == null || fullName.isBlank()) {
                return;
            }

            log.info("Fetching remote snapshot for repository {} on branch {}", fullName, branch);
            try {
                String url = "https://api.github.com/repos/" + fullName + "/zipball/" + branch;
                org.springframework.web.client.RestClient client = org.springframework.web.client.RestClient.create();

                String token = null;
                if (repo.getUserId() != null && userSessionRepository != null) {
                    List<com.scanpilot.persistence.entity.UserSessionEntity> sessions = userSessionRepository.findByUserId(repo.getUserId());
                    if (!sessions.isEmpty()) {
                        token = sessions.get(0).getAccessToken();
                    }
                }

                var req = client.get().uri(url)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "Scan-Pilot");
                if (token != null && !token.isBlank() && !token.startsWith("mock-")) {
                    req.header("Authorization", "Bearer " + token);
                }

                byte[] zipBytes = req.retrieve().body(byte[].class);
                if (zipBytes != null && zipBytes.length > 0) {
                    extractZipArchive(zipBytes, workspacePath);
                    log.info("Successfully extracted {} bytes of repository snapshot for {}", zipBytes.length, fullName);
                }
            } catch (Exception e) {
                log.warn("Could not download remote snapshot for {} (branch: {}): {}", fullName, branch, e.getMessage());
            }
        });
    }

    private void extractZipArchive(byte[] zipBytes, Path targetDir) throws IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                int slashIdx = entryName.indexOf('/');
                if (slashIdx >= 0) {
                    entryName = entryName.substring(slashIdx + 1);
                }
                if (entryName.isBlank()) {
                    continue;
                }
                Path resolved = targetDir.resolve(entryName).normalize();
                // Zip-slip security protection
                if (!resolved.startsWith(targetDir)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    if (resolved.getParent() != null) {
                        Files.createDirectories(resolved.getParent());
                    }
                    Files.copy(zis, resolved, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
}
