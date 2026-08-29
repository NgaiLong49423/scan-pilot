package com.scanpilot.github.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(GitHubPullRequestClient.class)
@DisplayName("GitHub Pull Request Client Fail-Closed and Parsing Tests")
class GitHubPullRequestClientTest {

    @Autowired
    private GitHubPullRequestClient client;

    @Autowired
    private MockRestServiceServer server;

    private static final String OWNER = "alice";
    private static final String REPO = "safe-repo";
    private static final String TOKEN = "ghs_mockInstallationToken";
    private static final String VALID_SHA = "1234567890abcdef1234567890abcdef12345678";

    @Test
    @DisplayName("P1-4: Resolves default branch and valid 40-hex SHA successfully")
    void testGetDefaultBranchHeadSuccess() {
        server.expect(requestTo("https://api.github.com/repos/alice/safe-repo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"default_branch\":\"main\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://api.github.com/repos/alice/safe-repo/git/ref/heads/main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"object\":{\"sha\":\"" + VALID_SHA + "\"}}", MediaType.APPLICATION_JSON));

        GitHubPullRequestClient.DefaultBranchHead head = client.getDefaultBranchHead(OWNER, REPO, TOKEN);

        assertThat(head).isNotNull();
        assertThat(head.branchName()).isEqualTo("main");
        assertThat(head.commitSha()).isEqualTo(VALID_SHA);
        server.verify();
    }

    @Test
    @DisplayName("P1-4: Fails closed with INVALID_DEFAULT_BRANCH when default_branch is missing in repo response")
    void testGetDefaultBranchMissingFailsClosed() {
        server.expect(requestTo("https://api.github.com/repos/alice/safe-repo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"name\":\"safe-repo\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getDefaultBranchHead(OWNER, REPO, TOKEN))
                .isInstanceOf(GitHubPullRequestClient.GitHubPrClientException.class)
                .satisfies(ex -> {
                    GitHubPullRequestClient.GitHubPrClientException gpe = (GitHubPullRequestClient.GitHubPrClientException) ex;
                    assertThat(gpe.getErrorCode()).isEqualTo("INVALID_DEFAULT_BRANCH");
                    assertThat(gpe.getStatusCode()).isEqualTo(502);
                });
        server.verify();
    }

    @Test
    @DisplayName("P1-4: Fails closed with INVALID_HEAD_SHA when commit SHA is not 40-hex or missing")
    void testGetDefaultBranchHeadMalformedShaFailsClosed() {
        server.expect(requestTo("https://api.github.com/repos/alice/safe-repo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"default_branch\":\"main\"}", MediaType.APPLICATION_JSON));

        // SHA is only 7 chars (short SHA) or non-hex
        server.expect(requestTo("https://api.github.com/repos/alice/safe-repo/git/ref/heads/main"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"object\":{\"sha\":\"1234567\"}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getDefaultBranchHead(OWNER, REPO, TOKEN))
                .isInstanceOf(GitHubPullRequestClient.GitHubPrClientException.class)
                .satisfies(ex -> {
                    GitHubPullRequestClient.GitHubPrClientException gpe = (GitHubPullRequestClient.GitHubPrClientException) ex;
                    assertThat(gpe.getErrorCode()).isEqualTo("INVALID_HEAD_SHA");
                    assertThat(gpe.getStatusCode()).isEqualTo(502);
                });
        server.verify();
    }
}