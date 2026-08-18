package com.scanpilot.auth.service;

import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.dto.GitHubUserDto;
import com.scanpilot.auth.model.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SessionServiceTest {

    private AuthConfigProperties properties;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        properties = new AuthConfigProperties();
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
        properties.setCookieName("SCANPILOT_SESSION");
        properties.setCookieSecure(true);
        properties.setSessionTtlSeconds(3600); // 1 hour
        properties.setStateTtlSeconds(600);

        sessionService = new SessionService(properties);
    }

    @Test
    @DisplayName("createSession creates active session and returns valid UserSession")
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

        Optional<UserSession> retrieved = sessionService.getSession(session.getSessionId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLogin()).isEqualTo("octocat");
    }

    @Test
    @DisplayName("getSession returns empty when session ID does not exist")
    void testGetSessionNonExistent() {
        Optional<UserSession> session = sessionService.getSession("non-existent-id");
        assertThat(session).isEmpty();
    }

    @Test
    @DisplayName("getSession returns empty and cleans up when session has expired")
    void testGetSessionExpired() {
        properties.setSessionTtlSeconds(-10); // Expired immediately
        SessionService expiredSessionService = new SessionService(properties);

        UserSession session = expiredSessionService.createSession(1L, "user1", "User One", "url", "email", "token");
        assertThat(session.isExpired()).isTrue();

        Optional<UserSession> retrieved = expiredSessionService.getSession(session.getSessionId());
        assertThat(retrieved).isEmpty();
        assertThat(expiredSessionService.getActiveSessionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("invalidateSession removes session from store")
    void testInvalidateSession() {
        UserSession session = sessionService.createSession(1L, "user1", "User One", "url", "email", "token");
        assertThat(sessionService.getSession(session.getSessionId())).isPresent();

        sessionService.invalidateSession(session.getSessionId());
        assertThat(sessionService.getSession(session.getSessionId())).isEmpty();
    }

    @Test
    @DisplayName("createSessionCookie creates HttpOnly, Secure, Lax cookie with correct maxAge")
    void testCreateSessionCookie() {
        ResponseCookie cookie = sessionService.createSessionCookie("sess_123456");

        assertThat(cookie.getName()).isEqualTo("SCANPILOT_SESSION");
        assertThat(cookie.getValue()).isEqualTo("sess_123456");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
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
    @DisplayName("cleanExpiredSessions removes only expired sessions")
    void testCleanExpiredSessions() {
        sessionService.createSession(1L, "active", "Active User", null, null, "tok1");

        // Manually create an expired session
        properties.setSessionTtlSeconds(-5);
        SessionService shortLivedService = new SessionService(properties);
        UserSession expired = shortLivedService.createSession(2L, "expired", "Expired User", null, null, "tok2");

        assertThat(shortLivedService.getActiveSessionCount()).isEqualTo(0);
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
