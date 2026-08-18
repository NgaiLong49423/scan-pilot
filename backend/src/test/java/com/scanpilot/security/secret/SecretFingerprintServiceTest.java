package com.scanpilot.security.secret;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretFingerprintServiceTest {

    private static final String TEST_HMAC_KEY = "test-insecure-hmac-key-for-unit-tests-32-chars-long";
    private static final Pattern HEX_64_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private SecurityConfigProperties properties;
    private SecretFingerprintService fingerprintService;

    @BeforeEach
    void setUp() {
        properties = new SecurityConfigProperties();
        properties.setHmacSecretKey(TEST_HMAC_KEY);
        fingerprintService = new SecretFingerprintService(properties);
    }

    @Nested
    @DisplayName("Fingerprint Determinism & Format")
    class DeterminismAndFormatTests {

        @Test
        @DisplayName("Same inputs produce identical 64-character lowercase hex output")
        void shouldProduceDeterministicHexOutput() {
            String repoId = "repo-uuid-101";
            String ruleId = "SP-CONFIG-001";
            String rawSecret = "AIzaSyDummyGoogleApiKey1234567890";

            String fp1 = fingerprintService.computeFingerprint(repoId, ruleId, rawSecret);
            String fp2 = fingerprintService.computeFingerprint(repoId, ruleId, rawSecret);

            assertThat(fp1).isNotNull();
            assertThat(fp1).isEqualTo(fp2);
            assertThat(fp1).hasSize(64);
            assertThat(fp1).matches(HEX_64_PATTERN);
        }

        @Test
        @DisplayName("Secret string and identical secret byte array produce same fingerprint")
        void shouldMatchBetweenStringAndBytesOverload() {
            String repoId = "repo-uuid-101";
            String ruleId = "SP-CONFIG-001";
            String rawSecret = "AIzaSyDummyGoogleApiKey1234567890";

            String fpString = fingerprintService.computeFingerprint(repoId, ruleId, rawSecret);
            String fpBytes = fingerprintService.computeFingerprint(repoId, ruleId, rawSecret.getBytes(StandardCharsets.UTF_8));

            assertThat(fpString).isEqualTo(fpBytes);
        }

        @Test
        @DisplayName("Computes short 8-char fingerprint prefix")
        void shouldComputeShortFingerprint() {
            String repoId = "repo-uuid-101";
            String ruleId = "SP-CONFIG-001";
            String rawSecret = "AIzaSyDummyGoogleApiKey1234567890";

            String fp = fingerprintService.computeFingerprint(repoId, ruleId, rawSecret);
            String shortFp = fingerprintService.shortFingerprint(fp);

            assertThat(shortFp).hasSize(8);
            assertThat(shortFp).isEqualTo(fp.substring(0, 8));
        }

        @Test
        @DisplayName("Handles short fingerprint edge cases gracefully")
        void shouldHandleShortFingerprintEdgeCases() {
            assertThat(fingerprintService.shortFingerprint(null)).isEmpty();
            assertThat(fingerprintService.shortFingerprint("")).isEmpty();
            assertThat(fingerprintService.shortFingerprint("1234")).isEqualTo("1234");
            assertThat(fingerprintService.shortFingerprint("12345678")).isEqualTo("12345678");
            assertThat(fingerprintService.shortFingerprint("123456789")).isEqualTo("12345678");
        }
    }

    @Nested
    @DisplayName("Isolation Guarantees")
    class IsolationTests {

        @Test
        @DisplayName("Repository isolation: same rule and secret in different repos yield different fingerprints")
        void shouldIsolateByRepository() {
            String ruleId = "SP-CONFIG-001";
            String secret = "AIzaSyDummyGoogleApiKey1234567890";

            String fpRepoA = fingerprintService.computeFingerprint("repo-A", ruleId, secret);
            String fpRepoB = fingerprintService.computeFingerprint("repo-B", ruleId, secret);

            assertThat(fpRepoA).isNotEqualTo(fpRepoB);
        }

        @Test
        @DisplayName("Rule isolation: same repo and secret under different rules yield different fingerprints")
        void shouldIsolateByRule() {
            String repoId = "repo-uuid-101";
            String secret = "AIzaSyDummyGoogleApiKey1234567890";

            String fpRule1 = fingerprintService.computeFingerprint(repoId, "SP-CONFIG-001", secret);
            String fpRule2 = fingerprintService.computeFingerprint(repoId, "SP-CONFIG-002", secret);

            assertThat(fpRule1).isNotEqualTo(fpRule2);
        }

        @Test
        @DisplayName("Key isolation: different HMAC keys yield different fingerprints")
        void shouldIsolateByHmacKey() {
            String repoId = "repo-uuid-101";
            String ruleId = "SP-CONFIG-001";
            String secret = "AIzaSyDummyGoogleApiKey1234567890";

            String fp1 = fingerprintService.computeFingerprint(repoId, ruleId, secret);

            SecurityConfigProperties props2 = new SecurityConfigProperties();
            props2.setHmacSecretKey("different-insecure-hmac-key-for-testing-only-32");
            SecretFingerprintService service2 = new SecretFingerprintService(props2);

            String fp2 = service2.computeFingerprint(repoId, ruleId, secret);

            assertThat(fp1).isNotEqualTo(fp2);
        }
    }

    @Nested
    @DisplayName("Length-Prefixed Collision Prevention (REC-03)")
    class CollisionPreventionTests {

        @Test
        @DisplayName("Prevents boundary shifting collision between ruleId, secretLength, and secretBytes")
        void shouldPreventLengthPrefixCollisions() {
            String repoId = "repo-1";

            // Case 1: secret is "test" (length 4)
            String fp1 = fingerprintService.computeFingerprint(repoId, "RULE", "test");

            // Case 2: secret is "4:test" (length 6) under empty or shifted rule
            String fp2 = fingerprintService.computeFingerprint(repoId, "RULE", "4:test");

            // Case 3: shifted rule and secret lengths
            String fp3 = fingerprintService.computeFingerprint(repoId, "RULE|4", "test");

            assertThat(fp1).isNotEqualTo(fp2);
            assertThat(fp1).isNotEqualTo(fp3);
            assertThat(fp2).isNotEqualTo(fp3);
        }
    }

    @Nested
    @DisplayName("Null & Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Null arguments handled gracefully without throwing NullPointerException")
        void shouldHandleNullInputsGracefully() {
            String fp1 = fingerprintService.computeFingerprint(null, null, (String) null);
            String fp2 = fingerprintService.computeFingerprint(null, null, (byte[]) null);

            assertThat(fp1).isNotNull().hasSize(64).matches(HEX_64_PATTERN);
            assertThat(fp2).isEqualTo(fp1);
        }

        @Test
        @DisplayName("Throws IllegalStateException when HMAC key is missing or blank")
        void shouldThrowWhenKeyIsMissingOrBlank() {
            properties.setHmacSecretKey(null);
            assertThatThrownBy(() -> fingerprintService.computeFingerprint("repo", "rule", "secret"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HMAC secret key is not configured");

            properties.setHmacSecretKey("   ");
            assertThatThrownBy(() -> fingerprintService.computeFingerprint("repo", "rule", "secret"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HMAC secret key is not configured");
        }
    }
}
