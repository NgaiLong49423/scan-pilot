package com.scanpilot.auth.interceptor;

import com.scanpilot.auth.annotation.CurrentUser;
import com.scanpilot.auth.annotation.RequireAuth;
import com.scanpilot.auth.dto.UserProfileDto;
import com.scanpilot.auth.model.UserSession;
import com.scanpilot.auth.service.SessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthInterceptorTest.TestProtectedController.class)
class AuthInterceptorTest {

    @RestController
    @RequestMapping("/api/v1/test-auth")
    static class TestProtectedController {

        @RequireAuth
        @GetMapping("/protected")
        public ResponseEntity<String> protectedEndpoint(@CurrentUser UserSession session) {
            return ResponseEntity.ok("Hello " + session.getLogin());
        }

        @GetMapping("/public")
        public ResponseEntity<String> publicEndpoint(@CurrentUser UserProfileDto profile) {
            if (profile != null) {
                return ResponseEntity.ok("Hello " + profile.login());
            }
            return ResponseEntity.ok("Hello Anonymous");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService.clearAllSessions();
    }

    @Test
    @DisplayName("Endpoint with @RequireAuth returns 401 when unauthenticated")
    void testRequireAuthUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/test-auth/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Endpoint with @RequireAuth succeeds when valid session cookie provided")
    void testRequireAuthSuccess() throws Exception {
        UserSession session = sessionService.createSession(
                100L,
                "alice",
                "Alice Dev",
                null,
                "alice@example.com",
                "tok"
        );

        mockMvc.perform(get("/api/v1/test-auth/protected")
                        .cookie(new Cookie("SCANPILOT_SESSION", session.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello alice"));
    }

    @Test
    @DisplayName("Public endpoint injects null profile when unauthenticated and returns 200")
    void testPublicEndpointAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/test-auth/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello Anonymous"));
    }

    @Test
    @DisplayName("Public endpoint injects UserProfileDto when session cookie present")
    void testPublicEndpointAuthenticated() throws Exception {
        UserSession session = sessionService.createSession(
                101L,
                "bob",
                "Bob Builder",
                null,
                "bob@example.com",
                "tok"
        );

        mockMvc.perform(get("/api/v1/test-auth/public")
                        .cookie(new Cookie("SCANPILOT_SESSION", session.getSessionId())))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello bob"));
    }
}
