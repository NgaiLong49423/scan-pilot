package com.scanpilot.auth.controller;

import com.scanpilot.auth.dto.GitHubUserDto;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.GitHubOAuthService;
import com.scanpilot.auth.service.SessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "scanpilot.auth.client-id=test-client-id",
        "scanpilot.auth.frontend-url=http://localhost:3000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @MockitoBean
    private GitHubOAuthService gitHubOAuthService;

    @BeforeEach
    void setUp() {
        sessionService.clearAllSessions();
    }

    @Test
    @DisplayName("GET /api/v1/auth/github/login redirects to GitHub OAuth authorize URL when client_id is set")
    void testLoginWithGitHubRedirects() throws Exception {
        String mockAuthorizeUrl = "https://github.com/login/oauth/authorize?client_id=test-client-id&redirect_uri=http://localhost:8080/callback&state=xyz123";
        when(gitHubOAuthService.generateAuthorizationUrl()).thenReturn(mockAuthorizeUrl);

        mockMvc.perform(get("/api/v1/auth/github/login"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, mockAuthorizeUrl));
    }

    @Test
    @DisplayName("GET /api/v1/auth/dev-login creates session and sets cookie for local dev")
    void testDevLogin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/dev-login"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SCANPILOT_SESSION=")));

        assertThat(sessionService.getActiveSessionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/v1/auth/github/callback with valid state and code creates session and sets cookie")
    void testCallbackSuccess() throws Exception {
        String state = "valid_state_123";
        String code = "valid_code_456";
        String accessToken = "gho_valid_token_789";
        GitHubUserDto userDto = new GitHubUserDto(999L, "johndoe", "John Doe", "https://avatar.url/john", "john@example.com");

        when(gitHubOAuthService.validateAndConsumeState(state)).thenReturn(true);
        when(gitHubOAuthService.exchangeCodeForAccessToken(code)).thenReturn(accessToken);
        when(gitHubOAuthService.fetchUserProfile(accessToken)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .param("code", code)
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SCANPILOT_SESSION=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));

        assertThat(sessionService.getActiveSessionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/v1/auth/github/callback with invalid state redirects to frontend with error")
    void testCallbackInvalidState() throws Exception {
        when(gitHubOAuthService.validateAndConsumeState("invalid_state")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .param("code", "some-code")
                        .param("state", "invalid_state"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:3000?auth_error=invalid_state"));

        assertThat(sessionService.getActiveSessionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/v1/auth/github/callback with OAuth error redirects to frontend with error")
    void testCallbackOAuthError() throws Exception {
        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .param("error", "access_denied")
                        .param("error_description", "The user has denied your application access."))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:3000?auth_error=access_denied"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/github/callback with missing code redirects to frontend with missing_code error")
    void testCallbackMissingCode() throws Exception {
        when(gitHubOAuthService.validateAndConsumeState("valid_state")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .param("state", "valid_state"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:3000?auth_error=missing_code"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/github/callback when token exchange fails redirects with oauth_failed")
    void testCallbackTokenExchangeFailure() throws Exception {
        when(gitHubOAuthService.validateAndConsumeState("valid_state")).thenReturn(true);
        when(gitHubOAuthService.exchangeCodeForAccessToken("code")).thenThrow(new IllegalStateException("Exchange error"));

        mockMvc.perform(get("/api/v1/auth/github/callback")
                        .param("code", "code")
                        .param("state", "valid_state"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:3000?auth_error=oauth_failed"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with valid session cookie returns user profile without accessToken")
    void testGetMeAuthenticated() throws Exception {
        UserSession session = sessionService.createSession(
                12345L,
                "octocat",
                "The Octocat",
                "https://avatars.githubusercontent.com/u/12345",
                "octocat@github.com",
                "gho_secret_access_token_never_expose"
        );

        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(new Cookie("SCANPILOT_SESSION", session.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubUserId").value(12345))
                .andExpect(jsonPath("$.login").value("octocat"))
                .andExpect(jsonPath("$.name").value("The Octocat"))
                .andExpect(jsonPath("$.avatarUrl").value("https://avatars.githubusercontent.com/u/12345"))
                .andExpect(jsonPath("$.email").value("octocat@github.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without session cookie returns 401 Unauthorized")
    void testGetMeUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with invalid session cookie returns 401 Unauthorized")
    void testGetMeInvalidSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(new Cookie("SCANPILOT_SESSION", "invalid-session-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout invalidates session and clears cookie")
    void testLogout() throws Exception {
        UserSession session = sessionService.createSession(
                12345L,
                "octocat",
                "The Octocat",
                "https://avatars.githubusercontent.com/u/12345",
                "octocat@github.com",
                "gho_secret_token"
        );

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("SCANPILOT_SESSION", session.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SCANPILOT_SESSION=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertThat(sessionService.getSession(session.getSessionId())).isEmpty();
    }
}
