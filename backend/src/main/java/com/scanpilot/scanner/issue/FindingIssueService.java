package com.scanpilot.scanner.issue;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.service.GitHubAppAuthService;
import com.scanpilot.github.service.GitHubAppService;
import com.scanpilot.github.service.GitHubIssueClient;
import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingIssueLinkEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingIssueLinkRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.dto.CreateFindingIssueRequest;
import com.scanpilot.scanner.dto.FindingIssueLinkDto;
import com.scanpilot.scanner.dto.FindingIssuePreviewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestration service for secret-safe GitHub issue preview, validation, and creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindingIssueService {

    public static final java.time.Duration STALE_PENDING_THRESHOLD = java.time.Duration.ofSeconds(60);

    private final FindingRepository findingRepository;
    private final FindingLocationRepository findingLocationRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final FindingIssueLinkRepository findingIssueLinkRepository;
    private final FindingIssueTemplateService templateService;
    private final FindingIssueTokenService tokenService;
    private final GitHubIssueClient gitHubIssueClient;
    private final GitHubAppAuthService gitHubAppAuthService;
    private final GitHubAppService gitHubAppService;

    /**
     * Generates a preview and signed previewToken for creating a GitHub issue from a finding.
     */
    public FindingIssuePreviewDto generatePreview(UUID findingId, UserSession session) {
        if (session == null || session.getGithubUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (findingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding ID must not be null");
        }

        FindingEntity finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));

        RepositoryEntity repo = repositoryRepository.findById(finding.getRepositoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));

        UserEntity user = userRepository.findByGithubUserId(session.getGithubUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!repo.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to repository");
        }

        Long installationId = session.getInstallationId() != null
                ? session.getInstallationId()
                : gitHubAppService.getInstallationId(session.getGithubUserId());

        if (installationId == null || !gitHubAppAuthService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "GitHub App installation required to create issues");
        }

        List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(findingId);
        FindingLocationEntity location = (locations != null && !locations.isEmpty()) ? locations.get(0) : null;

        List<EvidenceItemEntity> evidences = evidenceItemRepository.findByFindingId(findingId);
        EvidenceItemEntity evidence = (evidences != null && !evidences.isEmpty()) ? evidences.get(0) : null;

        String rawPath = location != null ? location.getFilePath() : null;
        String title = templateService.buildTitle(finding, rawPath);
        String body = templateService.buildBody(finding, location, evidence);

        long revision = computeRevision(finding);
        String draftHash = tokenService.computeDraftSha256(body);
        String previewToken = tokenService.generateToken(findingId, revision, draftHash);

        Optional<FindingIssueLinkEntity> linkOpt = findingIssueLinkRepository.findByFindingId(findingId);
        if (linkOpt.isPresent() && "CREATED".equalsIgnoreCase(linkOpt.get().getState())) {
            FindingIssueLinkEntity link = linkOpt.get();
            return new FindingIssuePreviewDto(
                findingId,
                title,
                body,
                previewToken,
                "CREATED",
                true,
                link.getGithubIssueNumber(),
                link.getGithubIssueUrl()
            );
        }

        return new FindingIssuePreviewDto(
            findingId,
            title,
            body,
            previewToken,
            linkOpt.map(FindingIssueLinkEntity::getState).orElse(null),
            false,
            null,
            null
        );
    }

    /**
     * Validates previewToken and creates a GitHub issue with durable state transitions.
     */
    public FindingIssueLinkDto createIssue(UUID findingId, CreateFindingIssueRequest request, UserSession session) {
        if (session == null || session.getGithubUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (findingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding ID must not be null");
        }
        if (request == null || request.previewToken() == null || request.previewToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "previewToken must not be blank");
        }

        FindingEntity finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));

        RepositoryEntity repo = repositoryRepository.findById(finding.getRepositoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));

        UserEntity user = userRepository.findByGithubUserId(session.getGithubUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!repo.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to repository");
        }

        Long installationId = session.getInstallationId() != null
                ? session.getInstallationId()
                : gitHubAppService.getInstallationId(session.getGithubUserId());

        if (installationId == null || !gitHubAppAuthService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "GitHub App installation required to create issues");
        }

        List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(findingId);
        FindingLocationEntity location = (locations != null && !locations.isEmpty()) ? locations.get(0) : null;

        List<EvidenceItemEntity> evidences = evidenceItemRepository.findByFindingId(findingId);
        EvidenceItemEntity evidence = (evidences != null && !evidences.isEmpty()) ? evidences.get(0) : null;

        String rawPath = location != null ? location.getFilePath() : null;
        String canonicalTitle = templateService.buildTitle(finding, rawPath);
        String canonicalBody = templateService.buildBody(finding, location, evidence);

        long currentRevision = computeRevision(finding);
        String currentDraftHash = tokenService.computeDraftSha256(canonicalBody);

        try {
            tokenService.validateToken(request.previewToken(), findingId, currentRevision, currentDraftHash);
        } catch (Exception e) {
            log.warn("Preview token validation failed for finding {}", findingId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED_OR_INVALID");
        }

        // State Machine Handling
        Optional<FindingIssueLinkEntity> linkOpt = findingIssueLinkRepository.findByFindingId(findingId);

        if (linkOpt.isPresent()) {
            FindingIssueLinkEntity existing = linkOpt.get();
            String state = existing.getState();

            if ("CREATED".equalsIgnoreCase(state)) {
                return FindingIssueLinkDto.from(existing);
            }

            if ("PENDING".equalsIgnoreCase(state)) {
                Instant updatedAt = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                if (updatedAt != null && java.time.Duration.between(updatedAt, Instant.now()).compareTo(STALE_PENDING_THRESHOLD) >= 0) {
                    // Stale PENDING recovery: atomically transition PENDING -> UNKNOWN
                    int updated = findingIssueLinkRepository.updateStateConditional(findingId, "PENDING", "UNKNOWN", Instant.now());
                    if (updated == 0) {
                        FindingIssueLinkEntity reloaded = findingIssueLinkRepository.findByFindingId(findingId).orElseThrow();
                        if ("CREATED".equalsIgnoreCase(reloaded.getState())) {
                            return FindingIssueLinkDto.from(reloaded);
                        }
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "CREATION_IN_PROGRESS");
                    }
                    state = "UNKNOWN";
                } else {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "CREATION_IN_PROGRESS");
                }
            }

            if ("UNKNOWN".equalsIgnoreCase(state)) {
                // Reconcile with GitHub across all issue pages
                String instToken = gitHubAppAuthService.createInstallationAccessToken(installationId);
                String marker = FindingIssueTemplateService.MARKER_PREFIX + findingId + FindingIssueTemplateService.MARKER_SUFFIX;
                Optional<GitHubIssueClient.GitHubIssueResult> matched;
                try {
                    matched = gitHubIssueClient.findIssueByMarker(repo.getOwner(), repo.getName(), instToken, marker);
                } catch (Exception e) {
                    log.warn("Marker reconciliation failed for finding {}", findingId);
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "GITHUB_ISSUE_CREATION_AMBIGUOUS");
                }

                if (matched.isPresent()) {
                    GitHubIssueClient.GitHubIssueResult found = matched.get();
                    existing.setState("CREATED");
                    existing.setGithubIssueNumber(found.issueNumber());
                    existing.setGithubIssueUrl(found.htmlUrl());
                    existing.setFailureReason(null);
                    existing.setUpdatedAt(Instant.now());
                    FindingIssueLinkEntity saved = findingIssueLinkRepository.saveAndFlush(existing);
                    return FindingIssueLinkDto.from(saved);
                }

                // Issue confirmed absent on GitHub: perform atomic conditional transition UNKNOWN -> PENDING
                int updated = findingIssueLinkRepository.updateStateConditional(findingId, "UNKNOWN", "PENDING", Instant.now());
                if (updated == 0) {
                    FindingIssueLinkEntity reloaded = findingIssueLinkRepository.findByFindingId(findingId).orElseThrow();
                    if ("CREATED".equalsIgnoreCase(reloaded.getState())) {
                        return FindingIssueLinkDto.from(reloaded);
                    }
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "CREATION_IN_PROGRESS");
                }
            } else if ("FAILED".equalsIgnoreCase(state)) {
                // Transition FAILED -> PENDING for retry
                int updated = findingIssueLinkRepository.updateStateConditional(findingId, "FAILED", "PENDING", Instant.now());
                if (updated == 0) {
                    FindingIssueLinkEntity reloaded = findingIssueLinkRepository.findByFindingId(findingId).orElseThrow();
                    if ("CREATED".equalsIgnoreCase(reloaded.getState())) {
                        return FindingIssueLinkDto.from(reloaded);
                    }
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "CREATION_IN_PROGRESS");
                }
            }
        } else {
            // Insert pre-write row with PENDING
            FindingIssueLinkEntity newLink = FindingIssueLinkEntity.builder()
                    .findingId(findingId)
                    .repositoryId(repo.getId())
                    .state("PENDING")
                    .idempotencyMarker("scanpilot-finding-" + findingId)
                    .createdByUserId(user.getId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            try {
                findingIssueLinkRepository.saveAndFlush(newLink);
            } catch (DataIntegrityViolationException e) {
                FindingIssueLinkEntity reloaded = findingIssueLinkRepository.findByFindingId(findingId).orElseThrow();
                if ("CREATED".equalsIgnoreCase(reloaded.getState())) {
                    return FindingIssueLinkDto.from(reloaded);
                }
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CREATION_IN_PROGRESS");
            }
        }

        // Execute remote call to GitHub outside database transaction
        String instToken = gitHubAppAuthService.createInstallationAccessToken(installationId);
        try {
            GitHubIssueClient.GitHubIssueResult result = gitHubIssueClient.createIssue(
                    repo.getOwner(),
                    repo.getName(),
                    instToken,
                    canonicalTitle,
                    canonicalBody
            );

            FindingIssueLinkEntity link = findingIssueLinkRepository.findByFindingId(findingId).orElseThrow();
            link.setState("CREATED");
            link.setGithubIssueNumber(result.issueNumber());
            link.setGithubIssueUrl(result.htmlUrl());
            link.setFailureReason(null);
            link.setUpdatedAt(Instant.now());
            FindingIssueLinkEntity saved = findingIssueLinkRepository.saveAndFlush(link);
            return FindingIssueLinkDto.from(saved);

        } catch (GitHubIssueClient.GitHubClientException ex) {
            FindingIssueLinkEntity link = findingIssueLinkRepository.findByFindingId(findingId).orElseThrow();
            link.setState("FAILED");
            link.setFailureReason("VALIDATION_ERROR");
            link.setUpdatedAt(Instant.now());
            findingIssueLinkRepository.saveAndFlush(link);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GITHUB_ISSUE_CREATION_REJECTED");

        } catch (GitHubIssueClient.GitHubAmbiguousException ex) {
            FindingIssueLinkEntity link = findingIssueLinkRepository.findByFindingId(findingId).orElseThrow();
            link.setState("UNKNOWN");
            link.setFailureReason("NETWORK_TIMEOUT");
            link.setUpdatedAt(Instant.now());
            findingIssueLinkRepository.saveAndFlush(link);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "GITHUB_ISSUE_CREATION_AMBIGUOUS");

        } catch (Exception ex) {
            FindingIssueLinkEntity link = findingIssueLinkRepository.findByFindingId(findingId).orElseThrow();
            link.setState("UNKNOWN");
            link.setFailureReason("REMOTE_5XX");
            link.setUpdatedAt(Instant.now());
            findingIssueLinkRepository.saveAndFlush(link);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "GITHUB_ISSUE_CREATION_AMBIGUOUS");
        }
    }

    /**
     * Retrieves the persisted GitHub issue link for a finding if present.
     */
    public FindingIssueLinkDto getIssueLink(UUID findingId, UserSession session) {
        if (session == null || session.getGithubUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (findingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding ID must not be null");
        }

        FindingEntity finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));

        RepositoryEntity repo = repositoryRepository.findById(finding.getRepositoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));

        UserEntity user = userRepository.findByGithubUserId(session.getGithubUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!repo.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to repository");
        }

        FindingIssueLinkEntity link = findingIssueLinkRepository.findByFindingId(findingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue link not found for finding"));

        return FindingIssueLinkDto.from(link);
    }

    private long computeRevision(FindingEntity finding) {
        if (finding.getLastSeenAt() != null) {
            return finding.getLastSeenAt().toEpochMilli();
        }
        if (finding.getFirstSeenAt() != null) {
            return finding.getFirstSeenAt().toEpochMilli();
        }
        return 0L;
    }
}
