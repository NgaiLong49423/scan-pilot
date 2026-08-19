package com.scanpilot.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfiguration corsConfiguration;

    @Autowired
    private CorsProperties corsProperties;

    @Test
    @DisplayName("CorsConfiguration bean contains configured properties")
    void testCorsConfigurationBeanProperties() {
        assertThat(corsProperties.getAllowedOrigins()).contains(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://aistudio.google.com"
        );
        assertThat(corsConfiguration.getAllowedOriginPatterns()).contains(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://aistudio.google.com"
        );
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
        assertThat(corsConfiguration.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD");
        assertThat(corsConfiguration.getExposedHeaders()).contains("Set-Cookie");
    }

    @Test
    @DisplayName("GET request with Origin: https://aistudio.google.com receives CORS headers")
    void testCorsWithAiStudioOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/system/status")
                        .header(HttpHeaders.ORIGIN, "https://aistudio.google.com"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://aistudio.google.com"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("GET request with Origin: http://localhost:3000 receives CORS headers")
    void testCorsWithLocalhost3000Origin() throws Exception {
        mockMvc.perform(get("/api/v1/system/status")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("GET request with Origin: http://localhost:5173 receives CORS headers")
    void testCorsWithLocalhost5173Origin() throws Exception {
        mockMvc.perform(get("/api/v1/system/status")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("Preflight OPTIONS request from allowed origin receives valid preflight response")
    void testPreflightOptionsRequest() throws Exception {
        mockMvc.perform(options("/api/v1/system/status")
                        .header(HttpHeaders.ORIGIN, "https://aistudio.google.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://aistudio.google.com"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    @DisplayName("Request from disallowed origin does not receive Access-Control-Allow-Origin")
    void testDisallowedOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/system/status")
                        .header(HttpHeaders.ORIGIN, "https://evil-attacker-site.com"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
