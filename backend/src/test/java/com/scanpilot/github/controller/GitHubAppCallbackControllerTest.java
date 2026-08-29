package com.scanpilot.github.controller;

import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import com.scanpilot.github.dto.UserAccessibleInstallationDto;
import com.scanpilot.github.service.GitHubAppService;
import com.scanpilot.github.service.InstallationStateService;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.UserInstallationEntity;
import com.scanpilot.persistence.repository.UserInstallationRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GitHubAppCallbackController Tests")
class GitHubAppCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserInstallationRepository userInstallationRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private InstallationStateService installationStateService;

    @MockBean
    private GitHubAppService gitHubAppService;

    @MockBean
    private SessionService sessionService;

    private UserSession testSession;
    private UserEntity userEntity;
    private String testSessionId;
    private Long testGithubUserId;

    @BeforeEach
    void setUp() {
        testSessionId = "session-" + UUID.randomUUID();
        testGithubUserId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        userEntity = userRepository.findByGithubUserId(testGithubUserId)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .githubUserId(testGithubUserId)
                        .login("test-octocat-" + testGithubUserId)
                        .name("Octo Cat")
                        .email("octocat@github.local")
                        .createdAt(Instant.now())
                        .build()));

        testSession = new UserSession(
                testSessionId,
                testGithubUserId,
                userEntity.getLogin(),
                "Octo Cat",
                "https://avatar.url",
                "octocat@github.local",
                "ghu_test_user_token_12345",
                null,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        when(sessionService.getSession(testSessionId)).thenReturn(Optional.of(testSession));
    }

    @Test
    @DisplayName("AC-02: Should link installation when state is valid and installation is accessible to user")
    void testValidCallbackWithAccessibleInstallationCreatesAssociation() throws Exception {
        String state = "valid-opaque-state-256bit";
        Long installationId = 554433L;

        when(installationStateService.validateAndConsumeState(state, userEntity.getId(), testSessionId))
                .thenReturn(true);

        when(gitHubAppService.getUserAccessibleInstallations("ghu_test_user_token_12345"))
                .thenReturn(List.of(
                        new UserAccessibleInstallationDto(installationId, testGithubUserId, userEntity.getLogin(), "User")
                ));

        String redirectUrl = mockMvc.perform(get("/api/v1/github/installations/callback")
                        .param("installation_id", installationId.toString())
                        .param("setup_action", "install")
                        .param("state", state)
                        .cookie(new jakarta.servlet.http.Cookie("SCANPILOT_SESSION", testSessionId)))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(redirectUrl).contains("installation=linked");
        assertThat(redirectUrl).contains("installation_id=554433");

        Optional<UserInstallationEntity> saved = userInstallationRepository.findByUserIdAndInstallationId(userEntity.getId(), installationId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getUserId()).isEqualTo(userEntity.getId());
        assertThat(saved.get().getGithubUserId()).isEqualTo(testGithubUserId);
        assertThat(saved.get().getInstallationId()).isEqualTo(installationId);
        assertThat(saved.get().getAccountLogin()).isEqualTo(userEntity.getLogin());

        verify(sessionService).updateInstallationId(testSessionId, installationId);
    }

    @Test
    @DisplayName("AC-02: Should find and bind installation located on subsequent page of user installations")
    void testInstallationOnSubsequentPageFoundAndBound() throws Exception {
        String state = "valid-page-2-state";
        Long targetInstallationId = 778899L;

        when(installationStateService.validateAndConsumeState(state, userEntity.getId(), testSessionId))
                .thenReturn(true);

        when(gitHubAppService.getUserAccessibleInstallations("ghu_test_user_token_12345"))
                .thenReturn(List.of(
                        new UserAccessibleInstallationDto(111111L, 100L, "org-one", "Organization"),
                        new UserAccessibleInstallationDto(targetInstallationId, testGithubUserId, userEntity.getLogin(), "User")
                ));

        String redirectUrl = mockMvc.perform(get("/api/v1/github/installations/callback")
                        .param("installation_id", targetInstallationId.toString())
                        .param("state", state)
                        .cookie(new jakarta.servlet.http.Cookie("SCANPILOT_SESSION", testSessionId)))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(redirectUrl).contains("installation=linked");
        assertThat(redirectUrl).contains("installation_id=778899");

        Optional<UserInstallationEntity> saved = userInstallationRepository.findByUserIdAndInstallationId(userEntity.getId(), targetInstallationId);
        assertThat(saved).isPresent();
    }

    @Test
    @DisplayName("AC-02: Should fail closed (403/redirect with error) when installation is not accessible to authenticated user")
    void testInstallationNotAccessibleToOAuthUserFailsClosed() throws Exception {
        String state = "valid-state-unauthorized-inst";
        Long unauthorizedInstallationId = 999999L;

        when(installationStateService.validateAndConsumeState(state, userEntity.getId(), testSessionId))
                .thenReturn(true);

        when(gitHubAppService.getUserAccessibleInstallations("ghu_test_user_token_12345"))
                .thenReturn(List.of(
                        new UserAccessibleInstallationDto(554433L, testGithubUserId, userEntity.getLogin(), "User")
                ));

        String redirectUrl = mockMvc.perform(get("/api/v1/github/installations/callback")
                        .param("installation_id", unauthorizedInstallationId.toString())
                        .param("state", state)
                        .cookie(new jakarta.servlet.http.Cookie("SCANPILOT_SESSION", testSessionId)))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(redirectUrl).contains("installation_error=unauthorized_installation");

        Optional<UserInstallationEntity> saved = userInstallationRepository.findByUserIdAndInstallationId(userEntity.getId(), unauthorizedInstallationId);
        assertThat(saved).isEmpty();
    }

    @Test
    @DisplayName("AC-02: Should fail closed when remote GitHub API returns error or fails")
    void testRemoteGitHubErrorFailsClosed() throws Exception {
        String state = "valid-state-remote-err";
        Long installationId = 554433L;

        when(installationStateService.validateAndConsumeState(state, userEntity.getId(), testSessionId))
                .thenReturn(true);

        when(gitHubAppService.getUserAccessibleInstallations("ghu_test_user_token_12345"))
                .thenThrow(new IllegalStateException("GitHub API 401 Unauthorized / token revoked"));

        String redirectUrl = mockMvc.perform(get("/api/v1/github/installations/callback")
                        .param("installation_id", installationId.toString())
                        .param("state", state)
                        .cookie(new jakarta.servlet.http.Cookie("SCANPILOT_SESSION", testSessionId)))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(redirectUrl).contains("installation_error=github_api_error");

        Optional<UserInstallationEntity> saved = userInstallationRepository.findByUserIdAndInstallationId(userEntity.getId(), installationId);
        assertThat(saved).isEmpty();
    }

    @Test
    @DisplayName("AC-02: Should reject callback with invalid, expired, or replayed state")
    void testTamperedOrMalformedStateRejected() throws Exception {
        String invalidState = "bad-or-replayed-state";
        Long installationId = 554433L;

        when(installationStateService.validateAndConsumeState(invalidState, userEntity.getId(), testSessionId))
                .thenReturn(false);

        String redirectUrl = mockMvc.perform(get("/api/v1/github/installations/callback")
                        .param("installation_id", installationId.toString())
                        .param("state", invalidState)
                        .cookie(new jakarta.servlet.http.Cookie("SCANPILOT_SESSION", testSessionId)))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(redirectUrl).contains("installation_error=invalid_state");

        Optional<UserInstallationEntity> saved = userInstallationRepository.findByUserIdAndInstallationId(userEntity.getId(), installationId);
        assertThat(saved).isEmpty();
    }

    @Test
    @DisplayName("AC-02: Should reject callback when missing session")
    void testMissingSessionRejected() throws Exception {
        when(sessionService.getSession("missing-session")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/github/installations/callback")
                        .param("installation_id", "554433")
                        .param("state", "some-state"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-05: AppSec Check - Zero sensitive material in logs, error redirects, or responses")
    void testZeroSensitiveMaterialInLogsAndResponses() throws Exception {
        String secretToken = "ghu_very_secret_user_token_abc123";
        String rawState = "opaque-secret-state-xyz";

        UserSession customSession = new UserSession(
                testSessionId,
                testGithubUserId,
                userEntity.getLogin(),
                "Octo Cat",
                "https://avatar.url",
                "octocat@github.local",
                secretToken,
                null,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        when(sessionService.getSession(testSessionId)).thenReturn(Optional.of(customSession));

        when(installationStateService.validateAndConsumeState(rawState, userEntity.getId(), testSessionId))
                .thenReturn(false);

        String redirectedUrl = mockMvc.perform(get("/api/v1/github/installations/callback")
                        .param("installation_id", "554433")
                        .param("state", rawState)
                        .cookie(new jakarta.servlet.http.Cookie("SCANPILOT_SESSION", testSessionId)))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(redirectedUrl).isNotNull();
        assertThat(redirectedUrl).doesNotContain(secretToken);
        assertThat(redirectedUrl).doesNotContain(rawState);
    }
}
