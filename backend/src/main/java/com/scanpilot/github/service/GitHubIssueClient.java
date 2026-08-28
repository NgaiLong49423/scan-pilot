package com.scanpilot.github.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST client seam for GitHub Issues API operations with installation token.
 */
@Slf4j
@Service
public class GitHubIssueClient {

    public static final String GITHUB_ISSUES_URL = "https://api.github.com/repos/%s/%s/issues";

    public static final java.time.Duration CONNECT_TIMEOUT = java.time.Duration.ofSeconds(10);
    public static final java.time.Duration READ_TIMEOUT = java.time.Duration.ofSeconds(20);
    public static final java.time.Duration MAX_REQUEST_DURATION = CONNECT_TIMEOUT.plus(READ_TIMEOUT);

    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public GitHubIssueClient(RestClient.Builder restClientBuilder) {
        this(restClientBuilder, createDefaultRequestFactory());
    }

    public GitHubIssueClient(RestClient.Builder restClientBuilder, org.springframework.http.client.ClientHttpRequestFactory requestFactory) {
        if (requestFactory != null) {
            restClientBuilder.requestFactory(requestFactory);
        }
        this.restClient = restClientBuilder.build();
    }

    public static org.springframework.http.client.ClientHttpRequestFactory createDefaultRequestFactory() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        return factory;
    }

    public record GitHubIssueResult(int issueNumber, String htmlUrl) {}

    @Getter
    public static class GitHubClientException extends RuntimeException {
        private final int statusCode;
        private final String errorCode;

        public GitHubClientException(int statusCode, String errorCode) {
            super(errorCode);
            this.statusCode = statusCode;
            this.errorCode = errorCode;
        }
    }

    @Getter
    public static class GitHubAmbiguousException extends RuntimeException {
        private final String errorCode;

        public GitHubAmbiguousException(String errorCode) {
            super(errorCode);
            this.errorCode = errorCode;
        }
    }

    /**
     * Creates a new GitHub issue on the specified repository.
     */
    public GitHubIssueResult createIssue(
        String owner,
        String repo,
        String installationToken,
        String title,
        String body
    ) {
        if (owner == null || owner.isBlank() || repo == null || repo.isBlank()) {
            throw new IllegalArgumentException("Repository owner and name must not be blank");
        }
        if (installationToken == null || installationToken.isBlank()) {
            throw new IllegalArgumentException("Installation token must not be blank");
        }

        String uri = String.format(GITHUB_ISSUES_URL, owner.trim(), repo.trim());
        Map<String, String> requestBody = Map.of("title", title, "body", body);

        try {
            Map<String, Object> response = restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || !response.containsKey("number") || !response.containsKey("html_url")) {
                log.warn("GitHub issue API returned malformed response payload structure");
                throw new GitHubAmbiguousException("MALFORMED_RESPONSE");
            }

            int number = ((Number) response.get("number")).intValue();
            String htmlUrl = (String) response.get("html_url");

            return new GitHubIssueResult(number, htmlUrl);

        } catch (HttpClientErrorException e) {
            // Definite 4xx rejection by GitHub
            log.warn("GitHub rejected issue creation: status={}", e.getStatusCode().value());
            throw new GitHubClientException(e.getStatusCode().value(), "GITHUB_REJECTED_CLIENT_ERROR");
        } catch (HttpServerErrorException e) {
            // 5xx Server error from GitHub (ambiguous outcome)
            log.warn("GitHub server error during issue creation: status={}", e.getStatusCode().value());
            throw new GitHubAmbiguousException("GITHUB_SERVER_ERROR_5XX");
        } catch (ResourceAccessException e) {
            // Timeout / network dropped connection (ambiguous outcome)
            log.warn("Network timeout or connection drop during GitHub issue creation");
            throw new GitHubAmbiguousException("NETWORK_TIMEOUT");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("GitHub client error during issue creation: status={}", e.getStatusCode().value());
                throw new GitHubClientException(e.getStatusCode().value(), "GITHUB_REJECTED_CLIENT_ERROR");
            }
            log.warn("Ambiguous HTTP error during GitHub issue creation: status={}", e.getStatusCode().value());
            throw new GitHubAmbiguousException("AMBIGUOUS_HTTP_ERROR");
        } catch (Exception e) {
            log.warn("Unexpected failure communicating with GitHub issue API");
            throw new GitHubAmbiguousException("UNEXPECTED_COMMUNICATION_FAILURE");
        }
    }

    /**
     * Searches existing repository issues to find any issue containing the specified marker across all pages.
     * Excludes pull requests from marker matching.
     */
    public Optional<GitHubIssueResult> findIssueByMarker(
        String owner,
        String repo,
        String installationToken,
        String marker
    ) {
        if (owner == null || owner.isBlank() || repo == null || repo.isBlank()) {
            throw new IllegalArgumentException("Repository owner and name must not be blank");
        }
        if (installationToken == null || installationToken.isBlank()) {
            throw new IllegalArgumentException("Installation token must not be blank");
        }
        if (marker == null || marker.isBlank()) {
            return Optional.empty();
        }

        int page = 1;
        while (true) {
            String uri = String.format(GITHUB_ISSUES_URL + "?state=all&per_page=100&page=%d", owner.trim(), repo.trim(), page);

            try {
                List<Map<String, Object>> issues = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

                if (issues == null || issues.isEmpty()) {
                    return Optional.empty();
                }

                for (Map<String, Object> issue : issues) {
                    // Exclude pull requests (GitHub Issues API includes PRs with a 'pull_request' key)
                    if (issue.containsKey("pull_request") && issue.get("pull_request") != null) {
                        continue;
                    }

                    String body = (String) issue.get("body");
                    if (body != null && body.contains(marker.trim())) {
                        int number = ((Number) issue.get("number")).intValue();
                        String htmlUrl = (String) issue.get("html_url");
                        return Optional.of(new GitHubIssueResult(number, htmlUrl));
                    }
                }

                if (issues.size() < 100) {
                    return Optional.empty();
                }

                page++;

            } catch (HttpClientErrorException e) {
                log.warn("GitHub rejected marker reconciliation query: status={}", e.getStatusCode().value());
                throw new GitHubClientException(e.getStatusCode().value(), "GITHUB_REJECTED_RECONCILIATION");
            } catch (Exception e) {
                log.warn("Marker reconciliation failed on page {}", page);
                throw new GitHubAmbiguousException("RECONCILIATION_QUERY_FAILED");
            }
        }
    }
}
