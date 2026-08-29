package com.scanpilot.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * REST client seam for GitHub Pull Requests, Branches, and File Contents API operations.
 */
@Slf4j
@Service
public class GitHubPullRequestClient {

    public static final String GITHUB_REPO_URL = "https://api.github.com/repos/%s/%s";
    public static final String GITHUB_REF_URL = "https://api.github.com/repos/%s/%s/git/ref/heads/%s";
    public static final String GITHUB_CREATE_REF_URL = "https://api.github.com/repos/%s/%s/git/refs";
    public static final String GITHUB_CONTENTS_URL = "https://api.github.com/repos/%s/%s/contents/%s";
    public static final String GITHUB_PULLS_URL = "https://api.github.com/repos/%s/%s/pulls";

    public static final java.time.Duration CONNECT_TIMEOUT = java.time.Duration.ofSeconds(10);
    public static final java.time.Duration READ_TIMEOUT = java.time.Duration.ofSeconds(20);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public record DefaultBranchHead(String branchName, String commitSha) {}
    public record GitHubPrResult(int prNumber, String htmlUrl) {}

    @Getter
    public static class GitHubPrClientException extends RuntimeException {
        private final int statusCode;
        private final String errorCode;

        public GitHubPrClientException(int statusCode, String errorCode) {
            super(errorCode);
            this.statusCode = statusCode;
            this.errorCode = errorCode;
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GitHubPullRequestClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this(restClientBuilder, createDefaultRequestFactory(), objectMapper);
    }

    public GitHubPullRequestClient(RestClient.Builder restClientBuilder, org.springframework.http.client.ClientHttpRequestFactory requestFactory, ObjectMapper objectMapper) {
        if (requestFactory != null) {
            restClientBuilder.requestFactory(requestFactory);
        }
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public static org.springframework.http.client.ClientHttpRequestFactory createDefaultRequestFactory() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        return factory;
    }

    /**
     * Resolves the default branch name and latest commit SHA for a repository.
     */
    public DefaultBranchHead getDefaultBranchHead(String owner, String repo, String installationToken) {
        String repoUri = String.format(GITHUB_REPO_URL, owner.trim(), repo.trim());
        try {
            Map<String, Object> repoResp = restClient.get()
                    .uri(repoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            String defaultBranch = repoResp != null && repoResp.get("default_branch") != null
                    ? repoResp.get("default_branch").toString()
                    : "main";

            String refUri = String.format(GITHUB_REF_URL, owner.trim(), repo.trim(), defaultBranch);
            Map<String, Object> refResp = restClient.get()
                    .uri(refUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            String commitSha = "";
            if (refResp != null && refResp.get("object") instanceof Map<?, ?> objMap) {
                commitSha = objMap.get("sha") != null ? objMap.get("sha").toString() : "";
            }

            return new DefaultBranchHead(defaultBranch, commitSha);
        } catch (HttpClientErrorException e) {
            log.error("GitHub API error fetching default branch for {}/{}: {}", owner, repo, e.getStatusCode());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            log.error("Timeout connecting to GitHub API for {}/{}", owner, repo);
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (Exception e) {
            log.error("Unexpected error fetching default branch for {}/{}", owner, repo, e);
            throw new GitHubPrClientException(500, "GITHUB_INTERNAL_ERROR");
        }
    }

    /**
     * Fetches file content from repository at a specific ref.
     */
    public String getFileContent(String owner, String repo, String filePath, String ref, String installationToken) {
        String uri = String.format(GITHUB_CONTENTS_URL, owner.trim(), repo.trim(), filePath.trim()) + "?ref=" + ref.trim();
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp == null || resp.get("content") == null) {
                throw new GitHubPrClientException(404, "FILE_NOT_FOUND");
            }

            String base64Content = resp.get("content").toString().replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64Content);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (HttpClientErrorException e) {
            log.error("GitHub API error fetching file content {}/{}/{}: {}", owner, repo, filePath, e.getStatusCode());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (GitHubPrClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching file content {}/{}/{}", owner, repo, filePath, e);
            throw new GitHubPrClientException(500, "GITHUB_INTERNAL_ERROR");
        }
    }

    /**
     * Creates a git reference / branch.
     */
    public void createBranch(String owner, String repo, String branchName, String commitSha, String installationToken) {
        String uri = String.format(GITHUB_CREATE_REF_URL, owner.trim(), repo.trim());
        Map<String, String> body = Map.of(
                "ref", "refs/heads/" + branchName.trim(),
                "sha", commitSha.trim()
        );

        try {
            restClient.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            log.error("GitHub API error creating branch {} on {}/{}: {}", branchName, owner, repo, e.getStatusCode());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (Exception e) {
            log.error("Unexpected error creating branch {} on {}/{}", branchName, owner, repo, e);
            throw new GitHubPrClientException(500, "GITHUB_INTERNAL_ERROR");
        }
    }

    /**
     * Updates or creates a file on a specific branch.
     */
    public void updateFileContent(String owner, String repo, String filePath, String commitMessage, String newContent, String branchName, String installationToken) {
        String uri = String.format(GITHUB_CONTENTS_URL, owner.trim(), repo.trim(), filePath.trim());

        // First get existing file SHA on the branch
        String existingSha = null;
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(uri + "?ref=" + branchName.trim())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp != null && resp.get("sha") != null) {
                existingSha = resp.get("sha").toString();
            }
        } catch (Exception e) {
            log.debug("No existing file SHA found for {}/{} on branch {}", owner, repo, branchName);
        }

        String base64Encoded = Base64.getEncoder().encodeToString(newContent.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> body = existingSha != null
                ? Map.of("message", commitMessage, "content", base64Encoded, "branch", branchName, "sha", existingSha)
                : Map.of("message", commitMessage, "content", base64Encoded, "branch", branchName);

        try {
            restClient.put()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            log.error("GitHub API error updating file {} on {}/{}: {}", filePath, owner, repo, e.getStatusCode());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (Exception e) {
            log.error("Unexpected error updating file {} on {}/{}", filePath, owner, repo, e);
            throw new GitHubPrClientException(500, "GITHUB_INTERNAL_ERROR");
        }
    }

    /**
     * Opens a new Pull Request.
     */
    public GitHubPrResult createPullRequest(String owner, String repo, String title, String body, String headBranch, String baseBranch, String installationToken) {
        String uri = String.format(GITHUB_PULLS_URL, owner.trim(), repo.trim());
        Map<String, String> reqBody = Map.of(
                "title", title,
                "body", body,
                "head", headBranch.trim(),
                "base", baseBranch.trim()
        );

        try {
            Map<String, Object> resp = restClient.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reqBody)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp == null || resp.get("number") == null) {
                throw new GitHubPrClientException(500, "INVALID_GITHUB_PR_RESPONSE");
            }

            int prNumber = ((Number) resp.get("number")).intValue();
            String htmlUrl = resp.get("html_url") != null ? resp.get("html_url").toString() : "";

            return new GitHubPrResult(prNumber, htmlUrl);
        } catch (HttpClientErrorException e) {
            log.error("GitHub API error creating PR for {}/{}: {}", owner, repo, e.getStatusCode());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (GitHubPrClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating PR for {}/{}", owner, repo, e);
            throw new GitHubPrClientException(500, "GITHUB_INTERNAL_ERROR");
        }
    }

    private String mapHttpError(int statusCode) {
        return switch (statusCode) {
            case 401, 403 -> "GITHUB_AUTH_FAILED";
            case 404 -> "REPOSITORY_OR_RESOURCE_NOT_FOUND";
            case 409 -> "GITHUB_CONFLICT_ERROR";
            case 422 -> "GITHUB_VALIDATION_FAILED";
            default -> "GITHUB_API_ERROR";
        };
    }
}