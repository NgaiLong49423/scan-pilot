package com.scanpilot.auth.model;

import java.time.Instant;

public class UserSession {

    private final String sessionId;
    private final Long githubUserId;
    private final String login;
    private final String name;
    private final String avatarUrl;
    private final String email;
    private final String accessToken;
    private final Instant createdAt;
    private final Instant expiresAt;

    public UserSession(
            String sessionId,
            Long githubUserId,
            String login,
            String name,
            String avatarUrl,
            String email,
            String accessToken,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.sessionId = sessionId;
        this.githubUserId = githubUserId;
        this.login = login;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.email = email;
        this.accessToken = accessToken;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getGithubUserId() {
        return githubUserId;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Server-only access token. Must never be exposed via public APIs or logs.
     */
    public String getAccessToken() {
        return accessToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return isExpired(Instant.now());
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
