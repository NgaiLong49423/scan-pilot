package com.scanpilot.ai.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for orchestrating AI Explanation and Remediation Guidance via Google Gemini (FR-005, FR-048, DEC-007, DEC-048).
 * Enforces zero raw secrets, fingerprint caching, and deterministic fallback templates.
 */
@Slf4j
@Service
public class GeminiExplanationService {

    private final GeminiConfigProperties properties;
    private final GeminiApiClient apiClient;
    private final FindingRepository findingRepository;
    private final FindingLocationRepository findingLocationRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final ObjectMapper objectMapper;

    // In-memory ConcurrentHashMap for fingerprint-based explanation caching
    private final Map<String, CachedExplanation> explanationCache = new ConcurrentHashMap<>();

    public GeminiExplanationService(
        GeminiConfigProperties properties,
        GeminiApiClient apiClient,
        FindingRepository findingRepository,
        FindingLocationRepository findingLocationRepository,
        EvidenceItemRepository evidenceItemRepository
    ) {
        this.properties = properties;
        this.apiClient = apiClient;
        this.findingRepository = findingRepository;
        this.findingLocationRepository = findingLocationRepository;
        this.evidenceItemRepository = evidenceItemRepository;
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Explains a finding request, checking fingerprint cache first and applying fallbacks if needed.
     *
     * @param request     sanitized explanation request
     * @param fingerprint finding fingerprint for caching
     * @return structured GeminiExplanationResponse
     */
    public GeminiExplanationResponse explain(GeminiExplanationRequest request, String fingerprint) {
        validateRequest(request);

        String cacheKey = (fingerprint != null && !fingerprint.isBlank())
            ? fingerprint
            : (request.findingId() != null ? request.findingId().toString() : "request-" + request.hashCode());

        CachedExplanation cached = explanationCache.get(cacheKey);
        if (cached != null && !isExpired(cached)) {
            log.debug("Cache hit for explanation fingerprint={}", cacheKey);
            return cached.response();
        }

        // Attempt Gemini API invocation
        Optional<GeminiExplanationResponse> apiResponse = apiClient.generateExplanation(request);
        GeminiExplanationResponse response = apiResponse.orElseGet(() -> generateFallbackExplanation(request));

        explanationCache.put(cacheKey, new CachedExplanation(response, Instant.now()));
        return response;
    }

    /**
     * Explains a finding by UUID, persists an AI_INFERENCE EvidenceItem in PostgreSQL, and caches the result.
     *
     * @param findingId UUID of the finding
     * @return structured GeminiExplanationResponse
     */
    @Transactional
    public GeminiExplanationResponse explainAndPersist(UUID findingId) {
        if (findingId == null) {
            throw new IllegalArgumentException("Finding ID cannot be null");
        }

        FindingEntity finding = findingRepository.findById(findingId)
            .orElseThrow(() -> new NoSuchElementException("Finding not found: " + findingId));

        List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(findingId);
        List<EvidenceItemEntity> evidenceList = evidenceItemRepository.findByFindingIdOrderByCreatedAtAsc(findingId);

        String filePath = "unknown";
        String lineRange = "1-1";
        if (!locations.isEmpty()) {
            FindingLocationEntity loc = locations.get(0);
            filePath = loc.getFilePath() != null ? loc.getFilePath() : "unknown";
            int start = loc.getStartLine() != null ? loc.getStartLine() : 1;
            int end = loc.getEndLine() != null ? loc.getEndLine() : start;
            lineRange = start == end ? String.valueOf(start) : start + "-" + end;
        }

        String maskedSecret = "[REDACTED]";
        String redactedSnippet = "[REDACTED_SNIPPET]";
        Optional<EvidenceItemEntity> technicalEvidence = evidenceList.stream()
            .filter(e -> "TECHNICAL".equalsIgnoreCase(e.getEvidenceType()))
            .findFirst();

        if (technicalEvidence.isPresent()) {
            EvidenceItemEntity tech = technicalEvidence.get();
            if (tech.getMaskedSecret() != null && !tech.getMaskedSecret().isBlank()) {
                maskedSecret = tech.getMaskedSecret();
            }
            if (tech.getRedactedSnippet() != null && !tech.getRedactedSnippet().isBlank()) {
                redactedSnippet = tech.getRedactedSnippet();
            }
        }

        GeminiExplanationRequest request = new GeminiExplanationRequest(
            finding.getId(),
            finding.getRepositoryId(),
            finding.getRuleId(),
            filePath,
            lineRange,
            maskedSecret,
            redactedSnippet,
            finding.getLifecycle(),
            finding.getRemediationQuality()
        );

        GeminiExplanationResponse response = explain(request, finding.getFingerprint());

        // Persist AI_INFERENCE EvidenceItem in PostgreSQL (DEC-007, docs/EVIDENCE-MODEL.md)
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize explanation response to JSON: {}", e.getMessage());
            jsonPayload = response.summary();
        }

        EvidenceItemEntity aiEvidence = EvidenceItemEntity.builder()
            .findingId(finding.getId())
            .evidenceType("AI_INFERENCE")
            .maskedSecret(maskedSecret)
            .redactedSnippet(jsonPayload)
            .verificationStatus("INFERRED")
            .sourceAttribution(response.sourceAttribution() != null ? response.sourceAttribution() : "AI Inferred Guidance (Gemini 1.5 Flash)")
            .createdAt(Instant.now())
            .build();

        evidenceItemRepository.save(aiEvidence);
        log.info("Persisted AI_INFERENCE EvidenceItem for findingId={}", findingId);

        return response;
    }

    /**
     * Retrieves an existing persisted AI explanation for a finding if present.
     *
     * @param findingId UUID of the finding
     * @return optional GeminiExplanationResponse
     */
    public Optional<GeminiExplanationResponse> getExistingExplanation(UUID findingId) {
        if (findingId == null) {
            return Optional.empty();
        }

        List<EvidenceItemEntity> evidenceList = evidenceItemRepository.findByFindingIdOrderByCreatedAtAsc(findingId);
        List<EvidenceItemEntity> aiItems = evidenceList.stream()
            .filter(e -> "AI_INFERENCE".equalsIgnoreCase(e.getEvidenceType()))
            .toList();

        if (aiItems.isEmpty()) {
            return Optional.empty();
        }

        // Return latest AI_INFERENCE item
        EvidenceItemEntity latest = aiItems.get(aiItems.size() - 1);
        if (latest.getRedactedSnippet() == null || latest.getRedactedSnippet().isBlank()) {
            return Optional.empty();
        }

        try {
            GeminiExplanationResponse parsed = objectMapper.readValue(latest.getRedactedSnippet(), GeminiExplanationResponse.class);
            return Optional.of(parsed);
        } catch (Exception e) {
            log.warn("Could not deserialize stored AI explanation JSON for findingId={}: {}", findingId, e.getMessage());
            return Optional.of(new GeminiExplanationResponse(
                latest.getRedactedSnippet(),
                "Real-world credential exposure risk.",
                "Technical evidence confirms secret presence in scanned commit.",
                List.of("Revoke credential", "Replace with environment variable"),
                "",
                "Rotate key via service provider console",
                latest.getSourceAttribution() != null ? latest.getSourceAttribution() : "AI Inferred Guidance (Gemini 1.5 Flash)"
            ));
        }
    }

    /**
     * Clears all cached explanation entries.
     */
    public void clearCache() {
        explanationCache.clear();
    }

    /**
     * Validates that raw secrets are never passed to the AI boundary (FR-048).
     */
    private void validateRequest(GeminiExplanationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Explanation request cannot be null");
        }

        String masked = request.maskedSecret();
        if (masked != null && !masked.isBlank()) {
            // Check for raw unmasked tokens that lack redaction markers
            if (isRawUnmaskedSecret(masked)) {
                throw new IllegalArgumentException("Unsafe unmasked secret in explanation request: raw secrets are prohibited across AI boundaries");
            }
        }
    }

    private boolean isRawUnmaskedSecret(String s) {
        if (s.contains("*") || s.contains("[REDACTED") || s.contains("...") || s.length() <= 4) {
            return false;
        }
        return s.startsWith("AIzaSy") || s.startsWith("ghp_") || s.startsWith("github_pat_")
            || s.startsWith("AKIA") || s.startsWith("ASIA") || s.startsWith("-----BEGIN");
    }

    private boolean isExpired(CachedExplanation cached) {
        long ageSeconds = Duration.between(cached.cachedAt(), Instant.now()).getSeconds();
        return ageSeconds >= properties.getCacheTtlSeconds();
    }

    /**
     * Generates structured fallback explanation templates for known rule families.
     */
    public GeminiExplanationResponse generateFallbackExplanation(GeminiExplanationRequest request) {
        String ruleFamily = matchRuleFamily(request.ruleId());
        log.info("Generating fallback template explanation for ruleFamily={} (ruleId={})", ruleFamily, request.ruleId());

        return switch (ruleFamily) {
            case "google-api-key" -> new GeminiExplanationResponse(
                "A Google / Gemini API key was detected in your code. Hardcoded API keys can be extracted by anyone with access to the source code or repository.",
                "If exposed, unauthorized parties can use your Google Cloud / AI quotas, incur financial charges on your billing account, and access associated Google Cloud APIs and models.",
                "Technical evidence proves this key was committed to the repository at the specified line. It does not prove whether the key is active, unrestricted, or has been abused.",
                List.of(
                    "Immediately revoke or regenerate the API key in the Google Cloud Console / Google AI Studio.",
                    "Restrict the API key to specific APIs, IP addresses, or HTTP referrers.",
                    "Move the key to an environment variable (e.g. GEMINI_API_KEY) or a secure secret manager.",
                    "If committed to Git history, rotate the key and consider rewriting Git history."
                ),
                """
                - const apiKey = "AIzaSy...";
                + const apiKey = process.env.GEMINI_API_KEY;
                """.stripIndent(),
                "Revoke key at https://console.cloud.google.com/apis/credentials or https://aistudio.google.com/app/apikey",
                "Deterministic Fallback Guidance (google-api-key)"
            );
            case "github-pat" -> new GeminiExplanationResponse(
                "A GitHub Personal Access Token (PAT) was detected in your codebase.",
                "An attacker with this token could access your private repositories, push malicious code, alter GitHub Actions workflows, or access organization resources depending on token permissions.",
                "Proves token string presence in repository files/history. Does not verify token validity, expiration, or granted OAuth/PAT scopes.",
                List.of(
                    "Revoke the token immediately in GitHub Settings -> Developer settings -> Personal access tokens.",
                    "Generate a fine-grained personal access token with the minimum required permissions.",
                    "Store the token in GitHub Secrets or local .env file (ensure .env is in .gitignore)."
                ),
                """
                - GITHUB_TOKEN="ghp_..."
                + GITHUB_TOKEN="${GITHUB_TOKEN}"
                """.stripIndent(),
                "Revoke token at https://github.com/settings/tokens or run `gh auth logout`",
                "Deterministic Fallback Guidance (github-pat)"
            );
            case "aws-access-key" -> new GeminiExplanationResponse(
                "An Amazon Web Services (AWS) Access Key ID or Secret Key was detected in your code.",
                "Exposed AWS credentials can allow attackers to provision costly cloud infrastructure, exfiltrate S3 bucket data, modify IAM policies, and compromise your cloud environment.",
                "Proves the AWS credential pattern exists in the scanned repository. Does not verify active IAM permissions or STS session status.",
                List.of(
                    "Deactivate and delete the access key immediately in AWS IAM Console.",
                    "Create a new IAM user/role with least-privilege policies.",
                    "Use AWS IAM Roles, AWS Secrets Manager, or AWS Vault instead of long-lived access keys."
                ),
                """
                - AWS_ACCESS_KEY_ID="AKIA..."
                + AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"
                """.stripIndent(),
                "aws iam update-access-key --access-key-id <KEY_ID> --status Inactive",
                "Deterministic Fallback Guidance (aws-access-key)"
            );
            case "private-key" -> new GeminiExplanationResponse(
                "An asymmetric Private Key (e.g. RSA, SSH, or PKCS#8) was detected in your repository.",
                "Private keys allow attackers to decrypt confidential traffic, impersonate servers/users, sign unauthorized binaries, or access SSH servers.",
                "Proves cryptographic private key material is stored in repository files. Does not check if the corresponding public key is currently authorized on servers.",
                List.of(
                    "Remove the private key from all authorized_keys files and servers immediately.",
                    "Generate a new cryptographic keypair.",
                    "Store private keys in a secure hardware module, SSH agent, or Vault, never in code repositories."
                ),
                """
                - -----BEGIN RSA PRIVATE KEY-----
                - ...
                - -----END RSA PRIVATE KEY-----
                + # Reference private key via path or SSH agent
                + PRIVATE_KEY_PATH="/etc/secrets/private_key.pem"
                """.stripIndent(),
                "Remove public key from ~/.ssh/authorized_keys and cloud provider SSH key management",
                "Deterministic Fallback Guidance (private-key)"
            );
            default -> new GeminiExplanationResponse(
                "A sensitive secret credential was detected in your repository.",
                "Hardcoded secrets in code repositories are vulnerable to leaks, unauthorized access, and privilege escalation.",
                "Proves secret pattern match at the specified repository location. Does not verify external credential validity or unauthorized usage.",
                List.of(
                    "Revoke and rotate the exposed credential in the respective service provider dashboard.",
                    "Store secrets in environment variables or an external secret management service (e.g., Vault, AWS Secrets Manager).",
                    "Add sensitive configuration files to .gitignore."
                ),
                """
                - SECRET_KEY="hardcoded_secret_value"
                + SECRET_KEY=System.getenv("SECRET_KEY")
                """.stripIndent(),
                "Rotate and invalidate the exposed secret key on the service provider management console",
                "Deterministic Fallback Guidance (generic)"
            );
        };
    }

    private String matchRuleFamily(String ruleId) {
        if (ruleId == null) {
            return "generic";
        }
        String lower = ruleId.toLowerCase();
        if (lower.contains("google") || lower.contains("gemini") || lower.contains("gcp")) {
            return "google-api-key";
        }
        if (lower.contains("github") || lower.contains("ghp") || lower.contains("pat")) {
            return "github-pat";
        }
        if (lower.contains("aws") || lower.contains("akuser") || lower.contains("access-key")) {
            return "aws-access-key";
        }
        if (lower.contains("private-key") || lower.contains("rsa") || lower.contains("ssh") || lower.contains("pgp") || lower.contains("pkcs")) {
            return "private-key";
        }
        return "generic";
    }

    private record CachedExplanation(GeminiExplanationResponse response, Instant cachedAt) {}
}
