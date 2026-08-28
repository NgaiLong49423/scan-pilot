package com.scanpilot.scanner.issue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.security.secret.SecurityConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Finding Issue Token Service Tests")
class FindingIssueTokenServiceTest {

    private FindingIssueTokenService tokenService;
    private SecurityConfigProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new SecurityConfigProperties();
        properties.setHmacSecretKey("test-secret-key-for-token-signing-32bytes");
        objectMapper = new ObjectMapper();
        tokenService = new FindingIssueTokenService(properties, objectMapper);
    }

    @Test
    @DisplayName("GIVEN valid finding and draft WHEN generating and validating token THEN succeeds")
    void testValidTokenGenerationAndValidation() {
        UUID findingId = UUID.randomUUID();
        long revision = Instant.now().toEpochMilli();
        String draft = "### Canonical Draft Body";
        String draftHash = tokenService.computeDraftSha256(draft);

        String token = tokenService.generateToken(findingId, revision, draftHash);
        assertThat(token).isNotNull();
        assertThat(token).contains(".");

        assertDoesNotThrow(() -> tokenService.validateToken(token, findingId, revision, draftHash));
    }

    @Test
    @DisplayName("GIVEN expired token (now > exp) WHEN validating THEN throws IllegalStateException")
    void testExpiredTokenThrows() {
        UUID findingId = UUID.randomUUID();
        long revision = Instant.now().toEpochMilli();
        String draftHash = tokenService.computeDraftSha256("draft");

        // Construct expired token payload (iat: now - 1000, exp: now - 100)
        long now = Instant.now().getEpochSecond();
        String expiredPayload = String.format(
                "{\"v\":\"%s\",\"fid\":\"%s\",\"rev\":%d,\"dh\":\"%s\",\"iat\":%d,\"exp\":%d}",
                FindingIssueTokenService.TOKEN_VERSION,
                findingId.toString(),
                revision,
                draftHash,
                now - 1000,
                now - 100
        );

        String token = signPayload(expiredPayload);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                tokenService.validateToken(token, findingId, revision, draftHash)
        );
        assertThat(ex.getMessage()).contains("expired");
    }

    @Test
    @DisplayName("GIVEN exact-boundary expired token (now == exp) WHEN validating THEN throws IllegalStateException (exact boundary condition)")
    void testExactBoundaryExpiryThrows() {
        UUID findingId = UUID.randomUUID();
        long revision = Instant.now().toEpochMilli();
        String draftHash = tokenService.computeDraftSha256("draft");

        long now = Instant.now().getEpochSecond();
        // exp set exactly to now (now >= exp is rejected)
        String exactExpiryPayload = String.format(
                "{\"v\":\"%s\",\"fid\":\"%s\",\"rev\":%d,\"dh\":\"%s\",\"iat\":%d,\"exp\":%d}",
                FindingIssueTokenService.TOKEN_VERSION,
                findingId.toString(),
                revision,
                draftHash,
                now - 900,
                now
        );

        String token = signPayload(exactExpiryPayload);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                tokenService.validateToken(token, findingId, revision, draftHash)
        );
        assertThat(ex.getMessage()).contains("expired");
    }

    @Test
    @DisplayName("GIVEN tampered token signature WHEN validating THEN throws IllegalStateException")
    void testTamperedSignatureThrows() {
        UUID findingId = UUID.randomUUID();
        long revision = Instant.now().toEpochMilli();
        String draftHash = tokenService.computeDraftSha256("draft");

        String token = tokenService.generateToken(findingId, revision, draftHash);
        String tamperedToken = token.substring(0, token.length() - 4) + "XXXX";

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                tokenService.validateToken(tamperedToken, findingId, revision, draftHash)
        );
        assertThat(ex.getMessage()).contains("signature");
    }

    @Test
    @DisplayName("GIVEN updated finding revision (stale preview) WHEN validating THEN throws IllegalStateException")
    void testStaleRevisionThrows() {
        UUID findingId = UUID.randomUUID();
        long oldRevision = Instant.now().toEpochMilli() - 5000;
        long newRevision = Instant.now().toEpochMilli();
        String draftHash = tokenService.computeDraftSha256("draft");

        String token = tokenService.generateToken(findingId, oldRevision, draftHash);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                tokenService.validateToken(token, findingId, newRevision, draftHash)
        );
        assertThat(ex.getMessage()).contains("stale token");
    }

    @Test
    @DisplayName("GIVEN modified draft content (tampered draft) WHEN validating THEN throws IllegalStateException")
    void testModifiedDraftHashThrows() {
        UUID findingId = UUID.randomUUID();
        long revision = Instant.now().toEpochMilli();
        String originalDraftHash = tokenService.computeDraftSha256("original");
        String modifiedDraftHash = tokenService.computeDraftSha256("modified");

        String token = tokenService.generateToken(findingId, revision, originalDraftHash);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                tokenService.validateToken(token, findingId, revision, modifiedDraftHash)
        );
        assertThat(ex.getMessage()).contains("does not match preview token hash");
    }

    private String signPayload(String payloadJson) {
        String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(properties.getHmacSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] domainKey = mac.doFinal(FindingIssueTokenService.DOMAIN_PURPOSE.getBytes(StandardCharsets.UTF_8));

            javax.crypto.Mac signMac = javax.crypto.Mac.getInstance("HmacSHA256");
            signMac.init(new javax.crypto.spec.SecretKeySpec(domainKey, "HmacSHA256"));
            byte[] sigBytes = signMac.doFinal(payloadEncoded.getBytes(StandardCharsets.UTF_8));
            String sigEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);
            return payloadEncoded + "." + sigEncoded;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
