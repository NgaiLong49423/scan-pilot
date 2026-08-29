package com.scanpilot.github.controller;

import com.scanpilot.github.config.GitHubAppConfigProperties;
import com.scanpilot.github.service.GitHubWebhookService;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.entity.WebhookDeliveryEntity;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.persistence.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("GitHubWebhookController API Tests")
class GitHubWebhookControllerTest {

    private static final String TEST_SECRET = "test-webhook-secret-xyz";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubAppConfigProperties gitHubAppConfigProperties;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private GitHubWebhookService gitHubWebhookService;

    private UserEntity testUser;
    private RepositoryEntity testRepo;

    @BeforeEach
    void setUp() {
        gitHubAppConfigProperties.setWebhookSecret(TEST_SECRET);

        long uniqueGithubUserId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        testUser = userRepository.save(UserEntity.builder()
                .githubUserId(uniqueGithubUserId)
                .login("test-owner-" + uniqueGithubUserId)
                .createdAt(Instant.now())
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(999000L)
                .installationId(888000L)
                .fullName("test-owner/test-repo")
                .status("ACTIVE")
                .build());
    }

    private String computeHmac(byte[] payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
    }

    @Test
    @DisplayName("Should return 400 when mandatory headers are missing and write 0 rows to DB")
    void testMissingHeadersReturns400WithZeroDbWrites() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_HEADER"))
                .andExpect(jsonPath("$.reason").value("MISSING_WEBHOOK_HEADERS"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should return 400 when X-GitHub-Delivery is not a valid UUID and write 0 rows to DB")
    void testInvalidDeliveryHeaderReturns400WithZeroDbWrites() throws Exception {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", "invalid-not-uuid-1234")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_HEADER"))
                .andExpect(jsonPath("$.reason").value("INVALID_DELIVERY_HEADER"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should return 500 when webhook secret is unconfigured and write 0 rows to DB")
    void testUnconfiguredSecretReturns500WithZeroDbWrites() throws Exception {
        gitHubAppConfigProperties.setWebhookSecret("");

        String deliveryId = UUID.randomUUID().toString();
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", "sha256=1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("REJECTED_CONFIG"))
                .andExpect(jsonPath("$.reason").value("WEBHOOK_SECRET_UNCONFIGURED"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should return 413 when payload exceeds 1 MiB and write 0 rows to DB")
    void testBodyExceeding1MiBReturns413WithZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        byte[] largePayload = new byte[1_048_576 + 1];
        java.util.Arrays.fill(largePayload, (byte) 'a');
        String signature = computeHmac(largePayload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(largePayload))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value("REJECTED_SIZE"))
                .andExpect(jsonPath("$.reason").value("PAYLOAD_TOO_LARGE"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should return 401 when signature is invalid and write 0 rows to DB")
    void testInvalidSignatureReturns401WithZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", "sha256=0000000000000000000000000000000000000000000000000000000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("REJECTED_SIGNATURE"))
                .andExpect(jsonPath("$.reason").value("INVALID_WEBHOOK_SIGNATURE"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should return 400 when JSON payload is malformed and write 0 rows to DB")
    void testMalformedJsonPayloadReturns400WithZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        byte[] malformedPayload = "{invalid-json:".getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(malformedPayload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Signed push with repository.id as string must return 400 and write 0 rows to DB")
    void testSignedPushWithRepositoryIdAsStringReturns400AndZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "id": "not-a-number",
                    "default_branch": "main"
                },
                "installation": {
                    "id": 888000
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Signed push missing installation.id must return 400 and write 0 rows to DB")
    void testSignedPushMissingInstallationIdReturns400AndZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "id": 999000,
                    "default_branch": "main"
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Signed PR with invalid head SHA must return 400 and write 0 rows to DB")
    void testSignedPrWithInvalidHeadShaReturns400AndZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "action": "opened",
                "number": 42,
                "repository": {
                    "id": 999000
                },
                "installation": {
                    "id": 888000
                },
                "pull_request": {
                    "head": {
                        "sha": "invalid-non-40-hex-sha"
                    }
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Signed PR with non-numeric root PR number must return 400 and write 0 rows to DB")
    void testSignedPrWithNonNumericPrNumberReturns400AndZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "action": "opened",
                "number": "forty-two",
                "repository": {
                    "id": 999000
                },
                "installation": {
                    "id": 888000
                },
                "pull_request": {
                    "head": {
                        "sha": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    }
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Signed PR missing root PR number must return 400 and write 0 rows to DB")
    void testSignedPrMissingRootNumberReturns400AndZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "action": "opened",
                "repository": {
                    "id": 999000
                },
                "installation": {
                    "id": 888000
                },
                "pull_request": {
                    "head": {
                        "sha": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    }
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Signed PR with zero or negative root PR number must return 400 and write 0 rows to DB")
    void testSignedPrWithZeroOrNegativeRootNumberReturns400AndZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "action": "opened",
                "number": 0,
                "repository": {
                    "id": 999000
                },
                "installation": {
                    "id": 888000
                },
                "pull_request": {
                    "head": {
                        "sha": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    }
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Signed PR with decimal root PR number must return 400 and write 0 rows to DB")
    void testSignedPrWithDecimalRootNumberReturns400AndZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "action": "opened",
                "number": 42.5,
                "repository": {
                    "id": 999000
                },
                "installation": {
                    "id": 888000
                },
                "pull_request": {
                    "head": {
                        "sha": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    }
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Signed PR with non-boolean merged field must return 400 and write 0 rows to DB")
    void testSignedPrWithNonBooleanMergedReturns400AndZeroDbWrites() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "action": "closed",
                "number": 42,
                "repository": {
                    "id": 999000
                },
                "installation": {
                    "id": 888000
                },
                "pull_request": {
                    "merged": "yes-merged"
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andExpect(jsonPath("$.reason").value("MALFORMED_WEBHOOK_PAYLOAD"));

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should accept valid standards-shaped signed PR payload with root number and persist prNumber")
    void testValidSignedPrRequestPassesAndPersistsPrNumber() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "action": "opened",
                "number": 42,
                "repository": {
                    "id": 999000,
                    "default_branch": "main"
                },
                "installation": {
                    "id": 888000
                },
                "pull_request": {
                    "head": {
                        "ref": "feature/branch",
                        "sha": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    },
                    "base": {
                        "ref": "main",
                        "sha": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                    },
                    "merged": false
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(deliveryId))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.reason").value("ROUTED_ACTIVE_MONITORED_REPOSITORY"));

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("ACCEPTED");
        assertThat(entity.getPrNumber()).isEqualTo(42);
        assertThat(entity.getHeadBranch()).isEqualTo("feature/branch");
        assertThat(entity.getBaseBranch()).isEqualTo("main");
        assertThat(entity.getCommitSha()).isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertThat(entity.getBaseSha()).isEqualTo("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        assertThat(entity.getRepositoryId()).isEqualTo(testRepo.getId());
    }

    @Test
    @DisplayName("Signed malformed payload with forged token/path must return 400 without logging token, path, or stack trace")
    void testSignedMalformedPayloadWithForgedTokenZeroLeakage(CapturedOutput output) throws Exception {
        String forgedToken = "ghp_forged_secret_token_123456789";
        String sensitivePath = "/etc/passwd/secret/key";

        String deliveryId = UUID.randomUUID().toString();
        String malformedJson = String.format("{invalid_json_with_token: \"%s\", path: \"%s\"", forgedToken, sensitivePath);
        byte[] payload = malformedJson.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        MvcResult result = mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED_PARSE"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain(forgedToken);
        assertThat(responseBody).doesNotContain(sensitivePath);

        String logs = output.getAll();
        assertThat(logs).doesNotContain(forgedToken);
        assertThat(logs).doesNotContain(sensitivePath);
        assertThat(logs).doesNotContain("JsonParseException");
        assertThat(logs).doesNotContain("at com.fasterxml.jackson");

        assertThat(webhookDeliveryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Forced service processing exception must return 500 without logging sensitive exception message or stack trace")
    void testForcedServiceErrorWithForgedTokenZeroLeakage(CapturedOutput output) throws Exception {
        String forgedToken = "ghp_sensitive_internal_token_9999";
        String sensitivePath = "/var/secrets/database.key";

        doThrow(new RuntimeException("Crash with " + forgedToken + " at " + sensitivePath))
                .when(gitHubWebhookService).processWebhook(any(), any(), any());

        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "id": 999000,
                    "default_branch": "main"
                },
                "installation": {
                    "id": 888000
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        MvcResult result = mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.reason").value("PROCESSING_FAILED"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain(forgedToken);
        assertThat(responseBody).doesNotContain(sensitivePath);

        String logs = output.getAll();
        assertThat(logs).doesNotContain(forgedToken);
        assertThat(logs).doesNotContain(sensitivePath);
        assertThat(logs).doesNotContain("Crash with");
        assertThat(logs).doesNotContain("RuntimeException");
    }

    @Test
    @DisplayName("Should return 200 ACCEPTED for valid request and populate database")
    void testValidChunkedRequestWithoutContentLengthPasses() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "id": 999000,
                    "default_branch": "main",
                    "fork": false
                },
                "installation": {
                    "id": 888000
                },
                "head_commit": {
                    "id": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(deliveryId))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.reason").value("ROUTED_ACTIVE_MONITORED_REPOSITORY"));

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("ACCEPTED");
        assertThat(entity.getReasonCode()).isEqualTo("ROUTED_ACTIVE_MONITORED_REPOSITORY");
        assertThat(entity.getRepositoryId()).isEqualTo(testRepo.getId());
        assertThat(entity.getBranch()).isEqualTo("main");
        assertThat(entity.getDefaultBranch()).isEqualTo("main");
        assertThat(entity.getCommitSha()).isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    @DisplayName("Should return 200 IGNORED_UNSUPPORTED_EVENT for ping event")
    void testPingEventReturns200IgnoredUnsupportedEvent() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = "{\"zen\":\"Design for failure.\"}";
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "ping")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(deliveryId))
                .andExpect(jsonPath("$.status").value("IGNORED_UNSUPPORTED_EVENT"))
                .andExpect(jsonPath("$.reason").value("EVENT_PING_ACKNOWLEDGED"));

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo("IGNORED_UNSUPPORTED_EVENT");
        assertThat(entity.getReasonCode()).isEqualTo("EVENT_PING_ACKNOWLEDGED");
        assertThat(entity.getGithubRepoId()).isNull();
        assertThat(entity.getRepositoryId()).isNull();
    }

    @Test
    @DisplayName("Should return 200 IGNORED_DUPLICATE when deliveryId was already processed")
    void testDuplicateDeliveryReturns200IgnoredDuplicate() throws Exception {
        String deliveryId = UUID.randomUUID().toString();
        String json = """
            {
                "ref": "refs/heads/main",
                "repository": {
                    "id": 999000,
                    "default_branch": "main"
                },
                "installation": {
                    "id": 888000
                }
            }
            """;
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        // First delivery
        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Duplicate delivery
        mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED_DUPLICATE"))
                .andExpect(jsonPath("$.reason").value("DUPLICATE_DELIVERY"));
    }

    @Test
    @DisplayName("Should guarantee zero sensitive data / token / path leakage in response and database")
    void testZeroLeakageWhenPayloadContainsForgedTokensOrPaths() throws Exception {
        String forgedToken = "ghp_forged_secret_token_123456789";
        String sensitivePath = "/etc/passwd/secret/key";
        String commitMessage = "CVE-2026-9999 exploit code included";

        String deliveryId = UUID.randomUUID().toString();
        String json = String.format("""
            {
                "ref": "refs/heads/main",
                "repository": {
                    "id": 999000,
                    "default_branch": "main"
                },
                "installation": {
                    "id": 888000
                },
                "head_commit": {
                    "id": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                    "message": "%s"
                },
                "access_token": "%s",
                "file_path": "%s"
            }
            """, commitMessage, forgedToken, sensitivePath);

        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmac(payload, TEST_SECRET);

        MvcResult result = mockMvc.perform(post("/api/v1/github/webhooks")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain(forgedToken);
        assertThat(responseBody).doesNotContain(sensitivePath);
        assertThat(responseBody).doesNotContain(commitMessage);

        WebhookDeliveryEntity entity = webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
        assertThat(entity.getCommitSha()).isEqualTo("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    }
}

