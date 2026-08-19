package com.scanpilot.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@DisplayName("GeminiExplanationService Tests")
class GeminiExplanationServiceTest {

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private FindingLocationRepository findingLocationRepository;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    private GeminiConfigProperties properties;
    private GeminiApiClient apiClient;
    private GeminiExplanationService service;
    private ObjectMapper objectMapper;

    private UserEntity user;
    private RepositoryEntity repository;

    @BeforeEach
    void setUp() {
        evidenceItemRepository.deleteAll();
        findingLocationRepository.deleteAll();
        findingRepository.deleteAll();

        user = userRepository.findByGithubUserId(998877L)
            .orElseGet(() -> userRepository.save(UserEntity.builder()
                .githubUserId(998877L)
                .login("gemini-test-user")
                .name("Gemini Test User")
                .email("gemini@scanpilot.com")
                .createdAt(Instant.now())
                .build()));

        repository = repositoryRepository.findByUserIdAndGithubRepoId(user.getId(), 778899L)
            .orElseGet(() -> repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(778899L)
                .owner("scanpilot-test")
                .name("ai-test-repo")
                .fullName("scanpilot-test/ai-test-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .build()));

        properties = new GeminiConfigProperties();
        properties.setApiKey("test-gemini-api-key");
        properties.setModel("gemini-1.5-flash");
        properties.setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        properties.setTimeoutSeconds(5);
        properties.setEnabled(true);
        properties.setCacheTtlSeconds(3600);

        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Gemini REST API Integration & Parsing")
    class ApiIntegrationTests {

        @Test
        @DisplayName("Successfully invokes Gemini REST API and parses structured response")
        void testSuccessfulApiInvocation() {
            RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(properties.getBaseUrl());
            MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
            RestClient restClient = restClientBuilder.build();

            apiClient = new GeminiApiClient(properties, restClient, objectMapper);
            service = new GeminiExplanationService(properties, apiClient, findingRepository, findingLocationRepository, evidenceItemRepository);

            String geminiJsonResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"summary\\": \\"Exposed Google Gemini API key in backend config\\", \\"riskImpact\\": \\"Enables unauthorized API usage and billing costs\\", \\"evidenceLimits\\": \\"Proves presence in commit; does not prove current active status\\", \\"remediationSteps\\": [\\"Revoke key in Google AI Studio\\", \\"Move to environment variable\\"], \\"remediationDiff\\": \\"- key = \\\\\\"AIzaSy...\\\\\\"\\\\n+ key = process.env.GEMINI_KEY\\", \\"revocationCommandHint\\": \\"https://aistudio.google.com/app/apikey\\", \\"sourceAttribution\\": \\"AI Inferred Guidance (Gemini 1.5 Flash)\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

            mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-gemini-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(geminiJsonResponse, MediaType.APPLICATION_JSON));

            GeminiExplanationRequest request = new GeminiExplanationRequest(
                UUID.randomUUID(),
                repository.getId(),
                "google-api-key",
                "src/main/resources/application.properties",
                "12-12",
                "AIzaSy****************************1234",
                "gemini.api.key=[REDACTED_SECRET]",
                "OPEN",
                "UNRESOLVED"
            );

            GeminiExplanationResponse response = service.explain(request, "fp-12345");

            assertThat(response).isNotNull();
            assertThat(response.summary()).isEqualTo("Exposed Google Gemini API key in backend config");
            assertThat(response.riskImpact()).isEqualTo("Enables unauthorized API usage and billing costs");
            assertThat(response.evidenceLimits()).isEqualTo("Proves presence in commit; does not prove current active status");
            assertThat(response.remediationSteps()).hasSize(2);
            assertThat(response.sourceAttribution()).isEqualTo("AI Inferred Guidance (Gemini 1.5 Flash)");
            mockServer.verify();
        }

        @Test
        @DisplayName("Falls back to template guidance when Gemini API returns 500 error")
        void testFallbackOnApiError() {
            RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(properties.getBaseUrl());
            MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
            RestClient restClient = restClientBuilder.build();

            apiClient = new GeminiApiClient(properties, restClient, objectMapper);
            service = new GeminiExplanationService(properties, apiClient, findingRepository, findingLocationRepository, evidenceItemRepository);

            mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-gemini-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

            GeminiExplanationRequest request = new GeminiExplanationRequest(
                UUID.randomUUID(),
                repository.getId(),
                "google-api-key",
                "config.js",
                "10-10",
                "AIzaSy****************************1234",
                "apiKey = [REDACTED_SECRET]",
                "OPEN",
                "UNRESOLVED"
            );

            GeminiExplanationResponse response = service.explain(request, "fp-error-test");

            assertThat(response).isNotNull();
            assertThat(response.summary()).contains("Google / Gemini API key");
            assertThat(response.remediationSteps()).isNotEmpty();
            assertThat(response.sourceAttribution()).contains("Deterministic Fallback Guidance (google-api-key)");
            mockServer.verify();
        }

        @Test
        @DisplayName("Falls back to template guidance when API key is not configured")
        void testFallbackOnMissingApiKey() {
            properties.setApiKey("");
            apiClient = new GeminiApiClient(properties);
            service = new GeminiExplanationService(properties, apiClient, findingRepository, findingLocationRepository, evidenceItemRepository);

            GeminiExplanationRequest request = new GeminiExplanationRequest(
                UUID.randomUUID(),
                repository.getId(),
                "github-pat",
                ".env",
                "5-5",
                "ghp_********************************1234",
                "TOKEN=[REDACTED_SECRET]",
                "OPEN",
                "UNRESOLVED"
            );

            GeminiExplanationResponse response = service.explain(request, "fp-missing-key");

            assertThat(response).isNotNull();
            assertThat(response.summary()).contains("GitHub Personal Access Token");
            assertThat(response.sourceAttribution()).contains("Deterministic Fallback Guidance (github-pat)");
        }
    }

    @Nested
    @DisplayName("Rule Family Template Fallbacks")
    class FallbackTemplateTests {

        @BeforeEach
        void initService() {
            properties.setApiKey("");
            apiClient = new GeminiApiClient(properties);
            service = new GeminiExplanationService(properties, apiClient, findingRepository, findingLocationRepository, evidenceItemRepository);
        }

        @Test
        @DisplayName("Generates AWS access key guidance for AWS rules")
        void testAwsAccessKeyFallback() {
            GeminiExplanationRequest request = new GeminiExplanationRequest(
                UUID.randomUUID(),
                repository.getId(),
                "aws-access-key-id",
                "aws.config",
                "1-1",
                "AKIA****************1234",
                "aws_key=[REDACTED_SECRET]",
                "OPEN",
                "UNRESOLVED"
            );

            GeminiExplanationResponse response = service.explain(request, "fp-aws");
            assertThat(response.summary()).contains("Amazon Web Services");
            assertThat(response.revocationCommandHint()).contains("aws iam update-access-key");
            assertThat(response.sourceAttribution()).contains("aws-access-key");
        }

        @Test
        @DisplayName("Generates Private Key guidance for RSA/SSH private key rules")
        void testPrivateKeyFallback() {
            GeminiExplanationRequest request = new GeminiExplanationRequest(
                UUID.randomUUID(),
                repository.getId(),
                "private-key-rsa",
                "id_rsa",
                "1-20",
                "-----BEGIN RSA PRIVATE KEY-----***",
                "[REDACTED_SECRET]",
                "OPEN",
                "UNRESOLVED"
            );

            GeminiExplanationResponse response = service.explain(request, "fp-privkey");
            assertThat(response.summary()).contains("Private Key");
            assertThat(response.remediationSteps()).anyMatch(s -> s.contains("authorized_keys"));
            assertThat(response.sourceAttribution()).contains("private-key");
        }

        @Test
        @DisplayName("Generates generic fallback for unknown rules")
        void testGenericFallback() {
            GeminiExplanationRequest request = new GeminiExplanationRequest(
                UUID.randomUUID(),
                repository.getId(),
                "custom-secret-token",
                "app.json",
                "3-3",
                "se******************56",
                "secret=[REDACTED_SECRET]",
                "OPEN",
                "UNRESOLVED"
            );

            GeminiExplanationResponse response = service.explain(request, "fp-generic");
            assertThat(response.summary()).contains("sensitive secret credential");
            assertThat(response.sourceAttribution()).contains("generic");
        }
    }

    @Nested
    @DisplayName("Fingerprint Caching & Security Validations")
    class CacheAndSecurityTests {

        @Test
        @DisplayName("Fingerprint cache returns cached response without duplicate API calls")
        void testFingerprintCacheHit() {
            RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(properties.getBaseUrl());
            MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
            RestClient restClient = restClientBuilder.build();

            apiClient = new GeminiApiClient(properties, restClient, objectMapper);
            service = new GeminiExplanationService(properties, apiClient, findingRepository, findingLocationRepository, evidenceItemRepository);

            String geminiJsonResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"summary\\": \\"Cached summary\\", \\"riskImpact\\": \\"Cached risk\\", \\"evidenceLimits\\": \\"Cached limits\\", \\"remediationSteps\\": [\\"Step 1\\"], \\"remediationDiff\\": \\"+ diff\\", \\"revocationCommandHint\\": \\"cmd\\", \\"sourceAttribution\\": \\"AI Inferred Guidance (Gemini 1.5 Flash)\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

            // Exactly ONE API call expected
            mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-gemini-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(geminiJsonResponse, MediaType.APPLICATION_JSON));

            GeminiExplanationRequest request = new GeminiExplanationRequest(
                UUID.randomUUID(),
                repository.getId(),
                "google-api-key",
                "app.js",
                "1-1",
                "AIzaSy****************************1234",
                "apiKey = [REDACTED_SECRET]",
                "OPEN",
                "UNRESOLVED"
            );

            GeminiExplanationResponse firstCall = service.explain(request, "fp-cached-test");
            GeminiExplanationResponse secondCall = service.explain(request, "fp-cached-test");

            assertThat(firstCall).isNotNull();
            assertThat(secondCall).isNotNull();
            assertThat(secondCall.summary()).isEqualTo("Cached summary");
            mockServer.verify();
        }

        @Test
        @DisplayName("Throws IllegalArgumentException if raw unmasked secret is passed")
        void testZeroRawSecretEnforcement() {
            properties.setApiKey("");
            apiClient = new GeminiApiClient(properties);
            service = new GeminiExplanationService(properties, apiClient, findingRepository, findingLocationRepository, evidenceItemRepository);

            GeminiExplanationRequest unsafeRequest = new GeminiExplanationRequest(
                UUID.randomUUID(),
                repository.getId(),
                "google-api-key",
                "app.js",
                "1-1",
                "AIzaSyAbcDef1234567890GhiJklMnoPqrStuV", // raw unmasked secret!
                "key = AIzaSyAbcDef1234567890GhiJklMnoPqrStuV",
                "OPEN",
                "UNRESOLVED"
            );

            assertThatThrownBy(() -> service.explain(unsafeRequest, "fp-unsafe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raw secrets are prohibited");
        }
    }

    @Nested
    @DisplayName("explainAndPersist & Evidence Storage")
    class PersistenceTests {

        @BeforeEach
        void initService() {
            properties.setApiKey("");
            apiClient = new GeminiApiClient(properties);
            service = new GeminiExplanationService(properties, apiClient, findingRepository, findingLocationRepository, evidenceItemRepository);
        }

        @Test
        @DisplayName("explainAndPersist creates and persists AI_INFERENCE EvidenceItem in PostgreSQL")
        void testExplainAndPersistSuccess() {
            FindingEntity finding = findingRepository.save(FindingEntity.builder()
                .repositoryId(repository.getId())
                .ruleId("google-api-key")
                .fingerprint("fp-persistence-test-1")
                .severity("HIGH")
                .title("Exposed Google API Key")
                .description("Google API Key found in config.json")
                .lifecycle("OPEN")
                .remediationQuality("UNRESOLVED")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build());

            findingLocationRepository.save(FindingLocationEntity.builder()
                .findingId(finding.getId())
                .filePath("config.json")
                .startLine(14)
                .endLine(14)
                .startColumn(5)
                .endColumn(45)
                .commitSha("abc12345")
                .author("developer@scanpilot.com")
                .isCurrentHead(true)
                .detectedAt(Instant.now())
                .build());

            evidenceItemRepository.save(EvidenceItemEntity.builder()
                .findingId(finding.getId())
                .evidenceType("TECHNICAL")
                .maskedSecret("AIzaSy****************************1234")
                .redactedSnippet("\"google_key\": \"[REDACTED_SECRET]\"")
                .verificationStatus("OBSERVED")
                .sourceAttribution("GitleaksDetectorAdapter:SP-CONFIG-001")
                .createdAt(Instant.now())
                .build());

            GeminiExplanationResponse response = service.explainAndPersist(finding.getId());

            assertThat(response).isNotNull();
            assertThat(response.summary()).contains("Google / Gemini API key");

            List<EvidenceItemEntity> evidenceItems = evidenceItemRepository.findByFindingId(finding.getId());
            assertThat(evidenceItems).hasSize(2);

            EvidenceItemEntity aiEvidence = evidenceItems.stream()
                .filter(e -> "AI_INFERENCE".equals(e.getEvidenceType()))
                .findFirst()
                .orElseThrow();

            assertThat(aiEvidence.getVerificationStatus()).isEqualTo("INFERRED");
            assertThat(aiEvidence.getSourceAttribution()).contains("Deterministic Fallback Guidance");
            assertThat(aiEvidence.getRedactedSnippet()).contains("Google / Gemini API key");

            // Test getExistingExplanation
            Optional<GeminiExplanationResponse> existing = service.getExistingExplanation(finding.getId());
            assertThat(existing).isPresent();
            assertThat(existing.get().summary()).isEqualTo(response.summary());
        }

        @Test
        @DisplayName("explainAndPersist throws NoSuchElementException when finding does not exist")
        void testExplainNonExistentFinding() {
            UUID randomFindingId = UUID.randomUUID();
            assertThatThrownBy(() -> service.explainAndPersist(randomFindingId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Finding not found");
        }
    }
}
