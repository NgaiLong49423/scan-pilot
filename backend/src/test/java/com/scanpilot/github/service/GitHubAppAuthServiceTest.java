package com.scanpilot.github.service;

import com.scanpilot.github.config.GitHubAppConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubAppAuthServiceTest {

    private GitHubAppConfigProperties properties;
    private GitHubAppAuthService authService;
    private MockRestServiceServer mockServer;
    private KeyPair testKeyPair;
    private String testPrivateKeyPem;

    @BeforeEach
    void setUp() throws Exception {
        properties = new GitHubAppConfigProperties();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        authService = new GitHubAppAuthService(properties, builder);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        testKeyPair = keyGen.generateKeyPair();

        String base64Key = Base64.getEncoder().encodeToString(testKeyPair.getPrivate().getEncoded());
        testPrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + base64Key + "\n-----END PRIVATE KEY-----";
    }

    @Test
    @DisplayName("isConfigured returns true only when appId and appPrivateKey are present")
    void testIsConfigured() {
        assertThat(authService.isConfigured()).isFalse();

        properties.setAppId("123456");
        assertThat(authService.isConfigured()).isFalse();

        properties.setAppPrivateKey(testPrivateKeyPem);
        assertThat(authService.isConfigured()).isTrue();
    }

    @Test
    @DisplayName("generateAppJwt produces valid RS256 JWT verifiable with RSA public key")
    void testGenerateAppJwt() throws Exception {
        properties.setAppId("654321");
        properties.setAppPrivateKey(testPrivateKeyPem);

        String jwt = authService.generateAppJwt();
        assertThat(jwt).isNotNull();

        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        assertThat(headerJson).contains("\"alg\":\"RS256\"");
        assertThat(headerJson).contains("\"typ\":\"JWT\"");
        assertThat(payloadJson).contains("\"iss\":\"654321\"");
        assertThat(payloadJson).contains("\"iat\":");
        assertThat(payloadJson).contains("\"exp\":");

        // Verify cryptographic signature with public key
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(testKeyPair.getPublic());
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        boolean verified = signature.verify(signatureBytes);

        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("generateAppJwt throws IllegalStateException when unconfigured")
    void testGenerateAppJwtUnconfigured() {
        assertThatThrownBy(() -> authService.generateAppJwt())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GitHub App is not configured");
    }

    @Test
    @DisplayName("createInstallationAccessToken calls GitHub API and returns access token")
    void testCreateInstallationAccessToken() {
        properties.setAppId("123456");
        properties.setAppPrivateKey(testPrivateKeyPem);

        mockServer.expect(requestTo("https://api.github.com/app/installations/98765/access_tokens"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Bearer ")))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(withSuccess("{\"token\":\"ghs_installation_token_123\",\"expires_at\":\"2026-08-18T18:00:00Z\"}", MediaType.APPLICATION_JSON));

        String token = authService.createInstallationAccessToken(98765L);
        assertThat(token).isEqualTo("ghs_installation_token_123");
        mockServer.verify();
    }

    @Test
    @DisplayName("createInstallationAccessToken with null ID throws IllegalArgumentException")
    void testCreateInstallationAccessTokenNullId() {
        assertThatThrownBy(() -> authService.createInstallationAccessToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
