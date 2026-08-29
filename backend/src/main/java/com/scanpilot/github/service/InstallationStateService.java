package com.scanpilot.github.service;

import com.scanpilot.persistence.entity.InstallationStateEntity;
import com.scanpilot.persistence.repository.InstallationStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstallationStateService {

    public static final long STATE_TTL_SECONDS = 600L; // 10 minutes

    private final InstallationStateRepository installationStateRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a 256-bit cryptographically secure opaque state token,
     * stores only its SHA-256 digest in PostgreSQL, and returns the raw state string.
     */
    @Transactional
    public String generateAndSaveState(UUID userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("User ID and session ID are required to generate installation state");
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawState = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String stateHash = computeSha256(rawState);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(STATE_TTL_SECONDS);

        InstallationStateEntity entity = InstallationStateEntity.builder()
                .stateHash(stateHash)
                .userId(userId)
                .sessionId(sessionId)
                .status("ACTIVE")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();

        installationStateRepository.save(entity);
        return rawState;
    }

    /**
     * Atomically validates and consumes the opaque state token.
     * Fails closed if the state is invalid, expired, already consumed, or bound to a different user/session.
     */
    @Transactional
    public boolean validateAndConsumeState(String rawState, UUID userId, String sessionId) {
        if (rawState == null || rawState.isBlank() || userId == null || sessionId == null || sessionId.isBlank()) {
            return false;
        }

        String stateHash = computeSha256(rawState);
        int updated = installationStateRepository.consumeState(stateHash, userId, sessionId, Instant.now());
        return updated > 0;
    }

    public static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
