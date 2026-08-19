package com.scanpilot.ai.gemini;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Lean HTTP Client for Google Gemini REST API using Spring 6 RestClient.
 * Adheres to ponytail lean principles with zero heavy third-party SDKs.
 */
@Slf4j
@Component
public class GeminiApiClient {

    private final GeminiConfigProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiApiClient(GeminiConfigProperties properties) {
        this(properties, createDefaultRestClient(properties), createDefaultObjectMapper());
    }

    public GeminiApiClient(GeminiConfigProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    private static RestClient createDefaultRestClient(GeminiConfigProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(1, properties.getTimeoutSeconds()) * 1000;
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        return RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl(properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()
                ? properties.getBaseUrl()
                : "https://generativelanguage.googleapis.com/v1beta")
            .build();
    }

    private static ObjectMapper createDefaultObjectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Sends prompt to Gemini API with structured JSON output enforcement.
     *
     * @param request explanation request containing redacted context
     * @return structured GeminiExplanationResponse if successful
     */
    public Optional<GeminiExplanationResponse> generateExplanation(GeminiExplanationRequest request) {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.debug("Gemini AI client skipped: API key missing or integration disabled");
            return Optional.empty();
        }

        String prompt = buildPrompt(request);
        GeminiGenerateContentPayload payload = new GeminiGenerateContentPayload(
            List.of(new Content(List.of(new Part(prompt)))),
            new GenerationConfig("application/json", 0.2)
        );

        String uri = String.format("/models/%s:generateContent?key=%s", properties.getModel(), properties.getApiKey());

        try {
            log.info("Sending explanation request to Gemini model {} for findingId={}", properties.getModel(), request.findingId());

            GeminiApiResponse response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(GeminiApiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                log.warn("Gemini API returned empty response or candidates for findingId={}", request.findingId());
                return Optional.empty();
            }

            Candidate candidate = response.candidates().get(0);
            if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
                log.warn("Gemini candidate contains no content parts for findingId={}", request.findingId());
                return Optional.empty();
            }

            String rawJsonText = candidate.content().parts().get(0).text();
            GeminiExplanationResponse explanation = parseExplanationJson(rawJsonText);
            return Optional.of(explanation);
        } catch (Exception e) {
            log.warn("Failed to generate explanation from Gemini API for findingId={}: {}", request.findingId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Builds bounded, safe prompt engineering instructions.
     */
    private String buildPrompt(GeminiExplanationRequest request) {
        return """
            You are Scan Pilot's Security Explanation AI. Analyze the detected secret finding using the bounded redacted evidence below.
            
            Return ONLY a structured JSON object matching this schema:
            {
              "summary": "Plain-language explanation for beginners describing what happened",
              "riskImpact": "Real-world risk analysis explaining why this vulnerability matters and potential exploit scenarios",
              "evidenceLimits": "Scoped claims explicitly describing what this evidence proves and what it does NOT prove per EVIDENCE-MODEL.md",
              "remediationSteps": ["Step 1", "Step 2", "Step 3"],
              "remediationDiff": "Clean before/after code diff showing how to fix the issue (e.g. replacing hardcoded secret with environment variable or secret manager)",
              "revocationCommandHint": "Actionable command or dashboard URL to revoke/rotate the compromised secret",
              "sourceAttribution": "AI Inferred Guidance (Gemini 1.5 Flash)"
            }
            
            Security Boundaries:
            - Ground your analysis strictly in the provided redacted snippet and finding metadata.
            - Never invent unobserved repository files or pretend runtime infrastructure is known.
            - Note evidence limits clearly (proves presence in commit/file, does not prove active validity or external exposure).
            
            Finding Metadata:
            - Finding ID: %s
            - Repository ID: %s
            - Rule ID: %s
            - File Path: %s
            - Line Range: %s
            - Masked Secret: %s
            - Lifecycle State: %s
            - Remediation Quality: %s
            
            Redacted Code Snippet:
            %s
            """.formatted(
            request.findingId(),
            request.repositoryId(),
            request.ruleId(),
            request.filePath(),
            request.lineRange(),
            request.maskedSecret(),
            request.lifecycle(),
            request.remediationQuality(),
            request.redactedSnippet()
        );
    }

    private GeminiExplanationResponse parseExplanationJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Empty response body from Gemini API");
        }

        String cleaned = rawText.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            GeminiExplanationResponse parsed = objectMapper.readValue(cleaned, GeminiExplanationResponse.class);
            String attribution = parsed.sourceAttribution();
            if (attribution == null || attribution.isBlank()) {
                attribution = "AI Inferred Guidance (Gemini 1.5 Flash)";
            }
            return new GeminiExplanationResponse(
                parsed.summary(),
                parsed.riskImpact(),
                parsed.evidenceLimits(),
                parsed.remediationSteps(),
                parsed.remediationDiff(),
                parsed.revocationCommandHint(),
                attribution
            );
        } catch (Exception e) {
            log.error("Failed to deserialize Gemini JSON output: {}", cleaned, e);
            throw new IllegalStateException("Failed to parse Gemini JSON output: " + e.getMessage(), e);
        }
    }

    // JSON DTOs for Gemini GenerateContent API
    record GeminiGenerateContentPayload(List<Content> contents, GenerationConfig generationConfig) {}
    record Content(List<Part> parts) {}
    record Part(String text) {}
    record GenerationConfig(String responseMimeType, Double temperature) {}

    record GeminiApiResponse(List<Candidate> candidates) {}
    record Candidate(Content content, String finishReason) {}
}
