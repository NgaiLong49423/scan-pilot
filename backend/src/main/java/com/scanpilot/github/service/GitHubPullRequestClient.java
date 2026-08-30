package com.scanpilot.github.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GitHubPullRequestClient {

    public static final String GITHUB_REPO_URL = "https://api.github.com/repos/%s/%s";
    public static final String GITHUB_REF_URL = "https://api.github.com/repos/%s/%s/git/ref/heads/%s";
    public static final String GITHUB_CREATE_REF_URL = "https://api.github.com/repos/%s/%s/git/refs";
    public static final String GITHUB_CONTENTS_URL = "https://api.github.com/repos/%s/%s/contents/%s";
    public static final String GITHUB_PULLS_URL = "https://api.github.com/repos/%s/%s/pulls";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern SHA40_HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{40}$");

    private final RestClient restClient;

    public GitHubPullRequestClient(RestClient.Builder restClientBuilder, RestClientCustomizer... customizers) {
        RestClient.Builder builder = restClientBuilder
                .requestFactory(createRequestFactory());
        for (RestClientCustomizer customizer : customizers) {
            customizer.customize(builder);
        }
        this.restClient = builder.build();
    }

    public record DefaultBranchHead(String branchName, String commitSha) {}
    public record GitHubPrResult(int prNumber, String htmlUrl) {}

    public static class GitHubPrClientException extends RuntimeException {
        private final int statusCode;
        private final String errorCode;

        public GitHubPrClientException(int statusCode, String errorCode) {
            super(errorCode);
            this.statusCode = statusCode;
            this.errorCode = errorCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    private SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        return factory;
    }

    /**
     * Resolves the default branch name and latest commit SHA for a repository.
     * Fails closed if repository response or commit SHA is missing or malformed.
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

            if (repoResp == null || repoResp.get("default_branch") == null) {
                throw new GitHubPrClientException(502, "INVALID_DEFAULT_BRANCH");
            }

            String defaultBranch = repoResp.get("default_branch").toString().trim();
            if (defaultBranch.isBlank()) {
                throw new GitHubPrClientException(502, "INVALID_DEFAULT_BRANCH");
            }

            String refUri = String.format(GITHUB_REF_URL, owner.trim(), repo.trim(), defaultBranch);
            Map<String, Object> refResp = restClient.get()
                    .uri(refUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            String commitSha = null;
            if (refResp != null && refResp.get("object") instanceof Map<?, ?> objMap) {
                commitSha = objMap.get("sha") != null ? objMap.get("sha").toString().trim() : null;
            }

            if (commitSha == null || !SHA40_HEX_PATTERN.matcher(commitSha).matches()) {
                throw new GitHubPrClientException(502, "INVALID_HEAD_SHA");
            }

            return new DefaultBranchHead(defaultBranch, commitSha);
        } catch (HttpClientErrorException e) {
            log.error("GitHub API error fetching default branch for {}/{}: HTTP {}", owner, repo, e.getStatusCode().value());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            log.error("Timeout connecting to GitHub API for {}/{}", owner, repo);
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (GitHubPrClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching default branch for {}/{}: {}", owner, repo, e.getClass().getSimpleName());
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
            log.error("GitHub API error fetching file content for {}/{}: HTTP {}", owner, repo, e.getStatusCode().value());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            log.error("Timeout connecting to GitHub API for {}/{}", owner, repo);
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (GitHubPrClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching file content for {}/{}: {}", owner, repo, e.getClass().getSimpleName());
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
            log.error("GitHub API error creating branch for {}/{}: HTTP {}", owner, repo, e.getStatusCode().value());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            log.error("Timeout connecting to GitHub API for {}/{}", owner, repo);
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (Exception e) {
            log.error("Unexpected error creating branch for {}/{}: {}", owner, repo, e.getClass().getSimpleName());
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
            log.debug("No existing file SHA found for {}/{} on branch", owner, repo);
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
            log.error("GitHub API error updating file for {}/{}: HTTP {}", owner, repo, e.getStatusCode().value());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            log.error("Timeout connecting to GitHub API for {}/{}", owner, repo);
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (Exception e) {
            log.error("Unexpected error updating file for {}/{}: {}", owner, repo, e.getClass().getSimpleName());
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
            log.error("GitHub API error creating PR for {}/{}: HTTP {}", owner, repo, e.getStatusCode().value());
            throw new GitHubPrClientException(e.getStatusCode().value(), mapHttpError(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            log.error("Timeout connecting to GitHub API for {}/{}", owner, repo);
            throw new GitHubPrClientException(504, "GITHUB_COMMUNICATION_TIMEOUT");
        } catch (GitHubPrClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating PR for {}/{}: {}", owner, repo, e.getClass().getSimpleName());
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