package com.scanpilot.scanner.issue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.security.secret.SecurityConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for issuing and validating HMAC-SHA256 signed preview tokens with 15-minute TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindingIssueTokenService {

    public static final String TOKEN_VERSION = "v1";
    public static final String DOMAIN_PURPOSE = "finding-issue-preview:v1";
    public static final long TOKEN_TTL_SECONDS = 900; // 15 minutes

    private final SecurityConfigProperties securityConfigProperties;
    private final ObjectMapper objectMapper;

    /**
     * Derives a domain-separated HMAC key for preview tokens.
     */
    private byte[] deriveDomainKey() {
        try {
            String secretKey = securityConfigProperties.getHmacSecretKey();
            if (secretKey == null || secretKey.isBlank()) {
                secretKey = "default-insecure-dev-hmac-key-for-local-testing-only-32bytes";
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(DOMAIN_PURPOSE.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive domain key for preview tokens", e);
        }
    }

    /**
     * Computes the SHA-256 hex string of a canonical draft markdown.
     */
    public String computeDraftSha256(String canonicalDraft) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((canonicalDraft != null ? canonicalDraft : "").getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute draft hash", e);
        }
    }

    /**
     * Generates a signed previewToken with 15-minute TTL.
     */
    public String generateToken(UUID findingId, long findingRevisionMillis, String draftSha256Hex) {
        if (findingId == null) {
            throw new IllegalArgumentException("findingId cannot be null");
        }
        if (draftSha256Hex == null || draftSha256Hex.isBlank()) {
            throw new IllegalArgumentException("draftSha256Hex cannot be null or blank");
        }

        long nowSec = Instant.now().getEpochSecond();
        long expSec = nowSec + TOKEN_TTL_SECONDS;

        try {
            String payloadJson = String.format(
                "{\"v\":\"%s\",\"fid\":\"%s\",\"rev\":%d,\"dh\":\"%s\",\"iat\":%d,\"exp\":%d}",
                TOKEN_VERSION,
                findingId.toString(),
                findingRevisionMillis,
                draftSha256Hex.trim(),
                nowSec,
                expSec
            );

            String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = computeHmac(payloadEncoded.getBytes(StandardCharsets.UTF_8));
            String signatureEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            return payloadEncoded + "." + signatureEncoded;
        } catch (Exception e) {
            log.error("Failed to generate preview token", e);
            throw new IllegalStateException("Failed to generate preview token: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a signed previewToken. Throws IllegalStateException if invalid, expired, or tampered.
     */
    public void validateToken(
        String token,
        UUID expectedFindingId,
        long currentFindingRevisionMillis,
        String expectedDraftSha256Hex
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Preview token must not be null or blank");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new IllegalStateException("Invalid preview token format");
        }

        String payloadEncoded = parts[0];
        String signatureEncoded = parts[1];

        // 1. Verify signature in constant time
        byte[] expectedSig = computeHmac(payloadEncoded.getBytes(StandardCharsets.UTF_8));
        byte[] actualSig;
        try {
            actualSig = Base64.getUrlDecoder().decode(signatureEncoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Malformed signature encoding in preview token", e);
        }

        if (!MessageDigest.isEqual(expectedSig, actualSig)) {
            throw new IllegalStateException("Invalid preview token signature");
        }

        // 2. Parse payload
        JsonNode node;
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadEncoded);
            node = objectMapper.readTree(payloadBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Malformed payload in preview token", e);
        }

        String version = node.path("v").asText();
        if (!TOKEN_VERSION.equals(version)) {
            throw new IllegalStateException("Unsupported preview token version: " + version);
        }

        String fid = node.path("fid").asText();
        if (!expectedFindingId.toString().equalsIgnoreCase(fid)) {
            throw new IllegalStateException("Preview token findingId mismatch");
        }

        long exp = node.path("exp").asLong();
        long nowSec = Instant.now().getEpochSecond();
        // Exact boundary condition: expired if now >= exp (valid strictly when now < exp)
        if (nowSec >= exp) {
            throw new IllegalStateException("Preview token has expired");
        }

        long rev = node.path("rev").asLong();
        if (rev != currentFindingRevisionMillis) {
            throw new IllegalStateException("Finding has changed since preview was generated (stale token)");
        }

        String dh = node.path("dh").asText();
        if (!MessageDigest.isEqual(dh.getBytes(StandardCharsets.UTF_8), expectedDraftSha256Hex.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalStateException("Issue draft content does not match preview token hash");
        }
    }

    private byte[] computeHmac(byte[] data) {
        try {
            byte[] domainKey = deriveDomainKey();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(domainKey, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }
}
