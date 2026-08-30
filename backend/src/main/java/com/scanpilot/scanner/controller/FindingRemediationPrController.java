package com.scanpilot.scanner.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.scanner.dto.CreateFindingRemediationPrRequest;
import com.scanpilot.scanner.dto.FindingRemediationPrLinkDto;
import com.scanpilot.scanner.dto.FindingRemediationPrPreviewDto;
import com.scanpilot.scanner.remediation.FindingRemediationPrService;
import jakarta.validation.Valid;
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

import java.util.UUID;

/**
 * REST controller for secret-safe GitHub Remediation Pull Request preview and creation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/findings")
@RequiredArgsConstructor
public class FindingRemediationPrController {

    private final FindingRemediationPrService remediationPrService;

    /**
     * Generates a secret-safe remediation preview with masked diff, environment variable placeholder, and signed token.
     */
    @GetMapping("/{findingId}/remediation-pr-preview")
    @RequireAuth
    public ResponseEntity<FindingRemediationPrPreviewDto> getRemediationPrPreview(
            @CurrentUser UserSession session,
            @PathVariable UUID findingId
    ) {
        FindingRemediationPrPreviewDto preview = remediationPrService.generatePreview(findingId, session);
        return ResponseEntity.ok(preview);
    }

    /**
     * Confirms and creates the remediation branch, commit, and Pull Request on GitHub.
     */
    @PostMapping("/{findingId}/remediation-pr")
    @RequireAuth
    public ResponseEntity<FindingRemediationPrLinkDto> createRemediationPr(
            @CurrentUser UserSession session,
            @PathVariable UUID findingId,
            @Valid @RequestBody CreateFindingRemediationPrRequest request
    ) {
        FindingRemediationPrLinkDto link = remediationPrService.createRemediationPr(findingId, request, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    /**
     * Retrieves the persisted GitHub remediation PR link for a finding.
     */
    @GetMapping("/{findingId}/remediation-pr")
    @RequireAuth
    public ResponseEntity<FindingRemediationPrLinkDto> getRemediationPrLink(
            @CurrentUser UserSession session,
            @PathVariable UUID findingId
    ) {
        FindingRemediationPrLinkDto link = remediationPrService.getRemediationPrLink(findingId, session);
        return ResponseEntity.ok(link);
    }
}