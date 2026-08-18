package com.scanpilot.scanner.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.persistence.entity.CoverageItemEntity;
import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.project.model.MonitoredProject;
import com.scanpilot.project.service.ProjectService;
import com.scanpilot.scanner.dto.CoverageItemDto;
import com.scanpilot.scanner.dto.CoverageSummaryDto;
import com.scanpilot.scanner.dto.FindingDto;
import com.scanpilot.scanner.dto.FindingLocationDto;
import com.scanpilot.scanner.dto.ScanJobDto;
import com.scanpilot.scanner.dto.ScanTriggerRequest;
import com.scanpilot.scanner.dto.ScanTriggerResponse;
import com.scanpilot.scanner.pipeline.ScanPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for triggering and inspecting scans, finding lifecycles, and coverage reports.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanPipelineService scanPipelineService;
    private final ScanJobRepository scanJobRepository;
    private final FindingRepository findingRepository;
    private final FindingLocationRepository findingLocationRepository;
    private final CoverageRecordRepository coverageRecordRepository;
    private final CoverageItemRepository coverageItemRepository;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    /**
     * Triggers a snapshot and git history scan on an active monitored repository (FR-025).
     */
    @PostMapping("/trigger")
    @RequireAuth
    public ResponseEntity<ScanTriggerResponse> triggerScan(
        @CurrentUser UserSession session,
        @RequestBody(required = false) ScanTriggerRequest request
    ) {
        UUID repositoryId = null;
        String branchName = null;
        Path sourcePath = null;

        if (request != null) {
            repositoryId = request.repositoryId();
            branchName = request.branchName();
            if (request.sourcePath() != null && !request.sourcePath().isBlank()) {
                sourcePath = Path.of(request.sourcePath().trim());
            }
        }

        // If repositoryId is not provided in request, resolve from active monitored project
        if (repositoryId == null) {
            Optional<MonitoredProject> currentProject = projectService.getCurrentProject(session);
            if (currentProject.isPresent()) {
                MonitoredProject project = currentProject.get();
                if (branchName == null || branchName.isBlank()) {
                    branchName = project.getPrimaryBranch();
                }

                // Resolve or synchronize RepositoryEntity in PostgreSQL
                UserEntity user = userRepository.findByGithubUserId(session.getGithubUserId())
                    .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .githubUserId(session.getGithubUserId())
                        .login(session.getLogin())
                        .name(session.getName())
                        .email(session.getEmail())
                        .avatarUrl(session.getAvatarUrl())
                        .createdAt(Instant.now())
                        .build()));

                RepositoryEntity repo = repositoryRepository.findByUserIdAndGithubRepoId(user.getId(), project.getGithubRepoId())
                    .orElseGet(() -> repositoryRepository.save(RepositoryEntity.builder()
                        .userId(user.getId())
                        .githubRepoId(project.getGithubRepoId())
                        .owner(project.getOwner())
                        .name(project.getName())
                        .fullName(project.getFullName())
                        .defaultBranch(project.getDefaultBranch())
                        .primaryBranch(project.getPrimaryBranch())
                        .isPrivate(project.isPrivate())
                        .status("ACTIVE")
                        .monitoredAt(Instant.now())
                        .build()));

                repositoryId = repo.getId();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ScanTriggerResponse(null, null, null, "FAILED", "No active monitored repository selected"));
            }
        }

        if (branchName == null || branchName.isBlank()) {
            branchName = "main";
        }

        log.info("Triggering scan for repositoryId={} on branch={}", repositoryId, branchName);
        ScanJobEntity job = scanPipelineService.executeScan(repositoryId, branchName, sourcePath);

        return ResponseEntity.ok(new ScanTriggerResponse(
            job.getId(),
            repositoryId,
            branchName,
            job.getStatus(),
            "Scan executed successfully"
        ));
    }

    /**
     * Retrieves scan job status, telemetry, and duration.
     */
    @GetMapping("/jobs/{jobId}")
    @RequireAuth
    public ResponseEntity<ScanJobDto> getScanJob(
        @CurrentUser UserSession session,
        @PathVariable UUID jobId
    ) {
        return scanJobRepository.findById(jobId)
            .map(ScanJobDto::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Retrieves all findings for a repository with severity, lifecycle state, and remediation quality.
     */
    @GetMapping("/repositories/{repositoryId}/findings")
    @RequireAuth
    public ResponseEntity<List<FindingDto>> getFindings(
        @CurrentUser UserSession session,
        @PathVariable UUID repositoryId
    ) {
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
     */
    @GetMapping("/repositories/{repositoryId}/coverage")
    @RequireAuth
    public ResponseEntity<CoverageSummaryDto> getCoverage(
        @CurrentUser UserSession session,
        @PathVariable UUID repositoryId
    ) {
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
