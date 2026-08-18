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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("GET /api/v1/github/install-url returns install URL")
    void testGetInstallUrl() throws Exception {
        when(gitHubAppService.getInstallUrl()).thenReturn("https://github.com/apps/scan-pilot/installations/new");

        mockMvc.perform(get("/api/v1/github/install-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installUrl").value("https://github.com/apps/scan-pilot/installations/new"));
    }

    @Test
    @DisplayName("POST /api/v1/github/installations/link with auth links installation ID")
    void testLinkInstallationAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/github/installations/link")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"installationId\": 98765}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Installation linked successfully"))
                .andExpect(jsonPath("$.installationId").value(98765));

        verify(gitHubAppService).linkInstallation(any(UserSession.class), eq(98765L));
    }

    @Test
    @DisplayName("POST /api/v1/github/installations/link without auth returns 401 Unauthorized")
    void testLinkInstallationUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/github/installations/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"installationId\": 98765}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/github/installations/link with null installationId returns 400 Bad Request")
    void testLinkInstallationInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/github/installations/link")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/github/repositories with auth returns list of accessible repositories")
    void testGetRepositoriesAuthenticated() throws Exception {
        List<GitHubRepositoryDto> repos = List.of(
                new GitHubRepositoryDto(1L, "repo-one", "octocat/repo-one", "octocat", "main", false, "https://github.com/octocat/repo-one", "First repo", false),
                new GitHubRepositoryDto(2L, "repo-two", "octocat/repo-two", "octocat", "develop", true, "https://github.com/octocat/repo-two", "Second repo", true)
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
