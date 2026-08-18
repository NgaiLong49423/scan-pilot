package com.scanpilot.github.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.dto.GitHubRepositoryDto;
import com.scanpilot.github.dto.InstallUrlResponse;
import com.scanpilot.github.dto.LinkInstallationRequest;
import com.scanpilot.github.service.GitHubAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubAppService gitHubAppService;

    /**
     * Returns the GitHub App installation URL.
     */
    @GetMapping("/install-url")
    public ResponseEntity<InstallUrlResponse> getInstallUrl() {
        String url = gitHubAppService.getInstallUrl();
        return ResponseEntity.ok(new InstallUrlResponse(url));
    }

    /**
     * Links a GitHub App installation to the authenticated user's session.
     */
    @PostMapping("/installations/link")
    @RequireAuth
    public ResponseEntity<Map<String, Object>> linkInstallation(
            @CurrentUser UserSession session,
            @Valid @RequestBody LinkInstallationRequest request
    ) {
        gitHubAppService.linkInstallation(session, request.installationId());
        return ResponseEntity.ok(Map.of(
                "message", "Installation linked successfully",
                "installationId", request.installationId()
        ));
    }

    /**
     * Lists repositories accessible to the user via GitHub App installation or OAuth.
     */
    @GetMapping("/repositories")
    @RequireAuth
    public ResponseEntity<List<GitHubRepositoryDto>> getRepositories(@CurrentUser UserSession session) {
        List<GitHubRepositoryDto> repositories = gitHubAppService.getAccessibleRepositories(session);
        return ResponseEntity.ok(repositories);
    }
}
