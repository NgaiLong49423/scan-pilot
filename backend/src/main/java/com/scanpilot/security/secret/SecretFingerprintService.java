package com.scanpilot.security.secret;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes deterministic, repository-scoped HMAC-SHA-256 fingerprints (SP_SECRET_FP_V1)
 * for detected secret credentials without persisting or leaking raw secret material.
 * <p>
 * Canonical format (REC-03):
 * {@code v1|{repositoryId}|{ruleId}|{secretLength}:{secretBytes}}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecretFingerprintService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SCHEME_VERSION = "v1";

    private final SecurityConfigProperties securityConfigProperties;

    /**
     * Computes a 64-character lowercase hex HMAC-SHA-256 fingerprint for a secret string.
     *
     * @param repositoryId stable internal repository identifier
     * @param ruleId       detection rule identifier (e.g., SP-CONFIG-001)
     * @param rawSecret    raw detected secret string
     * @return 64-character lowercase hex fingerprint
     */
    public String computeFingerprint(String repositoryId, String ruleId, String rawSecret) {
        byte[] secretBytes = (rawSecret != null) ? rawSecret.getBytes(StandardCharsets.UTF_8) : new byte[0];
        return computeFingerprint(repositoryId, ruleId, secretBytes);
    }

    /**
     * Computes a 64-character lowercase hex HMAC-SHA-256 fingerprint for raw secret bytes.
     *
     * @param repositoryId stable internal repository identifier
     * @param ruleId       detection rule identifier (e.g., SP-CONFIG-001)
     * @param secretBytes  raw detected secret bytes
     * @return 64-character lowercase hex fingerprint
     */
    public String computeFingerprint(String repositoryId, String ruleId, byte[] secretBytes) {
        String safeRepoId = (repositoryId != null) ? repositoryId : "";
        String safeRuleId = (ruleId != null) ? ruleId : "";
        byte[] safeSecretBytes = (secretBytes != null) ? secretBytes : new byte[0];

        // Format: v1|{repositoryId}|{ruleId}|{secretLength}:{secretBytes}
        String prefix = SCHEME_VERSION + "|" + safeRepoId + "|" + safeRuleId + "|" + safeSecretBytes.length + ":";
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);

        byte[] canonicalBytes = new byte[prefixBytes.length + safeSecretBytes.length];
        System.arraycopy(prefixBytes, 0, canonicalBytes, 0, prefixBytes.length);
        System.arraycopy(safeSecretBytes, 0, canonicalBytes, prefixBytes.length, safeSecretBytes.length);

        return computeHmacHex(canonicalBytes);
    }

    /**
     * Returns the short 8-character prefix of a fingerprint for display and correlation logs.
     *
     * @param fingerprint 64-character fingerprint
     * @return 8-character prefix, or empty string if input is null
     */
    public String shortFingerprint(String fingerprint) {
        if (fingerprint == null) {
            return "";
        }
        if (fingerprint.length() <= 8) {
            return fingerprint;
        }
        return fingerprint.substring(0, 8);
    }

    private String computeHmacHex(byte[] inputBytes) {
        String key = securityConfigProperties.getHmacSecretKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("HMAC secret key is not configured for fingerprint generation");
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(inputBytes);
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HMAC-SHA256 algorithm not available in current JVM", e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid HMAC key specification", e);
        }
    }
}
