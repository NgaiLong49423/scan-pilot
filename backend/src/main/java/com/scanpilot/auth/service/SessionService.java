package com.scanpilot.auth.service;

import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.dto.GitHubUserDto;
import com.scanpilot.auth.model.UserSession;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final AuthConfigProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, UserSession> sessionStore = new ConcurrentHashMap<>();

    public SessionService(AuthConfigProperties properties) {
        this.properties = properties;
    }

    public UserSession createSession(GitHubUserDto user, String accessToken) {
        return createSession(
                user.id(),
                user.login(),
                user.name(),
                user.avatarUrl(),
                user.email(),
                accessToken
        );
    }

    public UserSession createSession(
            Long githubUserId,
            String login,
            String name,
            String avatarUrl,
            String email,
            String accessToken
    ) {
        String sessionId = generateSecureSessionId();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getSessionTtlSeconds());

        UserSession session = new UserSession(
                sessionId,
                githubUserId,
                login,
                name,
                avatarUrl,
                email,
                accessToken,
                now,
                expiresAt
        );

        sessionStore.put(sessionId, session);
        return session;
    }

    public Optional<UserSession> getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        UserSession session = sessionStore.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }

        if (session.isExpired()) {
            sessionStore.remove(sessionId);
            return Optional.empty();
        }

        return Optional.of(session);
    }

    public void invalidateSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessionStore.remove(sessionId);
        }
    }

    public ResponseCookie createSessionCookie(String sessionId) {
        return ResponseCookie.from(properties.getCookieName(), sessionId)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(properties.getSessionTtlSeconds()))
                .build();
    }

    public ResponseCookie createLogoutCookie() {
        return ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
    }

    public void cleanExpiredSessions() {
        Instant now = Instant.now();
        sessionStore.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    public int getActiveSessionCount() {
        cleanExpiredSessions();
        return sessionStore.size();
    }

    public void clearAllSessions() {
        sessionStore.clear();
    }

    private String generateSecureSessionId() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
