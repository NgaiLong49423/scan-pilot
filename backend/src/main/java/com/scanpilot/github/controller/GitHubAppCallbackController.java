package com.scanpilot.github.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.github.dto.UserAccessibleInstallationDto;
import com.scanpilot.github.service.GitHubAppService;
import com.scanpilot.github.service.InstallationStateService;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserInstallationEntity;
import com.scanpilot.persistence.repository.UserInstallationRepository;
import com.scanpilot.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/github/installations")
@RequiredArgsConstructor
public class GitHubAppCallbackController {

    private final InstallationStateService installationStateService;
    private final GitHubAppService gitHubAppService;
    private final UserInstallationRepository userInstallationRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final AuthConfigProperties authConfigProperties;

    /**
     * GitHub App installation setup callback.
     * Invoked when user completes installation or modification on GitHub.
     */
    @GetMapping("/callback")
    @RequireAuth
    public ResponseEntity<Void> handleInstallationCallback(
            @CurrentUser UserSession session,
            @RequestParam(name = "installation_id", required = false) Long installationId,
            @RequestParam(name = "setup_action", required = false) String setupAction,
            @RequestParam(name = "state", required = false) String state
    ) {
        if (session == null) {
            log.warn("Installation callback rejected: missing active session");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (installationId == null || state == null || state.isBlank()) {
            log.warn("Installation callback rejected: missing installation_id or state");
            return buildRedirectWithError("missing_parameters");
        }

        UserEntity userEntity = userRepository.findByGithubUserId(session.getGithubUserId())
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .githubUserId(session.getGithubUserId())
                        .login(session.getLogin())
                        .name(session.getName())
                        .email(session.getEmail())
                        .avatarUrl(session.getAvatarUrl())
                        .createdAt(Instant.now())
                        .build()));
        UUID userId = userEntity.getId();

        // 1. Validate and atomically consume single-use opaque state token
        boolean stateValid = installationStateService.validateAndConsumeState(state, userId, session.getSessionId());
        if (!stateValid) {
            log.warn("Installation callback rejected: invalid, expired, or replayed state token");
            return buildRedirectWithError("invalid_state");
        }

        // 2. Level 1 Verification: Verify that the user access token has access to this installation
        List<UserAccessibleInstallationDto> accessibleInstallations;
        try {
            accessibleInstallations = gitHubAppService.getUserAccessibleInstallations(session.getAccessToken());
        } catch (Exception e) {
            log.warn("Installation callback rejected: failed to query user-accessible installations from GitHub");
            return buildRedirectWithError("github_api_error");
        }

        Optional<UserAccessibleInstallationDto> matchedInstallation = accessibleInstallations.stream()
                .filter(inst -> inst.id().equals(installationId))
                .findFirst();

        if (matchedInstallation.isEmpty()) {
            log.warn("Installation callback rejected: installation {} is not accessible to user", installationId);
            return buildRedirectWithError("unauthorized_installation");
        }

        UserAccessibleInstallationDto verifiedInst = matchedInstallation.get();

        // 3. Atomically persist / upsert verified association via native ON CONFLICT query
        userInstallationRepository.upsertUserInstallation(
                UUID.randomUUID(),
                userId,
                session.getGithubUserId(),
                installationId,
                verifiedInst.accountLogin(),
                verifiedInst.accountType(),
                Instant.now()
        );

        // 4. Update session with verified installation ID
        sessionService.updateInstallationId(session.getSessionId(), installationId);
        log.info("Successfully bound verified GitHub App installation {} to user {}", installationId, userId);

        String targetUrl = UriComponentsBuilder.fromUriString(authConfigProperties.getFrontendUrl())
                .queryParam("installation", "linked")
                .queryParam("installation_id", installationId)
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .build();
    }

    private ResponseEntity<Void> buildRedirectWithError(String error) {
        String targetUrl = UriComponentsBuilder.fromUriString(authConfigProperties.getFrontendUrl())
                .queryParam("installation_error", error)
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .build();
    }
}
