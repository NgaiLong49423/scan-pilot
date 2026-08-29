package com.scanpilot.scanner.remediation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.security.secret.SecurityConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FindingRemediationPrTokenServiceTest {

    private FindingRemediationPrTokenService tokenService;
    private SecurityConfigProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SecurityConfigProperties();
        properties.setHmacSecretKey("test-secret-key-32-bytes-minimum-length-for-hmac");
        tokenService = new FindingRemediationPrTokenService(properties, new ObjectMapper());
    }

    @Test
    @DisplayName("Generates and validates token with matching claims")
    void testGenerateAndValidate() {
        UUID findingId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        String sha = "abcdef1234567890abcdef1234567890abcdef12";
        String patchPlanHash = tokenService.computePatchPlanHash(findingId, sha, "application.properties", 3, "key=${VAL}");

        String token = tokenService.generateToken(findingId, repoId, sha, patchPlanHash);
        assertThat(token).isNotBlank();

        FindingRemediationPrTokenService.VerifiedRemediationToken verified =
                tokenService.validateToken(token, findingId, sha, patchPlanHash);

        assertThat(verified.findingId()).isEqualTo(findingId);
        assertThat(verified.repositoryId()).isEqualTo(repoId);
        assertThat(verified.targetCommitSha()).isEqualTo(sha);
        assertThat(verified.patchPlanHash()).isEqualTo(patchPlanHash);
    }

    @Test
    @DisplayName("Rejects token with tampered signature")
    void testTamperedSignature() {
        UUID findingId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        String sha = "abcdef1234567890abcdef1234567890abcdef12";
        String patchPlanHash = "fakehash";

        String token = tokenService.generateToken(findingId, repoId, sha, patchPlanHash);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThatThrownBy(() -> tokenService.validateToken(tamperedToken, findingId, sha, patchPlanHash))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
    }

    @Test
    @DisplayName("Rejects token when finding ID does not match expected")
    void testFindingIdMismatch() {
        UUID findingId = UUID.randomUUID();
        UUID anotherFindingId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        String sha = "abcdef1234567890abcdef1234567890abcdef12";
        String patchPlanHash = "hash123";

        String token = tokenService.generateToken(findingId, repoId, sha, patchPlanHash);

        assertThatThrownBy(() -> tokenService.validateToken(token, anotherFindingId, sha, patchPlanHash))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
    }
}