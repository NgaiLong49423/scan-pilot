package com.scanpilot.github.controller;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.github.dto.GitHubRepositoryDto;
import com.scanpilot.github.service.GitHubAppService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @MockitoBean
    private GitHubAppService gitHubAppService;

    private UserSession userSession;

    @BeforeEach
    void setUp() {
        sessionService.clearAllSessions();
        userSession = sessionService.createSession(
                12345L,
                "octocat",
                "The Octocat",
                "https://avatars.githubusercontent.com/u/12345",
                "octocat@github.com",
                "gho_valid_token"
        );
    }

    @Test
    @DisplayName("GET /api/v1/github/install-url with auth returns install URL with state")
    void testGetInstallUrlAuthenticated() throws Exception {
        when(gitHubAppService.getInstallUrl(any(UserSession.class)))
                .thenReturn("https://github.com/apps/scan-pilot/installations/new?state=test-state-123");

        mockMvc.perform(get("/api/v1/github/install-url")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installUrl").value("https://github.com/apps/scan-pilot/installations/new?state=test-state-123"));
    }

    @Test
    @DisplayName("GET /api/v1/github/install-url without auth returns 401 Unauthorized")
    void testGetInstallUrlUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/github/install-url"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/github/repositories with auth returns list of accessible repositories")
    void testGetRepositoriesAuthenticated() throws Exception {
        List<GitHubRepositoryDto> repos = List.of(
                new GitHubRepositoryDto(1L, "repo-one", "octocat/repo-one", "octocat", "main", false, "https://github.com/octocat/repo-one", "desc 1", false),
                new GitHubRepositoryDto(2L, "repo-two", "octocat/repo-two", "octocat", "develop", true, "https://github.com/octocat/repo-two", "desc 2", true)
        );

        when(gitHubAppService.getAccessibleRepositories(any(UserSession.class))).thenReturn(repos);

        mockMvc.perform(get("/api/v1/github/repositories")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("repo-one"))
                .andExpect(jsonPath("$[0].fullName").value("octocat/repo-one"))
                .andExpect(jsonPath("$[0].owner").value("octocat"))
                .andExpect(jsonPath("$[0].defaultBranch").value("main"))
                .andExpect(jsonPath("$[0].isPrivate").value(false))
                .andExpect(jsonPath("$[0].isSelected").value(false))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].isPrivate").value(true))
                .andExpect(jsonPath("$[1].isSelected").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/github/repositories without auth returns 401 Unauthorized")
    void testGetRepositoriesUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/github/repositories"))
                .andExpect(status().isUnauthorized());
    }
}
