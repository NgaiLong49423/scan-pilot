package com.scanpilot.scanner.remediation;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.service.GitHubAppAuthService;
import com.scanpilot.github.service.GitHubPullRequestClient;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.FindingRemediationPrLinkEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRemediationPrLinkRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.dto.CreateFindingRemediationPrRequest;
import com.scanpilot.scanner.dto.FindingRemediationPrLinkDto;
import com.scanpilot.scanner.dto.FindingRemediationPrPreviewDto;
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
 * Orchestration service for Spring Boot safe remediation PR generation, validation, and execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindingRemediationPrService {

    public static final String REVOCATION_WARNING =
            "WARNING: Creating and merging this Pull Request replaces hardcoded credentials with environment variable placeholders, but does NOT revoke the exposed secret. You must immediately revoke and rotate the secret in your cloud/service provider console.";

    private final FindingRepository findingRepository;
    private final FindingLocationRepository findingLocationRepository;
    private final FindingRemediationPrLinkRepository linkRepository;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final SpringConfigurationPatcher patcher;
    private final FindingRemediationPrTokenService tokenService;
    private final GitHubPullRequestClient gitHubPullRequestClient;
    private final GitHubAppAuthService gitHubAppAuthService;

    /**
     * Generates a preview diff and signed token for creating a remediation PR.
     */
    public FindingRemediationPrPreviewDto generatePreview(UUID findingId, UserSession session) {
        if (session == null || session.getGithubUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (findingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding ID must not be null");
        }

        FindingEntity finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));

        if (!"SP-CONFIG-001".equalsIgnoreCase(finding.getRuleId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MANUAL_REMEDIATION_REQUIRED: Remediation PR is only supported for SP-CONFIG-001");
        }

        RepositoryEntity repo = repositoryRepository.findById(finding.getRepositoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));

        UserEntity user = userRepository.findByGithubUserId(session.getGithubUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!repo.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to repository");
        }

        Long installationId = repo.getInstallationId();
        if (installationId == null || !gitHubAppAuthService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "GitHub App installation required to create remediation PRs");
        }

        List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(findingId);
        FindingLocationEntity location = (locations != null && !locations.isEmpty()) ? locations.get(0) : null;
        if (location == null || location.getFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MANUAL_REMEDIATION_REQUIRED: Missing finding location");
        }

        String filePath = location.getFilePath();
        int lineNumber = location.getStartLine() != null ? location.getStartLine() : 1;

        if (!patcher.isSupportedConfigFile(filePath)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MANUAL_REMEDIATION_REQUIRED: File type not supported for automated remediation");
        }

        String installationToken = gitHubAppAuthService.createInstallationAccessToken(installationId);
        GitHubPullRequestClient.DefaultBranchHead head = gitHubPullRequestClient.getDefaultBranchHead(repo.getOwner(), repo.getName(), installationToken);
        String targetCommitSha = head.commitSha();
        String targetBranch = head.branchName();

        String fileContent = gitHubPullRequestClient.getFileContent(repo.getOwner(), repo.getName(), filePath, targetCommitSha, installationToken);

        SpringConfigurationPatcher.PatchResult patchResult = patcher.createPatch(filePath, fileContent, lineNumber, null);
        if (!patchResult.success()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, patchResult.failureCode() != null ? patchResult.failureCode() : "MANUAL_REMEDIATION_REQUIRED");
        }

        String patchPlanHash = tokenService.computePatchPlanHash(findingId, targetCommitSha, filePath, lineNumber, patchResult.patchedLine());
        String previewToken = tokenService.generateToken(findingId, repo.getId(), targetCommitSha, patchPlanHash);

        String shortId = findingId.toString().substring(0, 8);
        String remediationBranchName = "scanpilot/remediation-" + shortId;

        Optional<FindingRemediationPrLinkEntity> linkOpt = linkRepository.findByFindingId(findingId);
        boolean alreadyLinked = false;
        Integer existingPrNumber = null;
        String existingPrUrl = null;
        String linkState = null;

        if (linkOpt.isPresent()) {
            FindingRemediationPrLinkEntity existing = linkOpt.get();
            linkState = existing.getState();
            if ("CREATED".equalsIgnoreCase(existing.getState())) {
                alreadyLinked = true;
                existingPrNumber = existing.getGithubPrNumber();
                existingPrUrl = existing.getGithubPrUrl();
            }
        }

        return new FindingRemediationPrPreviewDto(
                findingId,
                repo.getId(),
                filePath,
                lineNumber,
                targetCommitSha,
                targetBranch,
                remediationBranchName,
                patchResult.originalLineMasked(),
                patchResult.patchedLine(),
                patchResult.envVariableName(),
                previewToken,
                Instant.now().plusSeconds(FindingRemediationPrTokenService.TOKEN_TTL_SECONDS),
                REVOCATION_WARNING,
                alreadyLinked,
                existingPrNumber,
                existingPrUrl,
                linkState
        );
    }

    /**
     * Confirms and creates the remediation branch, commit, and Pull Request.
     */
    public FindingRemediationPrLinkDto createRemediationPr(UUID findingId, CreateFindingRemediationPrRequest request, UserSession session) {
        if (session == null || session.getGithubUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (findingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding ID must not be null");
        }
        if (request == null || request.previewToken() == null || request.previewToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "previewToken must not be blank");
        }

        FindingRemediationPrTokenService.VerifiedRemediationToken token;
        try {
            token = tokenService.validateToken(request.previewToken(), findingId, null, null);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED_OR_INVALID");
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

        Long installationId = repo.getInstallationId();
        if (installationId == null || !gitHubAppAuthService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "GitHub App installation required to create remediation PRs");
        }

        List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(findingId);
        FindingLocationEntity location = (locations != null && !locations.isEmpty()) ? locations.get(0) : null;
        if (location == null || location.getFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MANUAL_REMEDIATION_REQUIRED: Missing finding location");
        }

        String filePath = location.getFilePath();
        int lineNumber = location.getStartLine() != null ? location.getStartLine() : 1;

        String installationToken = gitHubAppAuthService.createInstallationAccessToken(installationId);
        GitHubPullRequestClient.DefaultBranchHead head = gitHubPullRequestClient.getDefaultBranchHead(repo.getOwner(), repo.getName(), installationToken);

        // Verify that default branch HEAD matches the preview target commit
        if (!head.commitSha().equalsIgnoreCase(token.targetCommitSha())) {
            log.warn("Default branch HEAD ({}) differs from token target SHA ({})", head.commitSha(), token.targetCommitSha());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STALE_REVISION_ERROR: Target default branch HEAD has changed since preview generation. Please refresh the preview.");
        }

        String targetBranch = head.branchName();
        String targetCommitSha = token.targetCommitSha();

        // Check if existing PR link exists
        Optional<FindingRemediationPrLinkEntity> existingOpt = linkRepository.findByFindingIdAndSourceRevisionCommit(findingId, targetCommitSha);
        if (existingOpt.isPresent()) {
            FindingRemediationPrLinkEntity existing = existingOpt.get();
            if ("CREATED".equalsIgnoreCase(existing.getState())) {
                return mapToDto(existing);
            }
        }

        String fileContent = gitHubPullRequestClient.getFileContent(repo.getOwner(), repo.getName(), filePath, targetCommitSha, installationToken);
        SpringConfigurationPatcher.PatchResult patchResult = patcher.createPatch(filePath, fileContent, lineNumber, null);
        if (!patchResult.success()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, patchResult.failureCode() != null ? patchResult.failureCode() : "MANUAL_REMEDIATION_REQUIRED");
        }

        String expectedPatchPlanHash = tokenService.computePatchPlanHash(findingId, targetCommitSha, filePath, lineNumber, patchResult.patchedLine());
        if (!expectedPatchPlanHash.equalsIgnoreCase(token.patchPlanHash())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PREVIEW_TOKEN_EXPIRED_OR_INVALID: File contents or patch plan have changed");
        }

        String shortId = findingId.toString().substring(0, 8);
        String remediationBranchName = "scanpilot/remediation-" + shortId;
        String idempotencyMarker = "remediation-pr:" + findingId + ":" + targetCommitSha;

        FindingRemediationPrLinkEntity linkEntity = existingOpt.orElseGet(() -> FindingRemediationPrLinkEntity.builder()
                .findingId(findingId)
                .repositoryId(repo.getId())
                .sourceRevisionCommit(targetCommitSha)
                .targetBranch(targetBranch)
                .headBranch(remediationBranchName)
                .state("PENDING")
                .idempotencyMarker(idempotencyMarker)
                .createdByUserId(user.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        linkEntity.setState("PENDING");
        linkEntity.setUpdatedAt(Instant.now());
        try {
            linkEntity = linkRepository.saveAndFlush(linkEntity);
        } catch (DataIntegrityViolationException e) {
            log.info("Concurrent PR creation detected for finding {}", findingId);
            Optional<FindingRemediationPrLinkEntity> concurrent = linkRepository.findByFindingIdAndSourceRevisionCommit(findingId, targetCommitSha);
            if (concurrent.isPresent() && "CREATED".equalsIgnoreCase(concurrent.get().getState())) {
                return mapToDto(concurrent.get());
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrent remediation PR operation in progress");
        }

        try {
            // 1. Create remediation branch
            gitHubPullRequestClient.createBranch(repo.getOwner(), repo.getName(), remediationBranchName, targetCommitSha, installationToken);

            // 2. Commit patched file
            String commitMessage = String.format("fix(security): replace hardcoded secret with ${%s} in %s (SP-CONFIG-001)", patchResult.envVariableName(), filePath);
            gitHubPullRequestClient.updateFileContent(repo.getOwner(), repo.getName(), filePath, commitMessage, patchResult.patchedContent(), remediationBranchName, installationToken);

            // 3. Open Pull Request
            String prTitle = String.format("fix(security): remediate SP-CONFIG-001 secret in %s", filePath);
            String prBody = buildPrBody(finding, filePath, lineNumber, patchResult.envVariableName(), repo.getOwner(), repo.getName());
            GitHubPullRequestClient.GitHubPrResult prResult = gitHubPullRequestClient.createPullRequest(
                    repo.getOwner(),
                    repo.getName(),
                    prTitle,
                    prBody,
                    remediationBranchName,
                    targetBranch,
                    installationToken
            );

            // 4. Update link entity to CREATED
            linkEntity.setState("CREATED");
            linkEntity.setGithubPrNumber(prResult.prNumber());
            linkEntity.setGithubPrUrl(prResult.htmlUrl());
            linkEntity.setFailureReason(null);
            linkEntity.setUpdatedAt(Instant.now());
            linkEntity = linkRepository.save(linkEntity);

            return mapToDto(linkEntity);
        } catch (Exception e) {
            log.error("Failed to create remediation branch or PR on GitHub for finding {}", findingId, e);
            linkEntity.setState("FAILED");
            linkEntity.setFailureReason(e.getMessage() != null && e.getMessage().length() > 64 ? e.getMessage().substring(0, 64) : e.getMessage());
            linkEntity.setUpdatedAt(Instant.now());
            linkRepository.save(linkEntity);

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to create GitHub remediation Pull Request: " + e.getMessage());
        }
    }

    /**
     * Retrieves the persisted remediation PR link for a finding.
     */
    public FindingRemediationPrLinkDto getRemediationPrLink(UUID findingId, UserSession session) {
        if (session == null || session.getGithubUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (findingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding ID must not be null");
        }

        FindingRemediationPrLinkEntity link = linkRepository.findByFindingId(findingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No remediation PR link found for finding"));

        return mapToDto(link);
    }

    private String buildPrBody(FindingEntity finding, String filePath, int lineNumber, String envVarName, String owner, String repo) {
        return String.format("""
## Scan Pilot Automated Security Remediation (SP-CONFIG-001)

> [!CAUTION]
> **MANDATORY REVOCATION & ROTATION NOTICE**
> Merging this Pull Request replaces the hardcoded secret in configuration with the environment variable placeholder `${%s}`.
> **This action DOES NOT revoke or invalidate the exposed credential.** You must immediately revoke and rotate the exposed secret in your provider console.

### Summary of Changes
- **Rule ID:** `SP-CONFIG-001 — Source Code Secret Exposure`
- **File:** `%s` (Line %d)
- **Environment Variable:** `${%s}`
- **Repository:** `%s/%s`

### Recommended Deployment Steps
1. Set the environment variable `%s` in your production / CI/CD environment with your rotated credential.
2. Verify application startup with the new environment configuration.
3. Merge this Pull Request to remove the hardcoded credential from source control.
4. Revoke the old credential in the service provider dashboard.

---
*Generated safely by Scan Pilot with zero raw secret exposure.*
""", envVarName, filePath, lineNumber, envVarName, owner, repo, envVarName);
    }

    private FindingRemediationPrLinkDto mapToDto(FindingRemediationPrLinkEntity entity) {
        return new FindingRemediationPrLinkDto(
                entity.getId(),
                entity.getFindingId(),
                entity.getRepositoryId(),
                entity.getSourceRevisionCommit(),
                entity.getTargetBranch(),
                entity.getHeadBranch(),
                entity.getState(),
                entity.getGithubPrNumber(),
                entity.getGithubPrUrl(),
                entity.getIdempotencyMarker(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}