package com.scanpilot.ai.gemini;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.model.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Gemini AI Explanation and Remediation Guidance (FR-005, FR-048).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiExplanationController {

    private final GeminiExplanationService geminiExplanationService;

    /**
     * Triggers AI explanation for a finding and persists an AI_INFERENCE EvidenceItem.
     *
     * @param session   authenticated user session
     * @param findingId UUID of the finding to explain
     * @return structured explanation and remediation guidance
     */
    @PostMapping("/findings/{findingId}/explain")
    @RequireAuth
    public ResponseEntity<GeminiExplanationResponse> explainFinding(
        @CurrentUser UserSession session,
        @PathVariable UUID findingId
    ) {
        log.info("User {} requested AI explanation for findingId={}", session.getLogin(), findingId);
        GeminiExplanationResponse response = geminiExplanationService.explainAndPersist(findingId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves existing AI explanation for a finding.
     *
     * @param session   authenticated user session
     * @param findingId UUID of the finding
     * @return structured explanation if present, 404 NOT FOUND otherwise
     */
    @GetMapping("/findings/{findingId}/explanation")
    @RequireAuth
    public ResponseEntity<GeminiExplanationResponse> getExplanation(
        @CurrentUser UserSession session,
        @PathVariable UUID findingId
    ) {
        return geminiExplanationService.getExistingExplanation(findingId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
