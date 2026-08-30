package com.scanpilot.auth.service;

import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.dto.GitHubUserDto;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserSessionEntity;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.persistence.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final AuthConfigProperties properties;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
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

    @Transactional
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

        UserEntity userEntity = userRepository.findByGithubUserId(githubUserId)
                .map(existing -> {
                    existing.setLogin(login);
                    existing.setName(name);
                    existing.setAvatarUrl(avatarUrl);
                    existing.setEmail(email);
                    existing.setUpdatedAt(now);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .githubUserId(githubUserId)
                        .login(login)
                        .name(name)
                        .avatarUrl(avatarUrl)
                        .email(email)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));

        UserSessionEntity sessionEntity = UserSessionEntity.builder()
                .sessionId(sessionId)
                .userId(userEntity.getId())
                .accessToken(accessToken)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        userSessionRepository.save(sessionEntity);

        return UserSession.builder()
                .sessionId(sessionId)
                .githubUserId(githubUserId)
                .login(login)
                .name(name)
                .avatarUrl(avatarUrl)
                .email(email)
                .accessToken(accessToken)
                .installationId(null)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public Optional<UserSession> getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        Optional<UserSessionEntity> entityOpt = userSessionRepository.findBySessionId(sessionId);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        UserSessionEntity entity = entityOpt.get();
        Instant now = Instant.now();
        if (entity.getExpiresAt() != null && now.isAfter(entity.getExpiresAt())) {
            userSessionRepository.deleteBySessionId(sessionId);
            return Optional.empty();
        }

        Optional<UserEntity> userOpt = userRepository.findById(entity.getUserId());
        if (userOpt.isEmpty()) {
            userSessionRepository.deleteBySessionId(sessionId);
            return Optional.empty();
        }

        UserEntity user = userOpt.get();
        UserSession session = UserSession.builder()
                .sessionId(entity.getSessionId())
                .githubUserId(user.getGithubUserId())
                .login(user.getLogin())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .accessToken(entity.getAccessToken())
                .installationId(entity.getInstallationId())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();

        return Optional.of(session);
    }

    @Transactional
    public Optional<UserSession> updateInstallationId(String sessionId, Long installationId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        Optional<UserSessionEntity> entityOpt = userSessionRepository.findBySessionId(sessionId);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        UserSessionEntity entity = entityOpt.get();
        Instant now = Instant.now();
        if (entity.getExpiresAt() != null && now.isAfter(entity.getExpiresAt())) {
            userSessionRepository.deleteBySessionId(sessionId);
            return Optional.empty();
        }

        entity.setInstallationId(installationId);
        userSessionRepository.save(entity);

        Optional<UserEntity> userOpt = userRepository.findById(entity.getUserId());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        UserEntity user = userOpt.get();
        return Optional.of(UserSession.builder()
                .sessionId(entity.getSessionId())
                .githubUserId(user.getGithubUserId())
                .login(user.getLogin())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .accessToken(entity.getAccessToken())
                .installationId(entity.getInstallationId())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build());
    }

    @Transactional
    public void invalidateSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            userSessionRepository.deleteBySessionId(sessionId);
        }
    }

    public ResponseCookie createSessionCookie(String sessionId) {
        return ResponseCookie.from(properties.getCookieName(), sessionId)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .path("/")
                .sameSite(properties.isCookieSecure() ? "None" : "Lax")
                .maxAge(Duration.ofSeconds(properties.getSessionTtlSeconds()))
                .build();
    }

    public ResponseCookie createLogoutCookie() {
        return ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .path("/")
                .sameSite(properties.isCookieSecure() ? "None" : "Lax")
                .maxAge(0)
                .build();
    }

    @Transactional
    public void cleanExpiredSessions() {
        userSessionRepository.deleteByExpiresAtBefore(Instant.now());
    }

    @Transactional(readOnly = true)
    public int getActiveSessionCount() {
        return (int) userSessionRepository.count();
    }

    @Transactional
    public void clearAllSessions() {
        userSessionRepository.deleteAll();
    }

    private String generateSecureSessionId() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
