package com.scanpilot.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"accessToken"})
public class UserSession {

    private String sessionId;
    private Long githubUserId;
    private String login;
    private String name;
    private String avatarUrl;
    private String email;
    private String accessToken;
    private Long installationId;
    private Instant createdAt;
    private Instant expiresAt;

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
        this(sessionId, githubUserId, login, name, avatarUrl, email, accessToken, null, createdAt, expiresAt);
    }

    public UserSession withInstallationId(Long newInstallationId) {
        return new UserSession(
                this.sessionId,
                this.githubUserId,
                this.login,
                this.name,
                this.avatarUrl,
                this.email,
                this.accessToken,
                newInstallationId,
                this.createdAt,
                this.expiresAt
        );
    }

    public boolean isExpired() {
        return isExpired(Instant.now());
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }
}
