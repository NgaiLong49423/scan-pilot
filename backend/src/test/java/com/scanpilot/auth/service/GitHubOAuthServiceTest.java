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
    @DisplayName("generateAuthorizationUrl generates valid GitHub App user-to-server authorize URL without scopes by default")
    void testGenerateAuthorizationUrlDefaultAppFlow() {
        String authUrl = gitHubOAuthService.generateAuthorizationUrl();

        assertThat(authUrl).startsWith("https://github.com/login/oauth/authorize");
        assertThat(authUrl).contains("client_id=test-client-id");
        assertThat(authUrl).contains("redirect_uri=" + "http://localhost:8080/api/v1/auth/github/callback");
        assertThat(authUrl).contains("state=");
        assertThat(authUrl).doesNotContain("scope=");

        // Extract state parameter
        URI uri = URI.create(authUrl);
        String query = uri.getQuery();
        String state = null;
        for (String param : query.split("&")) {
            if (param.startsWith("state=")) {
                state = URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8);
                break;
            }
        }
        assertThat(state).isNotNull().isNotBlank();
        assertThat(gitHubOAuthService.validateAndConsumeState(state)).isTrue();
    }

    @Test
    @DisplayName("generateAuthorizationUrl appends scope parameter when custom scopes are configured")
    void testGenerateAuthorizationUrlWithCustomScopes() {
        properties.setScopes("read:user,user:email");
        String authUrl = gitHubOAuthService.generateAuthorizationUrl();

        assertThat(authUrl).contains("scope=read:user,user:email");
    }

    @Test
    @DisplayName("validateAndConsumeState is single-use and fails on replayed state")
    void testValidateAndConsumeStateSingleUse() {
        String authUrl = gitHubOAuthService.generateAuthorizationUrl();
        URI uri = URI.create(authUrl);
        String query = uri.getQuery();
        String state = null;
        for (String param : query.split("&")) {
            if (param.startsWith("state=")) {
                state = URLDecoder.decode(param.substring(6), StandardCharsets.UTF_8);
                break;
            }
        }

        assertThat(gitHubOAuthService.validateAndConsumeState(state)).isTrue();
        assertThat(gitHubOAuthService.validateAndConsumeState(state)).isFalse();
    }

    @Test
    @DisplayName("exchangeCodeForAccessToken exchanges code for access token via GitHub API")
    void testExchangeCodeForAccessToken() {
        String code = "valid-auth-code";
        String expectedResponseJson = "{\"access_token\":\"ghu_16C7e42F292c6912E7710c838347Ae178B4a\",\"token_type\":\"bearer\",\"scope\":\"\"}";

        mockServer.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(expectedResponseJson, MediaType.APPLICATION_JSON));

        String token = gitHubOAuthService.exchangeCodeForAccessToken(code);
        assertThat(token).isEqualTo("ghu_16C7e42F292c6912E7710c838347Ae178B4a");
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchUserProfile fetches GitHub user profile with access token")
    void testFetchUserProfile() {
        String token = "ghu_16C7e42F292c6912E7710c838347Ae178B4a";
        String expectedUserJson = "{\"id\":583231,\"login\":\"octocat\",\"name\":\"The Octocat\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/583231?v=4\",\"email\":\"octocat@github.com\"}";

        mockServer.expect(requestTo("https://api.github.com/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(expectedUserJson, MediaType.APPLICATION_JSON));

        GitHubUserDto user = gitHubOAuthService.fetchUserProfile(token);
        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(583231L);
        assertThat(user.login()).isEqualTo("octocat");
        assertThat(user.email()).isEqualTo("octocat@github.com");
        mockServer.verify();
    }
}
