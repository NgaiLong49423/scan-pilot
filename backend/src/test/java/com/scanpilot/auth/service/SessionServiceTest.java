package com.scanpilot.auth.service;

import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.dto.GitHubUserDto;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserSessionEntity;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.persistence.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    private AuthConfigProperties properties;
    private UserSessionRepository userSessionRepository;
    private UserRepository userRepository;
    private SessionService sessionService;

    // In-memory test store acting as backing database for fast deterministic unit tests
    private final ConcurrentHashMap<String, UserSessionEntity> sessionStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UserEntity> userStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        sessionStore.clear();
        userStore.clear();

        properties = new AuthConfigProperties();
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
        properties.setCookieName("SCANPILOT_SESSION");
        properties.setCookieSecure(true);
        properties.setSessionTtlSeconds(3600); // 1 hour
        properties.setStateTtlSeconds(600);

        userSessionRepository = mock(UserSessionRepository.class);
        userRepository = mock(UserRepository.class);

        // Mock UserRepository behaviors
        when(userRepository.findByGithubUserId(anyLong())).thenAnswer(invocation -> {
            Long githubId = invocation.getArgument(0);
            return userStore.values().stream()
                    .filter(u -> githubId.equals(u.getGithubUserId()))
                    .findFirst();
        });
        when(userRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.ofNullable(userStore.get(id));
        });
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            userStore.put(entity.getId(), entity);
            return entity;
        });

        // Mock UserSessionRepository behaviors
        when(userSessionRepository.findBySessionId(anyString())).thenAnswer(invocation -> {
            String sid = invocation.getArgument(0);
            return Optional.ofNullable(sessionStore.get(sid));
        });
        when(userSessionRepository.save(any(UserSessionEntity.class))).thenAnswer(invocation -> {
            UserSessionEntity entity = invocation.getArgument(0);
            sessionStore.put(entity.getSessionId(), entity);
            return entity;
        });
        doAnswer(invocation -> {
            String sid = invocation.getArgument(0);
            sessionStore.remove(sid);
            return null;
        }).when(userSessionRepository).deleteBySessionId(anyString());
        doAnswer(invocation -> {
            Instant threshold = invocation.getArgument(0);
            sessionStore.entrySet().removeIf(e -> e.getValue().getExpiresAt() != null && threshold.isAfter(e.getValue().getExpiresAt()));
            return null;
        }).when(userSessionRepository).deleteByExpiresAtBefore(any(Instant.class));
        doAnswer(invocation -> {
            sessionStore.clear();
            return null;
        }).when(userSessionRepository).deleteAll();
        when(userSessionRepository.count()).thenAnswer(invocation -> (long) sessionStore.size());

        sessionService = new SessionService(properties, userSessionRepository, userRepository);
    }

    @Test
    @DisplayName("createSession persists UserSessionEntity to repository and returns valid UserSession")
    void testCreateSessionFromDto() {
        GitHubUserDto userDto = new GitHubUserDto(12345L, "octocat", "The Octocat", "https://avatar.url", "octo@github.com");
        String accessToken = "gho_secret_access_token";

        UserSession session = sessionService.createSession(userDto, accessToken);

        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).isNotBlank();
        assertThat(session.getGithubUserId()).isEqualTo(12345L);
        assertThat(session.getLogin()).isEqualTo("octocat");
        assertThat(session.getName()).isEqualTo("The Octocat");
        assertThat(session.getAvatarUrl()).isEqualTo("https://avatar.url");
        assertThat(session.getEmail()).isEqualTo("octo@github.com");
        assertThat(session.getAccessToken()).isEqualTo(accessToken);
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getExpiresAt()).isAfter(session.getCreatedAt());
        assertThat(session.isExpired()).isFalse();

        // Verify session was persisted in backing repository
        assertThat(sessionStore).containsKey(session.getSessionId());

        Optional<UserSession> retrieved = sessionService.getSession(session.getSessionId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLogin()).isEqualTo("octocat");
    }

    @Test
    @DisplayName("Cross-instance session retrieval survives new SessionService instance sharing the same repository")
    void testCrossInstanceSessionRetrieval() {
        GitHubUserDto userDto = new GitHubUserDto(99999L, "cross-instance-user", "CI User", "https://avatar.url", "ci@example.com");
        UserSession session = sessionService.createSession(userDto, "gho_token_abc");

        // Instantiate a second independent SessionService instance sharing the same repository
        SessionService secondServiceInstance = new SessionService(properties, userSessionRepository, userRepository);

        Optional<UserSession> retrieved = secondServiceInstance.getSession(session.getSessionId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getGithubUserId()).isEqualTo(99999L);
        assertThat(retrieved.get().getLogin()).isEqualTo("cross-instance-user");
    }

    @Test
    @DisplayName("getSession returns empty when session ID does not exist")
    void testGetSessionNonExistent() {
        Optional<UserSession> session = sessionService.getSession("non-existent-id");
        assertThat(session).isEmpty();
    }

    @Test
    @DisplayName("getSession returns empty and cleans up from repository when session has expired")
    void testGetSessionExpired() {
        properties.setSessionTtlSeconds(-10); // Expired immediately
        SessionService expiredSessionService = new SessionService(properties, userSessionRepository, userRepository);

        UserSession session = expiredSessionService.createSession(1L, "user1", "User One", "url", "email", "token");
        assertThat(session.isExpired()).isTrue();

        Optional<UserSession> retrieved = expiredSessionService.getSession(session.getSessionId());
        assertThat(retrieved).isEmpty();
        assertThat(sessionStore).doesNotContainKey(session.getSessionId());
    }

    @Test
    @DisplayName("invalidateSession removes session from backing database")
    void testInvalidateSession() {
        UserSession session = sessionService.createSession(1L, "user1", "User One", "url", "email", "token");
        assertThat(sessionService.getSession(session.getSessionId())).isPresent();

        sessionService.invalidateSession(session.getSessionId());
        assertThat(sessionService.getSession(session.getSessionId())).isEmpty();
        assertThat(sessionStore).doesNotContainKey(session.getSessionId());
    }

    @Test
    @DisplayName("updateInstallationId persists updated installationId in repository")
    void testUpdateInstallationId() {
        UserSession session = sessionService.createSession(1L, "user1", "User One", "url", "email", "token");
        assertThat(session.getInstallationId()).isNull();

        Optional<UserSession> updated = sessionService.updateInstallationId(session.getSessionId(), 54321L);
        assertThat(updated).isPresent();
        assertThat(updated.get().getInstallationId()).isEqualTo(54321L);

        Optional<UserSession> fetched = sessionService.getSession(session.getSessionId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getInstallationId()).isEqualTo(54321L);
    }

    @Test
    @DisplayName("createSessionCookie creates HttpOnly, Secure cookie with correct maxAge")
    void testCreateSessionCookie() {
        ResponseCookie cookie = sessionService.createSessionCookie("sess_123456");

        assertThat(cookie.getName()).isEqualTo("SCANPILOT_SESSION");
        assertThat(cookie.getValue()).isEqualTo("sess_123456");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo(properties.isCookieSecure() ? "None" : "Lax");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("createLogoutCookie creates HttpOnly, Max-Age=0 cookie to clear session")
    void testCreateLogoutCookie() {
        ResponseCookie cookie = sessionService.createLogoutCookie();

        assertThat(cookie.getName()).isEqualTo("SCANPILOT_SESSION");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(0);
    }

    @Test
    @DisplayName("cleanExpiredSessions removes only expired sessions from repository")
    void testCleanExpiredSessions() {
        sessionService.createSession(1L, "active", "Active User", null, null, "tok1");

        // Manually create an expired session in repository
        UserEntity expiredUser = userRepository.save(UserEntity.builder()
                .githubUserId(2L)
                .login("expired")
                .createdAt(Instant.now().minusSeconds(100))
                .build());

        UserSessionEntity expiredEntity = UserSessionEntity.builder()
                .sessionId("expired-session-id")
                .userId(expiredUser.getId())
                .createdAt(Instant.now().minusSeconds(100))
                .expiresAt(Instant.now().minusSeconds(10))
                .build();
        sessionStore.put("expired-session-id", expiredEntity);

        sessionService.cleanExpiredSessions();
        assertThat(sessionStore).doesNotContainKey("expired-session-id");
        assertThat(sessionStore).hasSize(1);
    }

    @Test
    @DisplayName("UserSession toString excludes sensitive accessToken")
    void testUserSessionToStringExcludesAccessToken() {
        String sensitiveToken = "gho_super_secret_token_12345";
        UserSession session = UserSession.builder()
                .sessionId("sess-abc")
                .githubUserId(42L)
                .login("alice")
                .name("Alice Smith")
                .email("alice@example.com")
                .accessToken(sensitiveToken)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        String toStringResult = session.toString();
        assertThat(toStringResult).contains("alice");
        assertThat(toStringResult).contains("42");
        assertThat(toStringResult).doesNotContain(sensitiveToken);
        assertThat(toStringResult).doesNotContain("accessToken");
    }

    @Test
    @DisplayName("UserSession supports Lombok builder, setters, and no-args constructor")
    void testUserSessionLombokBuilderAndSetters() {
        UserSession session = new UserSession();
        session.setSessionId("sess-xyz");
        session.setLogin("bob");
        session.setGithubUserId(99L);
        session.setAccessToken("gho_tok");

        assertThat(session.getSessionId()).isEqualTo("sess-xyz");
        assertThat(session.getLogin()).isEqualTo("bob");
        assertThat(session.getGithubUserId()).isEqualTo(99L);
        assertThat(session.getAccessToken()).isEqualTo("gho_tok");
    }
}
