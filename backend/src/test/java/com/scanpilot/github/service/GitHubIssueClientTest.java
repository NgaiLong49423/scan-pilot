package com.scanpilot.github.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("GitHub Issue Client Unit Tests")
class GitHubIssueClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private GitHubIssueClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new GitHubIssueClient(restClientBuilder, null);
    }

    @Test
    @DisplayName("GIVEN marker on page 2 WHEN findIssueByMarker THEN paginates and finds issue")
    void testFindIssueOnPage2() {
        StringBuilder page1Full = new StringBuilder("[");
        for (int i = 1; i <= 100; i++) {
            page1Full.append(String.format("{\"number\": %d, \"body\": \"issue %d\", \"html_url\": \"https://github.com/owner/repo/issues/%d\"}", i, i, i));
            if (i < 100) page1Full.append(",");
        }
        page1Full.append("]");

        String page2Json = """
            [
              {"number": 105, "body": "<!-- scan-pilot-finding-id: 3fa85f64-5717-4562-b3fc-2c963f66afa6 -->", "html_url": "https://github.com/owner/repo/issues/105"}
            ]
            """;

        mockServer.expect(requestTo("https://api.github.com/repos/owner/repo/issues?state=all&per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-123"))
                .andRespond(withSuccess(page1Full.toString(), MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.github.com/repos/owner/repo/issues?state=all&per_page=100&page=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-123"))
                .andRespond(withSuccess(page2Json, MediaType.APPLICATION_JSON));

        Optional<GitHubIssueClient.GitHubIssueResult> result = client.findIssueByMarker(
                "owner", "repo", "token-123", "<!-- scan-pilot-finding-id: 3fa85f64-5717-4562-b3fc-2c963f66afa6 -->"
        );

        assertThat(result).isPresent();
        assertThat(result.get().issueNumber()).isEqualTo(105);
        assertThat(result.get().htmlUrl()).isEqualTo("https://github.com/owner/repo/issues/105");
        mockServer.verify();
    }

    @Test
    @DisplayName("GIVEN pull request with marker WHEN findIssueByMarker THEN excludes pull request and returns empty")
    void testPullRequestWithMarkerIsExcluded() {
        String pageJson = """
            [
              {
                "number": 50,
                "body": "<!-- scan-pilot-finding-id: 3fa85f64-5717-4562-b3fc-2c963f66afa6 -->",
                "html_url": "https://github.com/owner/repo/pull/50",
                "pull_request": {"url": "https://api.github.com/repos/owner/repo/pulls/50"}
              }
            ]
            """;

        mockServer.expect(requestTo("https://api.github.com/repos/owner/repo/issues?state=all&per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(pageJson, MediaType.APPLICATION_JSON));

        Optional<GitHubIssueClient.GitHubIssueResult> result = client.findIssueByMarker(
                "owner", "repo", "token-123", "<!-- scan-pilot-finding-id: 3fa85f64-5717-4562-b3fc-2c963f66afa6 -->"
        );

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("GIVEN page traversal network error WHEN findIssueByMarker THEN throws GitHubAmbiguousException")
    void testPageTraversalFailureThrowsAmbiguous() {
        mockServer.expect(requestTo("https://api.github.com/repos/owner/repo/issues?state=all&per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        GitHubIssueClient.GitHubAmbiguousException ex = assertThrows(
                GitHubIssueClient.GitHubAmbiguousException.class,
                () -> client.findIssueByMarker("owner", "repo", "token-123", "marker")
        );

        assertThat(ex.getErrorCode()).isEqualTo("RECONCILIATION_QUERY_FAILED");
        mockServer.verify();
    }

    @Test
    @DisplayName("AC-A Proof: GitHub external timeout is strictly below stale PENDING threshold")
    void testTimeoutsAreStrictlyBelowStalePendingThreshold() {
        assertThat(GitHubIssueClient.MAX_REQUEST_DURATION)
                .isLessThan(com.scanpilot.scanner.issue.FindingIssueService.STALE_PENDING_THRESHOLD);
        assertThat(GitHubIssueClient.CONNECT_TIMEOUT.toSeconds()).isEqualTo(10);
        assertThat(GitHubIssueClient.READ_TIMEOUT.toSeconds()).isEqualTo(20);
        assertThat(GitHubIssueClient.MAX_REQUEST_DURATION.toSeconds()).isEqualTo(30);
        assertThat(com.scanpilot.scanner.issue.FindingIssueService.STALE_PENDING_THRESHOLD.toSeconds()).isEqualTo(60);
    }
}
