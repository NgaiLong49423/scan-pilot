package com.scanpilot.github.service;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.github.config.GitHubAppConfigProperties;
import com.scanpilot.github.dto.GitHubRepositoryDto;
import com.scanpilot.github.dto.UserAccessibleInstallationDto;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class GitHubAppService {

    public static final String GITHUB_USER_INSTALLATIONS_URL = "https://api.github.com/user/installations";
    public static final String GITHUB_USER_INSTALLATION_REPOS_URL = "https://api.github.com/user/installations/%d/repositories";
    public static final String GITHUB_USER_REPOS_URL = "https://api.github.com/user/repos";

    private final GitHubAppConfigProperties properties;
    private final InstallationStateService installationStateService;
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final RestClient restClient;

    public GitHubAppService(
            GitHubAppConfigProperties properties,
            InstallationStateService installationStateService,
            RepositoryRepository repositoryRepository,
            UserRepository userRepository,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.installationStateService = installationStateService;
        this.repositoryRepository = repositoryRepository;
        this.userRepository = userRepository;
        this.restClient = restClientBuilder.build();
    }

    /**
     * Constructs the GitHub App installation URL with an opaque single-use state token.
     */
    public String getInstallUrl(UserSession session) {
        String slug = properties.getAppSlug();
        if (slug == null || slug.isBlank()) {
            slug = "scan-pilot";
        }
        UUID userId = resolveUserId(session);
        String state = installationStateService.generateAndSaveState(userId, session.getSessionId());
        return "https://github.com/apps/" + slug.trim() + "/installations/new?state=" + state;
    }

    /**
     * Queries GitHub official REST API to list all app installations accessible to the user's access token,
     * following multi-page pagination. Fail-closed on missing fields, bad types, or remote HTTP errors.
     */
    public List<UserAccessibleInstallationDto> getUserAccessibleInstallations(String userAccessToken) {
        if (userAccessToken == null || userAccessToken.isBlank()) {
            throw new IllegalArgumentException("User access token is required");
        }

        List<UserAccessibleInstallationDto> result = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            Map<String, Object> response;
            try {
                response = restClient.get()
                        .uri(GITHUB_USER_INSTALLATIONS_URL + "?per_page=" + perPage + "&page=" + page)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new IllegalStateException("GITHUB_INSTALLATIONS_API_ERROR");
            }

            if (response == null || !response.containsKey("installations") || !(response.get("installations") instanceof List<?> rawList)) {
                throw new IllegalStateException("GITHUB_INSTALLATIONS_PARSE_ERROR");
            }

            if (rawList.isEmpty()) {
                break;
            }

            for (Object item : rawList) {
                if (!(item instanceof Map<?, ?> instMap)) {
                    throw new IllegalStateException("GITHUB_INSTALLATIONS_ROW_PARSE_ERROR");
                }
                Object rawId = instMap.get("id");
                if (!(rawId instanceof Number n)) {
                    throw new IllegalStateException("GITHUB_INSTALLATIONS_ID_MISSING");
                }
                Long id = n.longValue();

                Object rawAccount = instMap.get("account");
                if (!(rawAccount instanceof Map<?, ?> accountMap)) {
                    throw new IllegalStateException("GITHUB_INSTALLATIONS_ACCOUNT_MISSING");
                }
                Object rawAccountId = accountMap.get("id");
                Long accountId = rawAccountId instanceof Number an ? an.longValue() : null;
                String accountLogin = accountMap.get("login") instanceof String s ? s.trim() : "";
                if (accountLogin.isBlank()) {
                    throw new IllegalStateException("GITHUB_INSTALLATIONS_LOGIN_MISSING");
                }
                String accountType = accountMap.get("type") instanceof String s && !s.isBlank() ? s.trim() : "User";

                result.add(new UserAccessibleInstallationDto(id, accountId, accountLogin, accountType));
            }

            if (rawList.size() < perPage) {
                break;
            }
            page++;
        }

        return result;
    }

    /**
     * Queries GitHub official REST API to list all repositories accessible to the user access token
     * for a specific installation, following multi-page pagination. Fail-closed on missing fields or errors.
     */
    public List<GitHubRepositoryDto> getUserAccessibleInstallationRepositories(String userAccessToken, Long installationId) {
        if (userAccessToken == null || userAccessToken.isBlank()) {
            throw new IllegalArgumentException("User access token is required");
        }
        if (installationId == null) {
            throw new IllegalArgumentException("Installation ID is required");
        }

        List<GitHubRepositoryDto> result = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            String url = String.format(GITHUB_USER_INSTALLATION_REPOS_URL, installationId) + "?per_page=" + perPage + "&page=" + page;
            Map<String, Object> response;
            try {
                response = restClient.get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                        .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new IllegalStateException("GITHUB_INSTALLATION_REPOS_API_ERROR");
            }

            if (response == null || !response.containsKey("repositories") || !(response.get("repositories") instanceof List<?> rawList)) {
                throw new IllegalStateException("GITHUB_INSTALLATION_REPOS_PARSE_ERROR");
            }

            if (rawList.isEmpty()) {
                break;
            }

            for (Object item : rawList) {
                if (!(item instanceof Map<?, ?> repoMap)) {
                    throw new IllegalStateException("GITHUB_REPOSITORY_ROW_PARSE_ERROR");
                }
                Object rawId = repoMap.get("id");
                if (!(rawId instanceof Number n)) {
                    throw new IllegalStateException("GITHUB_REPOSITORY_ID_MISSING");
                }
                Long id = n.longValue();

                Object rawName = repoMap.get("name");
                if (!(rawName instanceof String name) || name.isBlank()) {
                    throw new IllegalStateException("GITHUB_REPOSITORY_NAME_MISSING");
                }

                Object rawFullName = repoMap.get("full_name");
                if (!(rawFullName instanceof String fullName) || fullName.isBlank()) {
                    throw new IllegalStateException("GITHUB_REPOSITORY_FULL_NAME_MISSING");
                }

                Object rawOwner = repoMap.get("owner");
                if (!(rawOwner instanceof Map<?, ?> ownerMap)) {
                    throw new IllegalStateException("GITHUB_REPOSITORY_OWNER_MISSING");
                }
                Object rawOwnerLogin = ownerMap.get("login");
                if (!(rawOwnerLogin instanceof String owner) || owner.isBlank()) {
                    throw new IllegalStateException("GITHUB_REPOSITORY_OWNER_LOGIN_MISSING");
                }

                Object rawDefaultBranch = repoMap.get("default_branch");
                if (!(rawDefaultBranch instanceof String defaultBranch) || defaultBranch.isBlank()) {
                    throw new IllegalStateException("GITHUB_REPOSITORY_DEFAULT_BRANCH_MISSING");
                }

                Object rawPrivate = repoMap.get("private");
                if (!(rawPrivate instanceof Boolean isPrivate)) {
                    throw new IllegalStateException("GITHUB_REPOSITORY_PRIVATE_FLAG_MISSING");
                }

                String htmlUrl = repoMap.get("html_url") instanceof String s ? s : "";
                String description = repoMap.get("description") instanceof String s ? s : "";

                result.add(new GitHubRepositoryDto(id, name.trim(), fullName.trim(), owner.trim(), defaultBranch.trim(), isPrivate, htmlUrl, description, false));
            }

            if (rawList.size() < perPage) {
                break;
            }
            page++;
        }

        return result;
    }

    /**
     * Fetches user accessible repositories from GitHub.
     */
    public List<GitHubRepositoryDto> getAccessibleRepositories(UserSession session) {
        if (session == null) {
            throw new IllegalArgumentException("User session is required");
        }

        Long installationId = session.getInstallationId();
        UUID userId = resolveUserId(session);
        Long currentSelectedRepoId = repositoryRepository.findByUserId(userId).stream()
                .findFirst()
                .map(com.scanpilot.persistence.entity.RepositoryEntity::getGithubRepoId)
                .orElse(null);

        if (installationId != null) {
            try {
                return getUserAccessibleInstallationRepositories(session.getAccessToken(), installationId);
            } catch (Exception e) {
                log.warn("Failed to fetch user-accessible installation repositories from remote provider");
            }
        }

        return fetchUserRepositories(session.getAccessToken(), currentSelectedRepoId);
    }

    private UUID resolveUserId(UserSession session) {
        return userRepository.findByGithubUserId(session.getGithubUserId())
                .map(UserEntity::getId)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .githubUserId(session.getGithubUserId())
                        .login(session.getLogin())
                        .name(session.getName())
                        .email(session.getEmail())
                        .avatarUrl(session.getAvatarUrl())
                        .createdAt(Instant.now())
                        .build()).getId());
    }

    private List<GitHubRepositoryDto> fetchUserRepositories(String userAccessToken, Long currentSelectedRepoId) {
        if (userAccessToken == null || userAccessToken.isBlank()) {
            return List.of();
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
            log.warn("Failed to fetch user repositories from remote provider");
            return List.of();
        }
    }

    private List<GitHubRepositoryDto> mapRawReposToDto(List<Map<String, Object>> reposList, Long currentSelectedRepoId) {
        List<GitHubRepositoryDto> dtos = new ArrayList<>();
        for (Map<String, Object> repo : reposList) {
            Long id = repo.get("id") instanceof Number n ? n.longValue() : null;
            String name = repo.get("name") instanceof String s ? s : "";
            String fullName = repo.get("full_name") instanceof String s ? s : "";
            Boolean isPrivate = repo.get("private") instanceof Boolean b ? b : false;
            String defaultBranch = repo.get("default_branch") instanceof String s ? s : "main";
            String htmlUrl = repo.get("html_url") instanceof String s ? s : "";
            String description = repo.get("description") instanceof String s ? s : "";

            Map<?, ?> ownerMap = repo.get("owner") instanceof Map<?, ?> m ? m : Map.of();
            String owner = ownerMap.get("login") instanceof String s ? s : "";

            if (id != null) {
                boolean isSelected = currentSelectedRepoId != null && currentSelectedRepoId.equals(id);
                dtos.add(new GitHubRepositoryDto(id, name, fullName, owner, defaultBranch, isPrivate, htmlUrl, description, isSelected));
            }
        }
        return dtos;
    }
}
