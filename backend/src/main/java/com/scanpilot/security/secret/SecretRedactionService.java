package com.scanpilot.security.secret;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Service for safe secret masking, snippet redaction, and building normalized evidence.
 * Ensures zero raw secrets escape the trusted normalization boundary into logs, metrics,
 * prompts, or persisted records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecretRedactionService {

    public static final String REDACTED_MARKER = "[REDACTED_SECRET]";

    private final SecretFingerprintService fingerprintService;

    /**
     * Masks a secret string according to token-specific redaction patterns.
     * Preserves minimal prefix/suffix for operational verification without revealing full entropy.
     *
     * @param rawSecret raw secret value
     * @param ruleId    detection rule identifier
     * @return masked secret string with asterisks
     */
    public String maskSecret(String rawSecret, String ruleId) {
        if (rawSecret == null || rawSecret.isEmpty()) {
            return "";
        }

        int len = rawSecret.length();

        // 1. Google API Key (starts with AIzaSy)
        if (rawSecret.startsWith("AIzaSy") && len >= 10) {
            return rawSecret.substring(0, 6) + "*".repeat(len - 10) + rawSecret.substring(len - 4);
        }

        // 2. GitHub Token (starts with github_pat_ or ghp_, gho_, ghs_, ghu_, ghr_)
        if (rawSecret.startsWith("github_pat_") && len >= 15) {
            int prefixLen = "github_pat_".length();
            return rawSecret.substring(0, prefixLen) + "*".repeat(len - prefixLen - 4) + rawSecret.substring(len - 4);
        }
        if ((rawSecret.startsWith("ghp_") || rawSecret.startsWith("gho_") || rawSecret.startsWith("ghs_")
                || rawSecret.startsWith("ghu_") || rawSecret.startsWith("ghr_")) && len >= 8) {
            int prefixLen = 4;
            return rawSecret.substring(0, prefixLen) + "*".repeat(len - prefixLen - 4) + rawSecret.substring(len - 4);
        }

        // 3. AWS Access Key (starts with AKIA or ASIA, or AWS rule ID with >= 8 chars)
        if ((rawSecret.startsWith("AKIA") || rawSecret.startsWith("ASIA")
                || (ruleId != null && ruleId.toUpperCase().contains("AWS"))) && len >= 8) {
            return rawSecret.substring(0, 4) + "*".repeat(len - 8) + rawSecret.substring(len - 4);
        }

        // 4. Short secrets (< 8 chars) -> fully masked
        if (len < 8) {
            return "*".repeat(len);
        }

        // 5. Generic secrets (>= 8 chars) -> preserve first 2, last 2
        return rawSecret.substring(0, 2) + "*".repeat(len - 4) + rawSecret.substring(len - 2);
    }

    /**
     * Replaces all occurrences of the raw secret in a snippet with [REDACTED_SECRET].
     *
     * @param rawSnippet source code snippet containing the match
     * @param rawSecret  raw secret to redact
     * @param ruleId     detection rule identifier
     * @return safe snippet with redacted secret
     */
    public String redactSnippet(String rawSnippet, String rawSecret, String ruleId) {
        if (rawSnippet == null) {
            return "";
        }
        if (rawSecret == null || rawSecret.isEmpty()) {
            return rawSnippet;
        }
        return rawSnippet.replace(rawSecret, REDACTED_MARKER);
    }

    /**
     * Cleans all occurrences of secrets from arbitrary text, messages, logs, or AI prompts.
     * Sorts secrets by length descending to prevent substring substitution collision.
     *
     * @param rawText           raw text content
     * @param collectionOfSecrets collection of raw secrets to sanitize
     * @return sanitized text safe for logging and transmission
     */
    public String redactText(String rawText, Collection<String> collectionOfSecrets) {
        if (rawText == null) {
            return "";
        }
        if (collectionOfSecrets == null || collectionOfSecrets.isEmpty()) {
            return rawText;
        }

        String result = rawText;
        List<String> sortedSecrets = collectionOfSecrets.stream()
                .filter(s -> s != null && !s.isEmpty())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        for (String secret : sortedSecrets) {
            result = result.replace(secret, REDACTED_MARKER);
        }
        return result;
    }

    /**
     * Combines fingerprinting, masking, and snippet redaction into a safe RedactedEvidence record.
     *
     * @param repositoryId stable internal repository identifier
     * @param secretMatch  raw match info from detector
     * @return safe RedactedEvidence record
     */
    public RedactedEvidence buildRedactedEvidence(String repositoryId, SecretMatch secretMatch) {
        if (secretMatch == null) {
            throw new IllegalArgumentException("SecretMatch cannot be null");
        }

        String fingerprint = fingerprintService.computeFingerprint(
                repositoryId,
                secretMatch.ruleId(),
                secretMatch.rawSecret()
        );
        String maskedSecret = maskSecret(secretMatch.rawSecret(), secretMatch.ruleId());
        String redactedSnippet = redactSnippet(secretMatch.snippet(), secretMatch.rawSecret(), secretMatch.ruleId());

        log.debug("Built RedactedEvidence for rule={}, fp={}",
                secretMatch.ruleId(),
                fingerprintService.shortFingerprint(fingerprint));

        return new RedactedEvidence(
                maskedSecret,
                redactedSnippet,
                fingerprint,
                secretMatch.ruleId(),
                secretMatch.startLine(),
                secretMatch.endLine(),
                secretMatch.startColumn(),
                secretMatch.endColumn()
        );
    }
}
