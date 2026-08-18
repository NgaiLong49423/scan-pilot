package com.scanpilot.auth.service;

import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.dto.GitHubTokenResponse;
import com.scanpilot.auth.dto.GitHubUserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GitHubOAuthService {

    public static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize";
    public static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    public static final String GITHUB_USER_API_URL = "https://api.github.com/user";
    public static final String DEFAULT_SCOPES = "read:user,user:email";

    private final AuthConfigProperties properties;
    private final RestClient restClient;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();

    public GitHubOAuthService(AuthConfigProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Generates a cryptographically secure random state, caches it with TTL,
     * and constructs the GitHub OAuth authorization URL.
     */
    public String generateAuthorizationUrl() {
        cleanExpiredStates();
        String state = generateSecureState();
        Instant expiresAt = Instant.now().plusSeconds(properties.getStateTtlSeconds());
        pendingStates.put(state, expiresAt);

        return UriComponentsBuilder.fromUriString(GITHUB_AUTH_URL)
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("scope", DEFAULT_SCOPES)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /**
     * Validates and consumes the OAuth state parameter (one-time use for CSRF protection).
     */
    public boolean validateAndConsumeState(String state) {
        if (state == null || state.isBlank()) {
            return false;
        }
        Instant expiresAt = pendingStates.remove(state);
        if (expiresAt == null) {
            return false;
        }
        return Instant.now().isBefore(expiresAt);
    }

    /**
     * Exchanges an authorization code for a GitHub OAuth access token.
     */
    public String exchangeCodeForAccessToken(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Authorization code cannot be null or blank");
        }

        Map<String, String> requestBody = Map.of(
                "client_id", properties.getClientId(),
                "client_secret", properties.getClientSecret(),
                "code", code,
                "redirect_uri", properties.getRedirectUri()
        );

        GitHubTokenResponse response = restClient.post()
                .uri(GITHUB_TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GitHubTokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            String errorMsg = response != null && response.errorDescription() != null
                ? response.errorDescription()
                : (response != null && response.error() != null ? response.error() : "Failed to obtain access token from GitHub");
            throw new IllegalStateException("GitHub OAuth token exchange failed: " + errorMsg);
        }

        return response.accessToken();
    }

    /**
     * Fetches the authenticated user profile from GitHub API.
     */
    public GitHubUserDto fetchUserProfile(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be null or blank");
        }

        GitHubUserDto user = restClient.get()
                .uri(GITHUB_USER_API_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(GitHubUserDto.class);

        if (user == null || user.id() == null) {
            throw new IllegalStateException("Failed to fetch user profile from GitHub API");
        }

        return user;
    }

    public void cleanExpiredStates() {
        Instant now = Instant.now();
        pendingStates.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

    public void clearAllStates() {
        pendingStates.clear();
    }

    private String generateSecureState() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
