package com.scanpilot.scanner.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.scanner.dto.CreateFindingIssueRequest;
import com.scanpilot.scanner.dto.FindingIssueLinkDto;
import com.scanpilot.scanner.dto.FindingIssuePreviewDto;
import com.scanpilot.scanner.issue.FindingIssueService;
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
 * REST controller for secret-safe GitHub issue preview, validation, and creation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/findings")
@RequiredArgsConstructor
public class FindingIssueController {

    private final FindingIssueService findingIssueService;

    /**
     * Generates a secret-safe canonical issue draft preview with an expiring signed previewToken.
     */
    @GetMapping("/{findingId}/issue-preview")
    @RequireAuth
    public ResponseEntity<FindingIssuePreviewDto> getIssuePreview(
            @CurrentUser UserSession session,
            @PathVariable UUID findingId
    ) {
        FindingIssuePreviewDto preview = findingIssueService.generatePreview(findingId, session);
        return ResponseEntity.ok(preview);
    }

    /**
     * Confirms and creates a GitHub issue using the signed previewToken.
     */
    @PostMapping("/{findingId}/issue")
    @RequireAuth
    public ResponseEntity<FindingIssueLinkDto> createIssue(
            @CurrentUser UserSession session,
            @PathVariable UUID findingId,
            @Valid @RequestBody CreateFindingIssueRequest request
    ) {
        FindingIssueLinkDto link = findingIssueService.createIssue(findingId, request, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    /**
     * Retrieves the persisted GitHub issue link for a finding.
     */
    @GetMapping("/{findingId}/issue")
    @RequireAuth
    public ResponseEntity<FindingIssueLinkDto> getIssueLink(
            @CurrentUser UserSession session,
            @PathVariable UUID findingId
    ) {
        FindingIssueLinkDto link = findingIssueService.getIssueLink(findingId, session);
        return ResponseEntity.ok(link);
    }
}
