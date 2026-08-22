package com.scanpilot.project.controller;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.project.service.ProjectService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private com.scanpilot.persistence.repository.RepositoryRepository repositoryRepository;

    @Autowired
    private com.scanpilot.persistence.repository.UserRepository userRepository;

    @Autowired
    private com.scanpilot.persistence.repository.MonitoredBranchRepository monitoredBranchRepository;

    private UserSession userSession;

    @BeforeEach
    void setUp() {
        monitoredBranchRepository.deleteAll();
        repositoryRepository.deleteAll();
        userRepository.deleteAll();
        sessionService.clearAllSessions();
        projectService.clearAllProjects();

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
    @DisplayName("POST /api/v1/projects/select-repository selects repo and derives PRIMARY branch")
    void testSelectRepositoryAuthenticated() throws Exception {
        String requestBody = """
                {
                    "githubRepoId": 777,
                    "fullName": "octocat/secure-guard",
                    "name": "secure-guard",
                    "owner": "octocat",
                    "defaultBranch": "main",
                    "isPrivate": true
                }
                """;

        mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.githubRepoId").value(777))
                .andExpect(jsonPath("$.fullName").value("octocat/secure-guard"))
                .andExpect(jsonPath("$.defaultBranch").value("main"))
                .andExpect(jsonPath("$.primaryBranch").value("main"))
                .andExpect(jsonPath("$.secondaryBranches", hasSize(0)))
                .andExpect(jsonPath("$.isPrivate").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/select-repository without auth returns 401 Unauthorized")
    void testSelectRepositoryUnauthenticated() throws Exception {
        String requestBody = """
                {
                    "githubRepoId": 777,
                    "fullName": "octocat/secure-guard"
                }
                """;

        mockMvc.perform(post("/api/v1/projects/select-repository")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/projects/select-repository with invalid payload returns 400 Bad Request")
    void testSelectRepositoryInvalidPayload() throws Exception {
        String requestBody = """
                {
                    "fullName": ""
                }
                """;

        mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/projects/current returns 404 when no repo selected")
    void testGetCurrentProjectNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/projects/current")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/projects/current returns active monitored project when selected")
    void testGetCurrentProjectFound() throws Exception {
        String selectBody = """
                {
                    "githubRepoId": 888,
                    "fullName": "octocat/monitored-app",
                    "defaultBranch": "main"
                }
                """;

        mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/current")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubRepoId").value(888))
                .andExpect(jsonPath("$.fullName").value("octocat/monitored-app"))
                .andExpect(jsonPath("$.primaryBranch").value("main"));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/branches updates secondary branch slots (up to 2)")
    void testUpdateBranchConfigurationSuccess() throws Exception {
        String selectBody = """
                {
                    "githubRepoId": 888,
                    "fullName": "octocat/monitored-app",
                    "defaultBranch": "main"
                }
                """;

        String selectResp = mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String repoId = com.jayway.jsonpath.JsonPath.read(selectResp, "$.id");

        String branchBody = String.format("""
                {
                    "repositoryId": "%s",
                    "secondaryBranches": ["develop", "release/v1.0"]
                }
                """, repoId);

        mockMvc.perform(put("/api/v1/projects/branches")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secondaryBranches", hasSize(2)))
                .andExpect(jsonPath("$.secondaryBranches[0]").value("develop"))
                .andExpect(jsonPath("$.secondaryBranches[1]").value("release/v1.0"));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/branches rejects more than 2 secondary branches with 400 Bad Request")
    void testUpdateBranchConfigurationExceedsMax() throws Exception {
        String selectBody = """
                {
                    "githubRepoId": 888,
                    "fullName": "octocat/monitored-app",
                    "defaultBranch": "main"
                }
                """;

        String selectResp = mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String repoId = com.jayway.jsonpath.JsonPath.read(selectResp, "$.id");

        String branchBody = String.format("""
                {
                    "repositoryId": "%s",
                    "secondaryBranches": ["develop", "release/v1.0", "staging"]
                }
                """, repoId);

        mockMvc.perform(put("/api/v1/projects/branches")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Maximum of 2 secondary branches allowed"));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/branches without repositoryId returns 400 Bad Request (fail-closed)")
    void testUpdateBranchConfigurationMissingRepositoryId() throws Exception {
        String branchBody = """
                {
                    "secondaryBranches": ["develop"]
                }
                """;

        mockMvc.perform(put("/api/v1/projects/branches")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/projects/branches with non-existent repositoryId returns 404 Not Found (fail-closed)")
    void testUpdateBranchConfigurationNonExistentRepositoryId() throws Exception {
        String branchBody = String.format("""
                {
                    "repositoryId": "%s",
                    "secondaryBranches": ["develop"]
                }
                """, java.util.UUID.randomUUID());

        mockMvc.perform(put("/api/v1/projects/branches")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchBody))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/projects/branches without auth returns 401 Unauthorized")
    void testUpdateBranchConfigurationUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/projects/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secondaryBranches\": [\"develop\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/projects/monitored returns list of monitored repositories from PostgreSQL")
    void testGetAllMonitoredProjects() throws Exception {
        String selectBody1 = """
                {
                    "githubRepoId": 101,
                    "fullName": "octocat/app-one",
                    "defaultBranch": "main"
                }
                """;
        String selectBody2 = """
                {
                    "githubRepoId": 102,
                    "fullName": "octocat/app-two",
                    "defaultBranch": "main"
                }
                """;

        mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/monitored")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Branch configuration updates are persisted in PostgreSQL and isolated between Repo A and Repo B")
    void testBranchConfigurationPersistenceAndIsolation() throws Exception {
        String selectBody1 = """
                {
                    "githubRepoId": 105,
                    "fullName": "octocat/persisted-repo-a",
                    "defaultBranch": "main"
                }
                """;
        String selectBody2 = """
                {
                    "githubRepoId": 106,
                    "fullName": "octocat/persisted-repo-b",
                    "defaultBranch": "main"
                }
                """;

        String resp1 = mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String resp2 = mockMvc.perform(post("/api/v1/projects/select-repository")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selectBody2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String repoIdA = com.jayway.jsonpath.JsonPath.read(resp1, "$.id");
        String repoIdB = com.jayway.jsonpath.JsonPath.read(resp2, "$.id");

        String branchBodyA = String.format("""
                {
                    "repositoryId": "%s",
                    "secondaryBranches": ["develop", "staging"]
                }
                """, repoIdA);

        mockMvc.perform(put("/api/v1/projects/branches")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchBodyA))
                .andExpect(status().isOk());

        // Verify Repo A has branches and Repo B has no secondary branches
        mockMvc.perform(get("/api/v1/projects/monitored")
                        .cookie(new Cookie("SCANPILOT_SESSION", userSession.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.fullName == 'octocat/persisted-repo-a')].secondaryBranches[0]").value("develop"))
                .andExpect(jsonPath("$[?(@.fullName == 'octocat/persisted-repo-b')].secondaryBranches[0]").doesNotExist());
    }
}
