package com.scanpilot.scanner.remediation;

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
 * Service for issuing and validating HMAC-SHA256 signed Remediation PR preview tokens with 15-minute TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FindingRemediationPrTokenService {

    public static final String TOKEN_VERSION = "v1";
    public static final String DOMAIN_PURPOSE = "finding-remediation-pr-preview:v1";
    public static final long TOKEN_TTL_SECONDS = 900; // 15 minutes

    private final SecurityConfigProperties securityConfigProperties;
    private final ObjectMapper objectMapper;

    public record VerifiedRemediationToken(
        UUID findingId,
        UUID repositoryId,
        String targetCommitSha,
        String patchPlanHash,
        long issuedAtEpochSec,
        long expiresAtEpochSec
    ) {}

    /**
     * Derives a domain-separated HMAC key for remediation preview tokens.
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
     * Computes the SHA-256 hex string of a canonical patch plan.
     */
    public String computePatchPlanHash(UUID findingId, String targetCommitSha, String filePath, int lineNumber, String patchedLine) {
        try {
            String canonical = String.format("%s:%s:%s:%d:%s",
                findingId != null ? findingId.toString() : "",
                targetCommitSha != null ? targetCommitSha.trim() : "",
                filePath != null ? filePath.trim() : "",
                lineNumber,
                patchedLine != null ? patchedLine.trim() : ""
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute patch plan hash", e);
        }
    }

    /**
     * Generates a signed previewToken with 15-minute TTL.
     */
    public String generateToken(UUID findingId, UUID repositoryId, String targetCommitSha, String patchPlanHash) {
        if (findingId == null || repositoryId == null || targetCommitSha == null || patchPlanHash == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        long nowSec = Instant.now().getEpochSecond();
        long expSec = nowSec + TOKEN_TTL_SECONDS;

        try {
            String payloadJson = String.format(
                "{\"v\":\"%s\",\"fid\":\"%s\",\"rid\":\"%s\",\"sha\":\"%s\",\"ph\":\"%s\",\"iat\":%d,\"exp\":%d}",
                TOKEN_VERSION,
                findingId.toString(),
                repositoryId.toString(),
                targetCommitSha.trim(),
                patchPlanHash.trim(),
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
     * Validates a signed preview token against findingId, targetCommitSha, and patchPlanHash.
     */
    public VerifiedRemediationToken validateToken(
        String tokenString,
        UUID expectedFindingId,
        String expectedTargetCommitSha,
        String expectedPatchPlanHash
    ) {
        if (tokenString == null || tokenString.isBlank()) {
            throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
        }

        String[] parts = tokenString.trim().split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
        }

        String payloadEncoded = parts[0];
        String signatureEncoded = parts[1];

        byte[] expectedSig = computeHmac(payloadEncoded.getBytes(StandardCharsets.UTF_8));
        byte[] actualSig;
        try {
            actualSig = Base64.getUrlDecoder().decode(signatureEncoded);
        } catch (Exception e) {
            throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
        }

        if (!MessageDigest.isEqual(expectedSig, actualSig)) {
            log.warn("Invalid signature on preview token");
            throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadEncoded);
            JsonNode root = objectMapper.readTree(payloadBytes);

            String v = root.path("v").asText("");
            if (!TOKEN_VERSION.equals(v)) {
                throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
            }

            long exp = root.path("exp").asLong(0);
            long nowSec = Instant.now().getEpochSecond();
            if (exp < nowSec) {
                log.warn("Preview token expired at {}, current time is {}", exp, nowSec);
                throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
            }

            UUID fid = UUID.fromString(root.path("fid").asText());
            UUID rid = UUID.fromString(root.path("rid").asText());
            String sha = root.path("sha").asText();
            String ph = root.path("ph").asText();
            long iat = root.path("iat").asLong(0);

            if (expectedFindingId != null && !expectedFindingId.equals(fid)) {
                log.warn("Token findingId {} does not match expected {}", fid, expectedFindingId);
                throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
            }

            if (expectedTargetCommitSha != null && !expectedTargetCommitSha.equalsIgnoreCase(sha)) {
                log.warn("Token commitSha {} does not match expected {}", sha, expectedTargetCommitSha);
                throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
            }

            if (expectedPatchPlanHash != null && !expectedPatchPlanHash.equalsIgnoreCase(ph)) {
                log.warn("Token patchPlanHash {} does not match expected {}", ph, expectedPatchPlanHash);
                throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
            }

            return new VerifiedRemediationToken(fid, rid, sha, ph, iat, exp);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse preview token payload", e);
            throw new IllegalArgumentException("PREVIEW_TOKEN_EXPIRED_OR_INVALID");
        }
    }

    private byte[] computeHmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(deriveDomainKey(), "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }
}