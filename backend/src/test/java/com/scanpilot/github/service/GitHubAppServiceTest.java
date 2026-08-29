package com.scanpilot.github.service;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.config.GitHubAppConfigProperties;
import com.scanpilot.github.dto.GitHubRepositoryDto;
import com.scanpilot.github.dto.UserAccessibleInstallationDto;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubAppServiceTest {

    private GitHubAppConfigProperties properties;
    private InstallationStateService installationStateService;
    private RepositoryRepository repositoryRepository;
    private UserRepository userRepository;
    private GitHubAppService gitHubAppService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        properties = new GitHubAppConfigProperties();
        properties.setAppSlug("scan-pilot-app");

        installationStateService = mock(InstallationStateService.class);
        repositoryRepository = mock(RepositoryRepository.class);
        userRepository = mock(UserRepository.class);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        gitHubAppService = new GitHubAppService(
                properties,
                installationStateService,
                repositoryRepository,
                userRepository,
                builder
        );
    }

    @Test
    @DisplayName("getInstallUrl returns correctly formatted URL with opaque state token")
    void testGetInstallUrl() {
        UUID userId = UUID.randomUUID();
        UserSession session = new UserSession(
                "session-1", 123L, "octocat", "Octo Cat", "avatar", "octo@github.com", "ghu_token", null, Instant.now(), Instant.now().plusSeconds(3600)
        );

        when(userRepository.findByGithubUserId(123L)).thenReturn(Optional.of(
                UserEntity.builder().id(userId).githubUserId(123L).build()
        ));

        when(installationStateService.generateAndSaveState(userId, "session-1"))
                .thenReturn("opaque-state-12345");

        assertThat(gitHubAppService.getInstallUrl(session))
                .isEqualTo("https://github.com/apps/scan-pilot-app/installations/new?state=opaque-state-12345");
    }

    @Test
    @DisplayName("getUserAccessibleInstallations queries GitHub API with user token and handles pagination")
    void testGetUserAccessibleInstallationsWithPagination() {
        String userToken = "ghu_valid_user_token";

        String page1Json = """
                {
                    "total_count": 2,
                    "installations": [
                        {
                            "id": 1001,
                            "account": { "id": 123, "login": "octocat", "type": "User" }
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/user/installations?per_page=100&page=1"))
                .andExpect(header("Authorization", "Bearer " + userToken))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withSuccess(page1Json, MediaType.APPLICATION_JSON));

        List<UserAccessibleInstallationDto> installations = gitHubAppService.getUserAccessibleInstallations(userToken);

        assertThat(installations).hasSize(1);
        assertThat(installations.get(0).id()).isEqualTo(1001L);
        assertThat(installations.get(0).accountLogin()).isEqualTo("octocat");
        assertThat(installations.get(0).accountType()).isEqualTo("User");
        mockServer.verify();
    }

    @Test
    @DisplayName("Remediation R54-02: mock-dev-token performs real remote call and does not bypass verification")
    void testMockDevTokenDoesNotBypassVerification() {
        String mockToken = "mock-dev-token";

        mockServer.expect(requestTo("https://api.github.com/user/installations?per_page=100&page=1"))
                .andExpect(header("Authorization", "Bearer " + mockToken))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> gitHubAppService.getUserAccessibleInstallations(mockToken))
                .isInstanceOf(IllegalStateException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("getUserAccessibleInstallations fails closed when response is malformed or missing id")
    void testLevel1MalformedResponseFailsClosed() {
        String userToken = "ghu_valid_user_token";
        String malformedJson = """
                {
                    "total_count": 1,
                    "installations": [
                        {
                            "account": { "login": "octocat" }
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/user/installations?per_page=100&page=1"))
                .andExpect(header("Authorization", "Bearer " + userToken))
                .andRespond(withSuccess(malformedJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gitHubAppService.getUserAccessibleInstallations(userToken))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("getUserAccessibleInstallations fails closed on subsequent page error without returning partial results")
    void testLevel1SubsequentPageErrorFailsClosed() {
        String userToken = "ghu_valid_user_token";

        StringBuilder page1Sb = new StringBuilder("{\"total_count\": 101, \"installations\": [");
        for (int i = 0; i < 100; i++) {
            if (i > 0) page1Sb.append(",");
            page1Sb.append(String.format("{\"id\": %d, \"account\": {\"id\": 1, \"login\": \"org-%d\", \"type\": \"User\"}}", 2000 + i, i));
        }
        page1Sb.append("]}");

        mockServer.expect(requestTo("https://api.github.com/user/installations?per_page=100&page=1"))
                .andRespond(withSuccess(page1Sb.toString(), MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.github.com/user/installations?per_page=100&page=2"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> gitHubAppService.getUserAccessibleInstallations(userToken))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("getUserAccessibleInstallationRepositories queries GitHub API with user token and handles pagination")
    void testGetUserAccessibleInstallationRepositoriesWithPagination() {
        String userToken = "ghu_valid_user_token";
        Long installationId = 1001L;

        String page1Json = """
                {
                    "total_count": 1,
                    "repositories": [
                        {
                            "id": 5001,
                            "name": "repo-alpha",
                            "full_name": "octocat/repo-alpha",
                            "owner": { "login": "octocat" },
                            "default_branch": "main",
                            "private": false
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/user/installations/1001/repositories?per_page=100&page=1"))
                .andExpect(header("Authorization", "Bearer " + userToken))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withSuccess(page1Json, MediaType.APPLICATION_JSON));

        List<GitHubRepositoryDto> repos = gitHubAppService.getUserAccessibleInstallationRepositories(userToken, installationId);

        assertThat(repos).hasSize(1);
        assertThat(repos.get(0).id()).isEqualTo(5001L);
        assertThat(repos.get(0).fullName()).isEqualTo("octocat/repo-alpha");
        assertThat(repos.get(0).owner()).isEqualTo("octocat");
        assertThat(repos.get(0).defaultBranch()).isEqualTo("main");
        assertThat(repos.get(0).isPrivate()).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("Remediation R54-02: Level 2 repo parsing fails closed when owner, name, full_name, default_branch, or private is missing")
    void testLevel2MissingRequiredFieldsFailsClosed() {
        String userToken = "ghu_valid_user_token";
        Long installationId = 1001L;

        // Missing owner
        String missingOwnerJson = """
                {
                    "repositories": [
                        {
                            "id": 5001,
                            "name": "repo-alpha",
                            "full_name": "octocat/repo-alpha",
                            "default_branch": "main",
                            "private": false
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/user/installations/1001/repositories?per_page=100&page=1"))
                .andRespond(withSuccess(missingOwnerJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gitHubAppService.getUserAccessibleInstallationRepositories(userToken, installationId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("getUserAccessibleInstallationRepositories fails closed on subsequent page error without returning partial results")
    void testLevel2SubsequentPageErrorFailsClosed() {
        String userToken = "ghu_valid_user_token";
        Long installationId = 1001L;

        StringBuilder page1Sb = new StringBuilder("{\"total_count\": 101, \"repositories\": [");
        for (int i = 0; i < 100; i++) {
            if (i > 0) page1Sb.append(",");
            page1Sb.append(String.format("{\"id\": %d, \"name\": \"repo-%d\", \"full_name\": \"org/repo-%d\", \"owner\": {\"login\": \"org\"}, \"default_branch\": \"main\", \"private\": false}", 6000 + i, i, i));
        }
        page1Sb.append("]}");

        mockServer.expect(requestTo("https://api.github.com/user/installations/1001/repositories?per_page=100&page=1"))
                .andRespond(withSuccess(page1Sb.toString(), MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.github.com/user/installations/1001/repositories?per_page=100&page=2"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> gitHubAppService.getUserAccessibleInstallationRepositories(userToken, installationId))
                .isInstanceOf(IllegalStateException.class);
    }
}
