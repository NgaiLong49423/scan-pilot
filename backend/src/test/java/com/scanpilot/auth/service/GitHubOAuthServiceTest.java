package com.scanpilot.auth.service;

import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.dto.GitHubUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubOAuthServiceTest {

    private AuthConfigProperties properties;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private GitHubOAuthService gitHubOAuthService;

    @BeforeEach
    void setUp() {
        properties = new AuthConfigProperties();
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
        properties.setRedirectUri("http://localhost:8080/api/v1/auth/github/callback");
        properties.setStateTtlSeconds(600);

        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        gitHubOAuthService = new GitHubOAuthService(properties, restClientBuilder);
    }

    @Test
    @DisplayName("generateAuthorizationUrl generates valid GitHub OAuth authorize URL with state")
    void testGenerateAuthorizationUrl() {
        String authUrl = gitHubOAuthService.generateAuthorizationUrl();

        assertThat(authUrl).startsWith("https://github.com/login/oauth/authorize");
        assertThat(authUrl).contains("client_id=test-client-id");
        assertThat(authUrl).contains("redirect_uri=" + "http://localhost:8080/api/v1/auth/github/callback");
        assertThat(authUrl).contains("scope=" + "read:user,user:email");
        assertThat(authUrl).contains("state=");

        // Extract state parameter
        URI uri = URI.create(authUrl);
        String query = uri.getQuery();
        String state = null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if ("state".equals(pair[0])) {
                state = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }

        assertThat(state).isNotBlank();
        // Validating state once succeeds
        assertThat(gitHubOAuthService.validateAndConsumeState(state)).isTrue();
        // Validating same state a second time fails (consumed/one-time use)
        assertThat(gitHubOAuthService.validateAndConsumeState(state)).isFalse();
    }

    @Test
    @DisplayName("validateAndConsumeState returns false for unknown or invalid state")
    void testValidateAndConsumeStateInvalid() {
        assertThat(gitHubOAuthService.validateAndConsumeState("unknown-state")).isFalse();
        assertThat(gitHubOAuthService.validateAndConsumeState(null)).isFalse();
        assertThat(gitHubOAuthService.validateAndConsumeState("")).isFalse();
    }

    @Test
    @DisplayName("exchangeCodeForAccessToken successfully exchanges code for access token")
    void testExchangeCodeForAccessTokenSuccess() {
        String mockResponseBody = """
                {
                    "access_token": "gho_test_token_12345",
                    "token_type": "bearer",
                    "scope": "read:user,user:email"
                }
                """;

        mockServer.expect(requestTo(GitHubOAuthService.GITHUB_TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        String accessToken = gitHubOAuthService.exchangeCodeForAccessToken("auth-code-123");

        assertThat(accessToken).isEqualTo("gho_test_token_12345");
        mockServer.verify();
    }

    @Test
    @DisplayName("exchangeCodeForAccessToken throws exception when GitHub returns error")
    void testExchangeCodeForAccessTokenError() {
        String mockResponseBody = """
                {
                    "error": "bad_verification_code",
                    "error_description": "The code passed is incorrect or has expired."
                }
                """;

        mockServer.expect(requestTo(GitHubOAuthService.GITHUB_TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gitHubOAuthService.exchangeCodeForAccessToken("invalid-code"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("The code passed is incorrect or has expired.");

        mockServer.verify();
    }

    @Test
    @DisplayName("fetchUserProfile successfully retrieves user profile with Bearer token")
    void testFetchUserProfileSuccess() {
        String mockResponseBody = """
                {
                    "id": 987654,
                    "login": "octocat-dev",
                    "name": "Octocat Developer",
                    "avatar_url": "https://avatars.githubusercontent.com/u/987654",
                    "email": "octocat@github.com"
                }
                """;

        mockServer.expect(requestTo(GitHubOAuthService.GITHUB_USER_API_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer gho_test_token_12345"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        GitHubUserDto user = gitHubOAuthService.fetchUserProfile("gho_test_token_12345");

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(987654L);
        assertThat(user.login()).isEqualTo("octocat-dev");
        assertThat(user.name()).isEqualTo("Octocat Developer");
        assertThat(user.avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/987654");
        assertThat(user.email()).isEqualTo("octocat@github.com");

        mockServer.verify();
    }
}
