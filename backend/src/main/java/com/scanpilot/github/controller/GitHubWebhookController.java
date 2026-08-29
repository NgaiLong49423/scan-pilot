package com.scanpilot.github.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanpilot.github.config.GitHubAppConfigProperties;
import com.scanpilot.github.dto.GitHubWebhookPayloadDto;
import com.scanpilot.github.dto.WebhookDeliveryResponseDto;
import com.scanpilot.github.service.BoundedStreamReader;
import com.scanpilot.github.service.GitHubWebhookService;
import com.scanpilot.github.service.WebhookSignatureValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/github/webhooks")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private static final int MAX_PAYLOAD_BYTES = 1_048_576; // 1 MiB
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern SHA_PATTERN = Pattern.compile("^[0-9a-fA-F]{40}$");

    private final GitHubAppConfigProperties gitHubAppConfigProperties;
    private final WebhookSignatureValidator webhookSignatureValidator;
    private final GitHubWebhookService gitHubWebhookService;
    private final ObjectMapper objectMapper;

    public static class MalformedPayloadException extends RuntimeException {
        public MalformedPayloadException(String message) {
            super(message);
        }
    }

    @PostMapping
    public ResponseEntity<WebhookDeliveryResponseDto> handleWebhook(HttpServletRequest request) {
        String eventHeader = request.getHeader("X-GitHub-Event");
        String deliveryHeader = request.getHeader("X-GitHub-Delivery");
        String signatureHeader = request.getHeader("X-Hub-Signature-256");

        // 1. Content-Length early rejection if > 1 MiB
        String contentLengthHeader = request.getHeader("Content-Length");
        if (contentLengthHeader != null) {
            try {
                long contentLength = Long.parseLong(contentLengthHeader.trim());
                if (contentLength > MAX_PAYLOAD_BYTES) {
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                            .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_SIZE", "PAYLOAD_TOO_LARGE"));
                }
            } catch (NumberFormatException ignored) {
                // Defer to bounded stream reader
            }
        }

        // 2. Mandatory headers check
        if (eventHeader == null || eventHeader.isBlank()
                || deliveryHeader == null || deliveryHeader.isBlank()
                || signatureHeader == null || signatureHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_HEADER", "MISSING_WEBHOOK_HEADERS"));
        }

        // Validate delivery ID format (UUID)
        if (!UUID_PATTERN.matcher(deliveryHeader.trim()).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_HEADER", "INVALID_DELIVERY_HEADER"));
        }

        // 3. Read bounded raw payload bytes
        byte[] rawPayload;
        try {
            rawPayload = BoundedStreamReader.readBoundedStream(request.getInputStream(), MAX_PAYLOAD_BYTES);
        } catch (BoundedStreamReader.PayloadTooLargeException e) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_SIZE", "PAYLOAD_TOO_LARGE"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_PARSE", "MALFORMED_WEBHOOK_PAYLOAD"));
        }

        // 4. Validate webhook secret configuration
        String webhookSecret = gitHubAppConfigProperties.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("GitHub webhook secret is unconfigured");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_CONFIG", "WEBHOOK_SECRET_UNCONFIGURED"));
        }

        // 5. Validate HMAC-SHA256 signature
        boolean validSignature = webhookSignatureValidator.validateSignature(rawPayload, signatureHeader, webhookSecret);
        if (!validSignature) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_SIGNATURE", "INVALID_WEBHOOK_SIGNATURE"));
        }

        // 6. Parse allow-listed fields from JSON payload with event-specific strict validation
        GitHubWebhookPayloadDto payloadDto;
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            if (root == null || !root.isObject()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_PARSE", "MALFORMED_WEBHOOK_PAYLOAD"));
            }
            payloadDto = parsePayload(root, eventHeader.trim());
        } catch (Exception e) {
            log.warn("Failed to parse webhook JSON payload");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new WebhookDeliveryResponseDto(deliveryHeader, "REJECTED_PARSE", "MALFORMED_WEBHOOK_PAYLOAD"));
        }

        // 7. Process webhook within transactional service
        try {
            WebhookDeliveryResponseDto response = gitHubWebhookService.processWebhook(
                    deliveryHeader.trim(),
                    eventHeader.trim(),
                    payloadDto
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Internal error processing webhook delivery");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new WebhookDeliveryResponseDto(deliveryHeader, "INTERNAL_ERROR", "PROCESSING_FAILED"));
        }
    }

    private GitHubWebhookPayloadDto parsePayload(JsonNode root, String eventType) {
        boolean isPing = "ping".equalsIgnoreCase(eventType);
        boolean isPush = "push".equalsIgnoreCase(eventType);
        boolean isPr = "pull_request".equalsIgnoreCase(eventType);

        Long githubRepoId = extractStrictPositiveLong(root, "repository", "id", !isPing);
        Long installationId = extractStrictPositiveLong(root, "installation", "id", !isPing);

        String rawRef = extractStrictText(root, "ref", false);
        String branch = sanitizeBranch(rawRef);
        String defaultBranch = truncate(extractStrictText(root, new String[]{"repository", "default_branch"}, false), 255);

        String baseBranch = truncate(extractStrictText(root, new String[]{"pull_request", "base", "ref"}, false), 255);
        String headBranch = truncate(extractStrictText(root, new String[]{"pull_request", "head", "ref"}, false), 255);

        boolean isDeleted = extractStrictBoolean(root, new String[]{"deleted"}, false);
        boolean isMerged = extractStrictBoolean(root, new String[]{"pull_request", "merged"}, false);
        String prAction = truncate(extractStrictText(root, new String[]{"action"}, isPr), 64);

        Integer prNumber = null;
        if (isPr) {
            JsonNode prNode = root.path("pull_request");
            if (!prNode.isObject()) {
                throw new MalformedPayloadException("pull_request node must be an object");
            }
            prNumber = extractStrictPositiveInt(root, "number", true);
        } else if (root.hasNonNull("number")) {
            prNumber = extractStrictPositiveInt(root, "number", false);
        }

        // Commit SHA
        String rawCommitSha = extractStrictSha(root, new String[]{"pull_request", "head", "sha"});
        if (rawCommitSha == null) {
            rawCommitSha = extractStrictSha(root, new String[]{"head_commit", "id"});
        }
        if (rawCommitSha == null) {
            rawCommitSha = extractStrictSha(root, new String[]{"after"});
        }

        // Base SHA
        String rawBaseSha = extractStrictSha(root, new String[]{"pull_request", "base", "sha"});
        if (rawBaseSha == null) {
            rawBaseSha = extractStrictSha(root, new String[]{"before"});
        }

        boolean repoFork = extractStrictBoolean(root, new String[]{"repository", "fork"}, false);
        boolean prFork = extractStrictBoolean(root, new String[]{"pull_request", "head", "repo", "fork"}, false);
        boolean isFork = repoFork || prFork;

        return GitHubWebhookPayloadDto.builder()
                .githubRepoId(githubRepoId)
                .installationId(installationId)
                .branch(branch)
                .defaultBranch(defaultBranch)
                .baseBranch(baseBranch)
                .headBranch(headBranch)
                .isDeleted(isDeleted)
                .isMerged(isMerged)
                .prAction(prAction)
                .prNumber(prNumber)
                .commitSha(rawCommitSha)
                .baseSha(rawBaseSha)
                .isFork(isFork)
                .build();
    }

    private Long extractStrictPositiveLong(JsonNode node, String parent, String child, boolean required) {
        JsonNode parentNode = node.path(parent);
        if (!parentNode.isObject()) {
            if (required) {
                throw new MalformedPayloadException("Missing parent object: " + parent);
            }
            return null;
        }
        JsonNode childNode = parentNode.get(child);
        if (childNode == null || childNode.isNull()) {
            if (required) {
                throw new MalformedPayloadException("Missing required property: " + parent + "." + child);
            }
            return null;
        }
        if (!childNode.isIntegralNumber() || childNode.asLong() <= 0) {
            throw new MalformedPayloadException("Property " + parent + "." + child + " must be a positive integer");
        }
        return childNode.asLong();
    }

    private Integer extractStrictPositiveInt(JsonNode parentNode, String child, boolean required) {
        JsonNode childNode = parentNode.get(child);
        if (childNode == null || childNode.isNull()) {
            if (required) {
                throw new MalformedPayloadException("Missing required integer property: " + child);
            }
            return null;
        }
        if (!childNode.isIntegralNumber() || childNode.asInt() <= 0) {
            throw new MalformedPayloadException("Property " + child + " must be a positive integer");
        }
        return childNode.asInt();
    }

    private boolean extractStrictBoolean(JsonNode root, String[] path, boolean defaultValue) {
        JsonNode current = root;
        for (String p : path) {
            if (!current.has(p)) {
                return defaultValue;
            }
            current = current.get(p);
        }
        if (current.isNull()) {
            return defaultValue;
        }
        if (!current.isBoolean()) {
            throw new MalformedPayloadException("Expected boolean value at " + String.join(".", path));
        }
        return current.asBoolean();
    }

    private String extractStrictSha(JsonNode root, String[] path) {
        JsonNode current = root;
        for (String p : path) {
            if (!current.has(p)) {
                return null;
            }
            current = current.get(p);
        }
        if (current.isNull()) {
            return null;
        }
        if (!current.isTextual()) {
            throw new MalformedPayloadException("Expected textual SHA at " + String.join(".", path));
        }
        String text = current.asText().trim();
        if (text.isEmpty()) {
            return null;
        }
        if (!SHA_PATTERN.matcher(text).matches()) {
            throw new MalformedPayloadException("Invalid SHA format at " + String.join(".", path));
        }
        return text;
    }

    private String extractStrictText(JsonNode node, String field, boolean required) {
        return extractStrictText(node, new String[]{field}, required);
    }

    private String extractStrictText(JsonNode node, String[] path, boolean required) {
        JsonNode current = node;
        for (String p : path) {
            if (!current.has(p)) {
                if (required) {
                    throw new MalformedPayloadException("Missing required text property: " + String.join(".", path));
                }
                return null;
            }
            current = current.get(p);
        }
        if (current.isNull()) {
            if (required) {
                throw new MalformedPayloadException("Required text property cannot be null: " + String.join(".", path));
            }
            return null;
        }
        if (!current.isTextual()) {
            throw new MalformedPayloadException("Property must be text: " + String.join(".", path));
        }
        String text = current.asText().trim();
        if (text.isEmpty() && required) {
            throw new MalformedPayloadException("Required text property cannot be empty: " + String.join(".", path));
        }
        return text.isEmpty() ? null : text;
    }

    private String sanitizeBranch(String ref) {
        if (ref == null) {
            return null;
        }
        String branch = ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : ref;
        return truncate(branch.trim(), 255);
    }

    private String truncate(String val, int maxLen) {
        if (val == null) {
            return null;
        }
        return val.length() > maxLen ? val.substring(0, maxLen) : val;
    }
}
