package com.scanpilot.github.service;

import com.scanpilot.auth.config.AuthConfigProperties;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.github.config.GitHubAppConfigProperties;
import com.scanpilot.github.dto.GitHubRepositoryDto;
import com.scanpilot.project.dto.SelectRepositoryRequest;
import com.scanpilot.project.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubAppServiceTest {

    private GitHubAppConfigProperties properties;
    private GitHubAppAuthService gitHubAppAuthService;
    private SessionService sessionService;
    private ProjectService projectService;
    private GitHubAppService gitHubAppService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        properties = new GitHubAppConfigProperties();
        properties.setAppSlug("scan-pilot-app");

        gitHubAppAuthService = mock(GitHubAppAuthService.class);
        sessionService = new SessionService(new AuthConfigProperties());
        projectService = new ProjectService();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        gitHubAppService = new GitHubAppService(
                properties,
                gitHubAppAuthService,
                sessionService,
                projectService,
                builder
        );
    }

    @Test
    @DisplayName("getInstallUrl returns correctly formatted URL using configured slug")
    void testGetInstallUrl() {
        assertThat(gitHubAppService.getInstallUrl()).isEqualTo("https://github.com/apps/scan-pilot-app/installations/new");

        properties.setAppSlug("custom-scanner-bot");
        assertThat(gitHubAppService.getInstallUrl()).isEqualTo("https://github.com/apps/custom-scanner-bot/installations/new");
    }

    @Test
    @DisplayName("linkInstallation updates user mapping and active session")
    void testLinkInstallation() {
        UserSession session = sessionService.createSession(
                123L, "octocat", "Octo Cat", "https://avatar", "octo@github.com", "gho_token"
        );

        gitHubAppService.linkInstallation(session, 99999L);

        assertThat(gitHubAppService.getInstallationId(123L)).isEqualTo(99999L);

        UserSession updatedSession = sessionService.getSession(session.getSessionId()).orElseThrow();
        assertThat(updatedSession.getInstallationId()).isEqualTo(99999L);
    }

    @Test
    @DisplayName("getAccessibleRepositories uses installation token when installation is linked and app configured")
    void testGetAccessibleRepositoriesWithInstallation() {
        UserSession session = new UserSession(
                "session-1", 123L, "octocat", "Octo Cat", "https://avatar", "octo@github.com", "gho_token",
                8888L, Instant.now(), Instant.now().plusSeconds(3600)
        );

        when(gitHubAppAuthService.isConfigured()).thenReturn(true);
        when(gitHubAppAuthService.createInstallationAccessToken(8888L)).thenReturn("ghs_ephemeral_token");

        String jsonResponse = """
                {
                    "total_count": 2,
                    "repositories": [
                        {
                            "id": 101,
                            "name": "repo-alpha",
                            "full_name": "octocat/repo-alpha",
                            "owner": { "login": "octocat" },
                            "default_branch": "main",
                            "private": false,
                            "html_url": "https://github.com/octocat/repo-alpha",
                            "description": "Alpha repo"
                        },
                        {
                            "id": 102,
                            "name": "repo-beta",
                            "full_name": "octocat/repo-beta",
                            "owner": { "login": "octocat" },
                            "default_branch": "dev",
                            "private": true,
                            "html_url": "https://github.com/octocat/repo-beta",
                            "description": "Beta repo"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/installation/repositories?per_page=100"))
                .andExpect(header("Authorization", "Bearer ghs_ephemeral_token"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // Select repo-beta in projectService to verify isSelected flag
        projectService.selectRepository(session, new SelectRepositoryRequest(102L, "octocat/repo-beta", "repo-beta", "octocat", "dev", true));

        List<GitHubRepositoryDto> repos = gitHubAppService.getAccessibleRepositories(session);
        assertThat(repos).hasSize(2);

        GitHubRepositoryDto alpha = repos.stream().filter(r -> r.id() == 101L).findFirst().orElseThrow();
        assertThat(alpha.name()).isEqualTo("repo-alpha");
        assertThat(alpha.fullName()).isEqualTo("octocat/repo-alpha");
        assertThat(alpha.owner()).isEqualTo("octocat");
        assertThat(alpha.defaultBranch()).isEqualTo("main");
        assertThat(alpha.isPrivate()).isFalse();
        assertThat(alpha.isSelected()).isFalse();

        GitHubRepositoryDto beta = repos.stream().filter(r -> r.id() == 102L).findFirst().orElseThrow();
        assertThat(beta.name()).isEqualTo("repo-beta");
        assertThat(beta.isPrivate()).isTrue();
        assertThat(beta.isSelected()).isTrue();

        mockServer.verify();
    }

    @Test
    @DisplayName("getAccessibleRepositories falls back to user OAuth token when no installation linked")
    void testGetAccessibleRepositoriesFallbackOAuth() {
        UserSession session = new UserSession(
                "session-1", 123L, "octocat", "Octo Cat", "https://avatar", "octo@github.com", "gho_user_token",
                null, Instant.now(), Instant.now().plusSeconds(3600)
        );

        when(gitHubAppAuthService.isConfigured()).thenReturn(false);

        String jsonResponse = """
                [
                    {
                        "id": 201,
                        "name": "personal-repo",
                        "full_name": "octocat/personal-repo",
                        "owner": { "login": "octocat" },
                        "default_branch": "master",
                        "private": false,
                        "html_url": "https://github.com/octocat/personal-repo",
                        "description": "Personal project"
                    }
                ]
                """;

        mockServer.expect(requestTo("https://api.github.com/user/repos?per_page=100&sort=updated"))
                .andExpect(header("Authorization", "Bearer gho_user_token"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<GitHubRepositoryDto> repos = gitHubAppService.getAccessibleRepositories(session);
        assertThat(repos).hasSize(1);
        assertThat(repos.get(0).id()).isEqualTo(201L);
        assertThat(repos.get(0).name()).isEqualTo("personal-repo");
        assertThat(repos.get(0).isSelected()).isFalse();

        mockServer.verify();
    }
}
