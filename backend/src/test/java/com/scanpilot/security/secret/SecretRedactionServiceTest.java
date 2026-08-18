package com.scanpilot.security.secret;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretRedactionServiceTest {

    private static final String TEST_HMAC_KEY = "test-insecure-hmac-key-for-unit-tests-32-chars-long";

    private SecretFingerprintService fingerprintService;
    private SecretRedactionService redactionService;

    @BeforeEach
    void setUp() {
        SecurityConfigProperties properties = new SecurityConfigProperties();
        properties.setHmacSecretKey(TEST_HMAC_KEY);
        fingerprintService = new SecretFingerprintService(properties);
        redactionService = new SecretRedactionService(fingerprintService);
    }

    @Nested
    @DisplayName("Mask Secret Rules")
    class MaskSecretTests {

        @Test
        @DisplayName("Google API Key: preserves 'AIzaSy' (6 chars), last 4, masks middle")
        void shouldMaskGoogleApiKey() {
            String googleKey = "AIzaSyDb-1234567890abcdef1234567890"; // 36 chars
            String masked = redactionService.maskSecret(googleKey, "SP-CONFIG-001");

            assertThat(masked).startsWith("AIzaSy");
            assertThat(masked).endsWith("7890");
            assertThat(masked).hasSize(googleKey.length());
            assertThat(masked).isEqualTo("AIzaSy" + "*".repeat(googleKey.length() - 10) + "7890");
            assertThat(masked).doesNotContain("1234567890abcdef");
        }

        @Test
        @DisplayName("GitHub Personal Access Token (classic): preserves 'ghp_', last 4, masks middle")
        void shouldMaskGitHubClassicToken() {
            String ghpToken = "ghp_1234567890abcdef1234"; // 25 chars
            String masked = redactionService.maskSecret(ghpToken, "GITHUB_TOKEN");

            assertThat(masked).startsWith("ghp_");
            assertThat(masked).endsWith("1234");
            assertThat(masked).hasSize(ghpToken.length());
            assertThat(masked).isEqualTo("ghp_" + "*".repeat(ghpToken.length() - 8) + "1234");
            assertThat(masked).doesNotContain("1234567890abcdef");
        }

        @Test
        @DisplayName("GitHub OAuth / App Tokens: preserves 'gho_', 'ghs_', 'ghu_', 'ghr_', last 4, masks middle")
        void shouldMaskGitHubOtherTokens() {
            String ghoToken = "gho_abcdef12345678901234";
            String ghsToken = "ghs_abcdef12345678901234";

            assertThat(redactionService.maskSecret(ghoToken, "GITHUB_TOKEN")).startsWith("gho_").endsWith("1234");
            assertThat(redactionService.maskSecret(ghsToken, "GITHUB_TOKEN")).startsWith("ghs_").endsWith("1234");
        }

        @Test
        @DisplayName("GitHub Fine-Grained PAT: preserves 'github_pat_', last 4, masks middle")
        void shouldMaskGitHubFineGrainedPat() {
            String finePat = "github_pat_11AAAAAAA_xxxxxxxxxxxxxxxxxxxxxx"; // 43 chars
            String masked = redactionService.maskSecret(finePat, "GITHUB_PAT");

            assertThat(masked).startsWith("github_pat_");
            assertThat(masked).endsWith("xxxx");
            assertThat(masked).hasSize(finePat.length());
            assertThat(masked).isEqualTo("github_pat_" + "*".repeat(finePat.length() - 15) + "xxxx");
        }

        @Test
        @DisplayName("AWS Access Key: preserves first 4 ('AKIA' or 'ASIA'), last 4, masks middle")
        void shouldMaskAwsAccessKey() {
            String awsKey = "AKIAIOSFODNN7EXAMPLE"; // 20 chars
            String masked = redactionService.maskSecret(awsKey, "AWS_ACCESS_KEY");

            assertThat(masked).startsWith("AKIA");
            assertThat(masked).endsWith("MPLE");
            assertThat(masked).hasSize(awsKey.length());
            assertThat(masked).isEqualTo("AKIA" + "*".repeat(awsKey.length() - 8) + "MPLE");

            String asiaKey = "ASIAIOSFODNN7EXAMPLE";
            assertThat(redactionService.maskSecret(asiaKey, "AWS_ACCESS_KEY"))
                    .startsWith("ASIA")
                    .endsWith("MPLE");
        }

        @Test
        @DisplayName("Short secrets (< 8 chars): entirely replaced with asterisks")
        void shouldMaskShortSecretsCompletely() {
            assertThat(redactionService.maskSecret("1234567", "GENERIC")).isEqualTo("*******");
            assertThat(redactionService.maskSecret("short", "GENERIC")).isEqualTo("*****");
            assertThat(redactionService.maskSecret("a", "GENERIC")).isEqualTo("*");
        }

        @Test
        @DisplayName("Generic long secrets (>= 8 chars): preserves first 2, last 2, masks middle")
        void shouldMaskGenericLongSecrets() {
            String secret = "my-super-secret-password-123";
            String masked = redactionService.maskSecret(secret, "GENERIC_SECRET");

            assertThat(masked).startsWith("my");
            assertThat(masked).endsWith("23");
            assertThat(masked).hasSize(secret.length());
            assertThat(masked).isEqualTo("my" + "*".repeat(secret.length() - 4) + "23");
        }

        @Test
        @DisplayName("Null or empty secret returns empty string")
        void shouldHandleNullOrEmptySecret() {
            assertThat(redactionService.maskSecret(null, "RULE")).isEmpty();
            assertThat(redactionService.maskSecret("", "RULE")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Redact Snippet")
    class RedactSnippetTests {

        @Test
        @DisplayName("Replaces raw secret in snippet with [REDACTED_SECRET]")
        void shouldRedactSnippet() {
            String snippet = "const apiKey = \"AIzaSyDummy1234567890\";\nconst client = new Client(apiKey);";
            String rawSecret = "AIzaSyDummy1234567890";

            String redacted = redactionService.redactSnippet(snippet, rawSecret, "SP-CONFIG-001");

            assertThat(redacted).isEqualTo("const apiKey = \"[REDACTED_SECRET]\";\nconst client = new Client(apiKey);");
            assertThat(redacted).doesNotContain(rawSecret);
        }

        @Test
        @DisplayName("Handles multiple occurrences of raw secret in snippet")
        void shouldRedactMultipleOccurrencesInSnippet() {
            String snippet = "key1: \"secret123\", key2: \"secret123\"";
            String rawSecret = "secret123";

            String redacted = redactionService.redactSnippet(snippet, rawSecret, "RULE");

            assertThat(redacted).isEqualTo("key1: \"[REDACTED_SECRET]\", key2: \"[REDACTED_SECRET]\"");
        }

        @Test
        @DisplayName("Handles null and empty snippets gracefully")
        void shouldHandleNullAndEmptySnippet() {
            assertThat(redactionService.redactSnippet(null, "secret", "RULE")).isEmpty();
            assertThat(redactionService.redactSnippet("snippet", null, "RULE")).isEqualTo("snippet");
            assertThat(redactionService.redactSnippet("snippet", "", "RULE")).isEqualTo("snippet");
        }
    }

    @Nested
    @DisplayName("Redact Text")
    class RedactTextTests {

        @Test
        @DisplayName("Redacts multiple secrets in arbitrary text in length-descending order")
        void shouldRedactMultipleSecretsInText() {
            String rawText = "Logs: User token was ghp_12345678 and prefix ghp_1234 in session.";
            List<String> secrets = List.of("ghp_1234", "ghp_12345678");

            String redacted = redactionService.redactText(rawText, secrets);

            assertThat(redacted).isEqualTo("Logs: User token was [REDACTED_SECRET] and prefix [REDACTED_SECRET] in session.");
            assertThat(redacted).doesNotContain("ghp_12345678");
            assertThat(redacted).doesNotContain("ghp_1234");
        }

        @Test
        @DisplayName("Handles null and empty inputs for redactText")
        void shouldHandleNullAndEmptyTextInputs() {
            assertThat(redactionService.redactText(null, List.of("s"))).isEmpty();
            assertThat(redactionService.redactText("some text", null)).isEqualTo("some text");
            assertThat(redactionService.redactText("some text", List.of())).isEqualTo("some text");
        }
    }

    @Nested
    @DisplayName("Build Redacted Evidence")
    class BuildRedactedEvidenceTests {

        @Test
        @DisplayName("Builds complete RedactedEvidence record without raw secrets")
        void shouldBuildRedactedEvidence() {
            String repoId = "repo-456";
            String rawSecret = "AIzaSyDummyGoogleApiKey1234567890";
            String snippet = "export const KEY = 'AIzaSyDummyGoogleApiKey1234567890';";

            SecretMatch match = new SecretMatch(
                    rawSecret,
                    "SP-CONFIG-001",
                    10,
                    10,
                    20,
                    53,
                    snippet
            );

            RedactedEvidence evidence = redactionService.buildRedactedEvidence(repoId, match);

            assertThat(evidence).isNotNull();
            assertThat(evidence.ruleId()).isEqualTo("SP-CONFIG-001");
            assertThat(evidence.startLine()).isEqualTo(10);
            assertThat(evidence.endLine()).isEqualTo(10);
            assertThat(evidence.startColumn()).isEqualTo(20);
            assertThat(evidence.endColumn()).isEqualTo(53);

            // Verify fingerprint
            assertThat(evidence.fingerprint()).hasSize(64);
            assertThat(evidence.fingerprint()).isEqualTo(fingerprintService.computeFingerprint(repoId, "SP-CONFIG-001", rawSecret));

            // Verify masked secret
            assertThat(evidence.maskedSecret()).startsWith("AIzaSy");
            assertThat(evidence.maskedSecret()).doesNotContain("DummyGoogleApiKey");

            // Verify redacted snippet
            assertThat(evidence.redactedSnippet()).isEqualTo("export const KEY = '[REDACTED_SECRET]';");
            assertThat(evidence.redactedSnippet()).doesNotContain(rawSecret);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when SecretMatch is null")
        void shouldThrowWhenSecretMatchIsNull() {
            assertThatThrownBy(() -> redactionService.buildRedactedEvidence("repo-1", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SecretMatch cannot be null");
        }
    }
}
