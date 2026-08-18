package com.scanpilot.auth.controller;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.dto.GitHubUserDto;
import com.scanpilot.auth.dto.UserProfileDto;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.GitHubOAuthService;
import com.scanpilot.auth.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final GitHubOAuthService gitHubOAuthService;
    private final SessionService sessionService;
    private final AuthConfigProperties properties;

    public AuthController(
            GitHubOAuthService gitHubOAuthService,
            SessionService sessionService,
            AuthConfigProperties properties
    ) {
        this.gitHubOAuthService = gitHubOAuthService;
        this.sessionService = sessionService;
        this.properties = properties;
    }

    /**
     * Initiates GitHub OAuth flow by redirecting user to GitHub's authorization endpoint.
     */
    @GetMapping("/github/login")
    public ResponseEntity<Void> loginWithGitHub() {
        String authUrl = gitHubOAuthService.generateAuthorizationUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    /**
     * OAuth callback endpoint invoked by GitHub after user authorization.
     */
    @GetMapping("/github/callback")
    public ResponseEntity<Void> handleGitHubCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        if (error != null && !error.isBlank()) {
            log.warn("OAuth authorization denied or error returned: error={}, description={}", error, errorDescription);
            return buildFrontendErrorRedirect(error);
        }

        if (state == null || !gitHubOAuthService.validateAndConsumeState(state)) {
            log.warn("Invalid or expired OAuth state parameter");
            return buildFrontendErrorRedirect("invalid_state");
        }

        if (code == null || code.isBlank()) {
            log.warn("Missing authorization code in OAuth callback");
            return buildFrontendErrorRedirect("missing_code");
        }

        try {
            String accessToken = gitHubOAuthService.exchangeCodeForAccessToken(code);
            GitHubUserDto gitHubUser = gitHubOAuthService.fetchUserProfile(accessToken);
            UserSession session = sessionService.createSession(gitHubUser, accessToken);

            ResponseCookie sessionCookie = sessionService.createSessionCookie(session.getSessionId());

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(properties.getFrontendUrl()))
                    .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                    .build();
        } catch (Exception e) {
            log.error("OAuth authentication flow failed", e);
            return buildFrontendErrorRedirect("oauth_failed");
        }
    }

    /**
     * Returns the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(@CurrentUser UserSession session) {
        if (session == null || session.isExpired()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(UserProfileDto.from(session));
    }

    /**
     * Terminates the active session and clears the session cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CurrentUser UserSession session,
            HttpServletRequest request
    ) {
        if (session != null) {
            sessionService.invalidateSession(session.getSessionId());
        } else {
            // Also attempt to invalidate based on cookie value directly if session object was not resolved
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                Arrays.stream(cookies)
                        .filter(c -> properties.getCookieName().equals(c.getName()))
                        .map(Cookie::getValue)
                        .forEach(sessionService::invalidateSession);
            }
        }

        ResponseCookie logoutCookie = sessionService.createLogoutCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, logoutCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    private ResponseEntity<Void> buildFrontendErrorRedirect(String errorCode) {
        String redirectUrl = UriComponentsBuilder.fromUriString(properties.getFrontendUrl())
                .queryParam("auth_error", errorCode)
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
