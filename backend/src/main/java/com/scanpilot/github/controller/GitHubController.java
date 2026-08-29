package com.scanpilot.github.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.dto.GitHubRepositoryDto;
import com.scanpilot.github.dto.InstallUrlResponse;
import com.scanpilot.github.service.GitHubAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubAppService gitHubAppService;

    /**
     * Returns the GitHub App installation URL with a single-use opaque state token.
     */
    @GetMapping("/install-url")
    @RequireAuth
    public ResponseEntity<InstallUrlResponse> getInstallUrl(@CurrentUser UserSession session) {
        String url = gitHubAppService.getInstallUrl(session);
        return ResponseEntity.ok(new InstallUrlResponse(url));
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
