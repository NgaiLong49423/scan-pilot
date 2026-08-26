package com.scanpilot.scanner.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.persistence.entity.CoverageItemEntity;
import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.MonitoredBranchEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanEventEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.MonitoredBranchRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanEventRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.dispatcher.ScanJobDispatcher;
import com.scanpilot.scanner.dto.CoverageItemDto;
import com.scanpilot.scanner.dto.CoverageSummaryDto;
import com.scanpilot.scanner.dto.FindingDto;
import com.scanpilot.scanner.dto.FindingLocationDto;
import com.scanpilot.scanner.dto.ScanEventDto;
import com.scanpilot.scanner.dto.ScanEventsResponse;
import com.scanpilot.scanner.dto.ScanJobDto;
import com.scanpilot.scanner.dto.ScanTriggerRequest;
import com.scanpilot.scanner.dto.ScanTriggerResponse;
import com.scanpilot.scanner.pipeline.ScanPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for triggering and inspecting scans, finding lifecycles, and coverage reports.
 * Enforces strict fail-closed repository identity and authorization (Issue #53, zero fallback).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
public class ScanController {

    private static final String FAIL_CLOSED_MESSAGE = "Invalid, missing, or unauthorized repository ID";

    private final ScanJobDispatcher scanJobDispatcher;
    private final ScanPipelineService scanPipelineService;
    private final ScanJobRepository scanJobRepository;
    private final ScanEventRepository scanEventRepository;
    private final FindingRepository findingRepository;
    private final FindingLocationRepository findingLocationRepository;
    private final CoverageRecordRepository coverageRecordRepository;
    private final CoverageItemRepository coverageItemRepository;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final MonitoredBranchRepository monitoredBranchRepository;

    /**
     * Triggers a snapshot and git history scan on an active monitored repository (FR-025, Issue #52, Issue #53).
     * Enforces strict fail-closed validation: repositoryId (UUID) is mandatory, must exist in PostgreSQL,
     * and must belong to the authenticated user. Dispatches scan job asynchronously with bounded queue protection.
     */
    @PostMapping("/trigger")
    @RequireAuth
    public ResponseEntity<ScanTriggerResponse> triggerScan(
        @CurrentUser UserSession session,
        @RequestBody(required = false) ScanTriggerRequest request
    ) {
        if (request == null || request.repositoryId() == null) {
            log.warn("Scan trigger rejected: Missing repositoryId in request payload (fail-closed)");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ScanTriggerResponse(null, null, null, "FAILED", FAIL_CLOSED_MESSAGE));
        }

        // Reject custom sourcePath strictly for remote scans (fail-closed, HTTP 400 Bad Request)
        if (request.sourcePath() != null && !request.sourcePath().isBlank()) {
            log.warn("Scan trigger rejected: Custom sourcePath is not permitted for remote repository scans (fail-closed)");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ScanTriggerResponse(null, null, null, "FAILED", "Custom sourcePath is not permitted for remote repository scans"));
        }

        UUID repositoryId = request.repositoryId();

        // Validate user exists in PostgreSQL
        Optional<UserEntity> userOpt = userRepository.findByGithubUserId(session.getGithubUserId());
        if (userOpt.isEmpty()) {
            log.warn("Scan trigger rejected: User not found in PostgreSQL for githubUserId={} (fail-closed)", session.getGithubUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ScanTriggerResponse(null, null, null, "FAILED", FAIL_CLOSED_MESSAGE));
        }
        UserEntity user = userOpt.get();

        // Validate repository exists and is owned by the authenticated user in PostgreSQL (source of truth)
        Optional<RepositoryEntity> repoOpt = repositoryRepository.findById(repositoryId);
        if (repoOpt.isEmpty() || !repoOpt.get().getUserId().equals(user.getId())) {
            log.warn("Scan trigger rejected: Repository {} does not exist or does not belong to user {} (fail-closed)",
                repositoryId, user.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ScanTriggerResponse(null, null, null, "FAILED", FAIL_CLOSED_MESSAGE));
        }

        RepositoryEntity repo = repoOpt.get();

        // Determine branch strictly from monitored_branches table as the sole authority (B.1)
        List<MonitoredBranchEntity> activeBranches = (monitoredBranchRepository != null)
            ? monitoredBranchRepository.findByRepositoryIdAndIsActiveTrue(repo.getId())
            : List.of();

        final String branchName = (request.branchName() != null && !request.branchName().isBlank())
            ? request.branchName().trim()
            : activeBranches.stream()
                .filter(b -> "PRIMARY".equalsIgnoreCase(b.getBranchType()))
                .map(MonitoredBranchEntity::getBranchName)
                .findFirst()
                .orElse(null);

        if (branchName == null) {
            log.warn("Scan trigger rejected: No active branch configured for repository {} (fail-closed)", repo.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ScanTriggerResponse(null, null, null, "FAILED", "Branch is not configured for monitoring on this repository"));
        }

        boolean isConfigured = activeBranches.stream()
            .anyMatch(b -> branchName.equals(b.getBranchName()) && Boolean.TRUE.equals(b.getIsActive()));

        if (!isConfigured) {
            log.warn("Scan trigger rejected: Branch '{}' is not configured for monitoring on repository {} (fail-closed)", branchName, repo.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ScanTriggerResponse(null, null, null, "FAILED", "Branch '" + branchName + "' is not configured for monitoring on this repository"));
        }

        log.info("Dispatching async scan for repositoryId={} on branch={}", repo.getId(), branchName);
        ScanJobEntity job = scanJobDispatcher.dispatch(repo, branchName);

        String message = "Scan job queued successfully";
        if ("RUNNING".equalsIgnoreCase(job.getStatus())) {
            message = "Scan job is currently running";
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new ScanTriggerResponse(
                job.getId(),
                repo.getId(),
                branchName,
                job.getStatus(),
                job.getStage() != null ? job.getStage() : job.getStatus(),
                message
            ));
    }

    /**
     * Retrieves scan job status, telemetry, and duration.
     * Enforces repository ownership check against authenticated session.
     */
    @GetMapping("/jobs/{jobId}")
    @RequireAuth
    public ResponseEntity<ScanJobDto> getScanJob(
        @CurrentUser UserSession session,
        @PathVariable UUID jobId
    ) {
        Optional<ScanJobEntity> jobOpt = scanJobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ScanJobEntity job = jobOpt.get();

        Optional<UserEntity> userOpt = userRepository.findByGithubUserId(session.getGithubUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<RepositoryEntity> repoOpt = repositoryRepository.findById(job.getRepositoryId());
        if (repoOpt.isEmpty() || !repoOpt.get().getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(ScanJobDto.from(job));
    }

    /**
     * Retrieves progressive telemetry scan events for a scan job.
     * Enforces fail-closed repository ownership check against authenticated session (AC-05).
     */
    @GetMapping("/jobs/{jobId}/events")
    @RequireAuth
    public ResponseEntity<ScanEventsResponse> getScanJobEvents(
        @CurrentUser UserSession session,
        @PathVariable UUID jobId,
        @RequestParam(name = "afterSeq", defaultValue = "0") long afterSeq,
        @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        if (session == null || session.getGithubUserId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (jobId == null || afterSeq < 0 || limit < 1 || limit > 50) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<ScanJobEntity> jobOpt = scanJobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ScanJobEntity job = jobOpt.get();

        Optional<UserEntity> userOpt = userRepository.findByGithubUserId(session.getGithubUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<RepositoryEntity> repoOpt = repositoryRepository.findById(job.getRepositoryId());
        if (repoOpt.isEmpty() || !repoOpt.get().getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        PageRequest pageRequest = PageRequest.of(0, limit);
        List<ScanEventEntity> eventEntities = scanEventRepository
                .findByScanJobIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(jobId, afterSeq, pageRequest);

        List<ScanEventDto> eventDtos = eventEntities.stream()
                .map(ScanEventDto::from)
                .toList();

        long maxSeqInBatch = eventEntities.isEmpty() ? afterSeq : eventEntities.get(eventEntities.size() - 1).getSequenceNumber();
        long totalCurrentSeq = job.getNextEventSequence();
        boolean hasMore = maxSeqInBatch < totalCurrentSeq;

        return ResponseEntity.ok(new ScanEventsResponse(
                job.getId(),
                job.getStatus(),
                job.getStage(),
                totalCurrentSeq,
                hasMore,
                eventDtos
        ));
    }

    /**
     * Retrieves all findings for a repository with severity, lifecycle state, and remediation quality.
     * Enforces repository ownership check against authenticated session.
     */
    @GetMapping("/repositories/{repositoryId}/findings")
    @RequireAuth
    public ResponseEntity<List<FindingDto>> getFindings(
        @CurrentUser UserSession session,
        @PathVariable UUID repositoryId
    ) {
        if (repositoryId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<UserEntity> userOpt = userRepository.findByGithubUserId(session.getGithubUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<RepositoryEntity> repoOpt = repositoryRepository.findById(repositoryId);
        if (repoOpt.isEmpty() || !repoOpt.get().getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<FindingEntity> findings = findingRepository.findByRepositoryId(repositoryId);
        List<FindingDto> dtos = new ArrayList<>();

        for (FindingEntity finding : findings) {
            List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(finding.getId());
            List<FindingLocationDto> locationDtos = locations.stream()
                .map(FindingLocationDto::from)
                .toList();
            dtos.add(FindingDto.from(finding, locationDtos));
        }

        return ResponseEntity.ok(dtos);
    }

    /**
     * Retrieves the latest coverage summary and skipped files report for a repository.
     * Enforces repository ownership check against authenticated session.
     */
    @GetMapping("/repositories/{repositoryId}/coverage")
    @RequireAuth
    public ResponseEntity<CoverageSummaryDto> getCoverage(
        @CurrentUser UserSession session,
        @PathVariable UUID repositoryId
    ) {
        if (repositoryId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<UserEntity> userOpt = userRepository.findByGithubUserId(session.getGithubUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<RepositoryEntity> repoOpt = repositoryRepository.findById(repositoryId);
        if (repoOpt.isEmpty() || !repoOpt.get().getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<CoverageRecordEntity> records = coverageRecordRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId);
        if (records.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        CoverageRecordEntity latest = records.get(0);
        List<CoverageItemEntity> items = coverageItemRepository.findByCoverageRecordId(latest.getId());
        List<CoverageItemDto> itemDtos = items.stream()
            .map(CoverageItemDto::from)
            .toList();

        return ResponseEntity.ok(CoverageSummaryDto.from(latest, itemDtos));
    }
}
