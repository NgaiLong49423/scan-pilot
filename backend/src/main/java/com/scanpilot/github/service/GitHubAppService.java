package com.scanpilot.github.service;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.github.config.GitHubAppConfigProperties;
import com.scanpilot.github.dto.GitHubRepositoryDto;
import com.scanpilot.project.model.MonitoredProject;
import com.scanpilot.project.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GitHubAppService {

    public static final String GITHUB_INSTALLATION_REPOS_URL = "https://api.github.com/installation/repositories?per_page=100";
    public static final String GITHUB_USER_REPOS_URL = "https://api.github.com/user/repos?per_page=100&sort=updated";

    private final GitHubAppConfigProperties properties;
    private final GitHubAppAuthService gitHubAppAuthService;
    private final SessionService sessionService;
    private final ProjectService projectService;
    private final RestClient restClient;

    // In-memory mapping of githubUserId -> installationId
    private final Map<Long, Long> userInstallations = new ConcurrentHashMap<>();

    public GitHubAppService(
            GitHubAppConfigProperties properties,
            GitHubAppAuthService gitHubAppAuthService,
            SessionService sessionService,
            ProjectService projectService,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.gitHubAppAuthService = gitHubAppAuthService;
        this.sessionService = sessionService;
        this.projectService = projectService;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Constructs the GitHub App installation URL.
     */
    public String getInstallUrl() {
        String slug = properties.getAppSlug();
        if (slug == null || slug.isBlank()) {
            slug = "scan-pilot";
        }
        return "https://github.com/apps/" + slug.trim() + "/installations/new";
    }

    /**
     * Links a GitHub App installation ID to the user's session and identity.
     */
    public void linkInstallation(UserSession session, Long installationId) {
        if (session == null) {
            throw new IllegalArgumentException("User session is required");
        }
        if (installationId == null) {
            throw new IllegalArgumentException("Installation ID is required");
        }

        userInstallations.put(session.getGithubUserId(), installationId);
        sessionService.updateInstallationId(session.getSessionId(), installationId);
        log.info("Linked GitHub App installation {} to user {}", installationId, session.getLogin());
    }

    public Long getInstallationId(Long githubUserId) {
        if (githubUserId == null) {
            return null;
        }
        return userInstallations.get(githubUserId);
    }

    /**
     * Fetches user accessible repositories from GitHub.
     * Tries GitHub App installation token first if available, otherwise falls back to user OAuth token.
     */
    public List<GitHubRepositoryDto> getAccessibleRepositories(UserSession session) {
        if (session == null) {
            throw new IllegalArgumentException("User session is required");
        }

        Long installationId = session.getInstallationId() != null
                ? session.getInstallationId()
                : userInstallations.get(session.getGithubUserId());

        Optional<MonitoredProject> currentProject = projectService.getCurrentProject(session);
        Long currentSelectedRepoId = currentProject.map(MonitoredProject::getGithubRepoId).orElse(null);

        // If installation is linked and GitHub App is configured, fetch installation repositories
        if (installationId != null && gitHubAppAuthService.isConfigured()) {
            try {
                String token = gitHubAppAuthService.createInstallationAccessToken(installationId);
                return fetchInstallationRepositories(token, currentSelectedRepoId);
            } catch (Exception e) {
                log.warn("Failed to fetch installation repositories, falling back to user repos: {}", e.getMessage());
            }
        }

        // Fallback to user OAuth access token
        return fetchUserRepositories(session.getAccessToken(), currentSelectedRepoId);
    }

    @SuppressWarnings("unchecked")
    private List<GitHubRepositoryDto> fetchInstallationRepositories(String installationToken, Long currentSelectedRepoId) {
        Map<String, Object> response = restClient.get()
                .uri(GITHUB_INSTALLATION_REPOS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (response == null || !response.containsKey("repositories")) {
            return List.of();
        }

        List<Map<String, Object>> reposList = (List<Map<String, Object>>) response.get("repositories");
        return mapRawReposToDto(reposList, currentSelectedRepoId);
    }

    private List<GitHubRepositoryDto> fetchUserRepositories(String userAccessToken, Long currentSelectedRepoId) {
        if (userAccessToken == null || userAccessToken.isBlank()) {
            return List.of();
        }

        if ("mock-dev-token".equals(userAccessToken)) {
            return getDevMockRepositories(currentSelectedRepoId);
        }

        try {
            List<Map<String, Object>> reposList = restClient.get()
                    .uri(GITHUB_USER_REPOS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (reposList == null) {
                return List.of();
            }

            return mapRawReposToDto(reposList, currentSelectedRepoId);
        } catch (Exception e) {
            log.warn("Failed to fetch user repos from GitHub ({}), returning dev fallback repos", e.getMessage());
            return getDevMockRepositories(currentSelectedRepoId);
        }
    }

    private List<GitHubRepositoryDto> getDevMockRepositories(Long currentSelectedRepoId) {
        return List.of(
                new GitHubRepositoryDto(
                        101L,
                        "scan-pilot",
                        "NgaiLong49423/scan-pilot",
                        "NgaiLong49423",
                        "main",
                        false,
                        "https://github.com/NgaiLong49423/scan-pilot",
                        "Continuous multi-project health and security monitoring platform",
                        Long.valueOf(101L).equals(currentSelectedRepoId)
                ),
                new GitHubRepositoryDto(
                        102L,
                        "ai-ecommerce-service",
                        "developer/ai-ecommerce-service",
                        "developer",
                        "main",
                        true,
                        "https://github.com/developer/ai-ecommerce-service",
                        "Spring Boot 3 + Gemini AI assistant e-commerce microservice",
                        Long.valueOf(102L).equals(currentSelectedRepoId)
                ),
                new GitHubRepositoryDto(
                        103L,
                        "security-lab-synthetic",
                        "developer/security-lab-synthetic",
                        "developer",
                        "main",
                        false,
                        "https://github.com/developer/security-lab-synthetic",
                        "Synthetic security benchmark test repository",
                        Long.valueOf(103L).equals(currentSelectedRepoId)
                )
        );
    }

    @SuppressWarnings("unchecked")
    private List<GitHubRepositoryDto> mapRawReposToDto(List<Map<String, Object>> reposList, Long currentSelectedRepoId) {
        List<GitHubRepositoryDto> result = new ArrayList<>();

        for (Map<String, Object> repo : reposList) {
            Long id = ((Number) repo.get("id")).longValue();
            String name = (String) repo.get("name");
            String fullName = (String) repo.get("full_name");
            String defaultBranch = repo.get("default_branch") != null ? (String) repo.get("default_branch") : "main";
            boolean isPrivate = Boolean.TRUE.equals(repo.get("private"));
            String htmlUrl = (String) repo.get("html_url");
            String description = (String) repo.get("description");

            String ownerLogin = "";
            Object ownerObj = repo.get("owner");
            if (ownerObj instanceof Map) {
                ownerLogin = (String) ((Map<String, Object>) ownerObj).get("login");
            } else if (ownerObj != null) {
                ownerLogin = ownerObj.toString();
            }

            boolean isSelected = currentSelectedRepoId != null && currentSelectedRepoId.equals(id);

            result.add(new GitHubRepositoryDto(
                    id,
                    name,
                    fullName,
                    ownerLogin,
                    defaultBranch,
                    isPrivate,
                    htmlUrl,
                    description,
                    isSelected
            ));
        }

        return result;
    }

    public void clearAllInstallations() {
        userInstallations.clear();
    }
}
