package com.scanpilot.github.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebhookSignatureValidator Unit Tests")
class WebhookSignatureValidatorTest {

    private WebhookSignatureValidator validator;
    private final String secret = "super-secret-key-12345";

    @BeforeEach
    void setUp() {
        validator = new WebhookSignatureValidator();
    }

    private String computeHmacHex(byte[] payload, String secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
    }

    @Test
    @DisplayName("Should return true for valid HMAC-SHA256 signature")
    void testValidHmacSha256SignaturePasses() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = computeHmacHex(payload, secret);

        boolean result = validator.validateSignature(payload, signature, secret);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for tampered payload in constant time")
    void testInvalidSignatureFailsConstantTime() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedPayload = "{\"action\":\"closed\"}".getBytes(StandardCharsets.UTF_8);
        String signature = computeHmacHex(payload, secret);

        boolean result = validator.validateSignature(tamperedPayload, signature, secret);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for invalid signature hash")
    void testTamperedSignatureHashFails() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = computeHmacHex(payload, secret) + "tampered";

        boolean result = validator.validateSignature(payload, signature, secret);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for missing or malformed signature header")
    void testMissingOrMalformedSignatureHeaderFails() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.validateSignature(payload, null, secret)).isFalse();
        assertThat(validator.validateSignature(payload, "", secret)).isFalse();
        assertThat(validator.validateSignature(payload, "invalid-header-without-prefix", secret)).isFalse();
        assertThat(validator.validateSignature(payload, "md5=abcdef123456", secret)).isFalse();
    }

    @Test
    @DisplayName("Should return false when webhook secret is null or unconfigured")
    void testUnconfiguredSecretFails() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.validateSignature(payload, "sha256=1234", null)).isFalse();
        assertThat(validator.validateSignature(payload, "sha256=1234", "")).isFalse();
        assertThat(validator.validateSignature(payload, "sha256=1234", "   ")).isFalse();
    }
}
