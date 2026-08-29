package com.scanpilot.github.service;

import com.scanpilot.persistence.entity.InstallationStateEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.InstallationStateRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("InstallationStateService Tests")
class InstallationStateServiceTest {

    @Autowired
    private InstallationStateService installationStateService;

    @Autowired
    private InstallationStateRepository installationStateRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID testUserId;
    private String testSessionId;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        long randomGithubId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        testUser = userRepository.save(UserEntity.builder()
                .githubUserId(randomGithubId)
                .login("test-user-" + randomGithubId)
                .name("Test User")
                .email("test@user.local")
                .createdAt(Instant.now())
                .build());

        testUserId = testUser.getId();
        testSessionId = "session-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("AC-01: Should generate opaque 256-bit state and store only SHA-256 hash in database")
    void testGenerateOpaqueStateStoresOnlySha256Hash() {
        String rawState = installationStateService.generateAndSaveState(testUserId, testSessionId);

        assertThat(rawState).isNotNull().isNotBlank();
        // 32 bytes Base64URL encoded is 43 characters
        assertThat(rawState.length()).isGreaterThanOrEqualTo(40);

        String stateHash = InstallationStateService.computeSha256(rawState);
        Optional<InstallationStateEntity> saved = installationStateRepository.findByStateHash(stateHash);

        assertThat(saved).isPresent();
        assertThat(saved.get().getStateHash()).isEqualTo(stateHash);
        assertThat(saved.get().getUserId()).isEqualTo(testUserId);
        assertThat(saved.get().getSessionId()).isEqualTo(testSessionId);
        assertThat(saved.get().getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.get().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("AC-01: Should atomically consume state on first validation and reject on second validation (single-use)")
    void testAtomicStateConsumption() {
        String rawState = installationStateService.generateAndSaveState(testUserId, testSessionId);

        boolean firstConsume = installationStateService.validateAndConsumeState(rawState, testUserId, testSessionId);
        assertThat(firstConsume).isTrue();

        String stateHash = InstallationStateService.computeSha256(rawState);
        Optional<InstallationStateEntity> entity = installationStateRepository.findByStateHash(stateHash);
        assertThat(entity).isPresent();
        assertThat(entity.get().getStatus()).isEqualTo("CONSUMED");
        assertThat(entity.get().getConsumedAt()).isNotNull();

        // Replay attempt must fail closed
        boolean secondConsume = installationStateService.validateAndConsumeState(rawState, testUserId, testSessionId);
        assertThat(secondConsume).isFalse();
    }

    @Test
    @DisplayName("AC-01: Should reject expired state tokens")
    void testExpiredStateRejected() {
        String rawState = "test-expired-state-" + UUID.randomUUID();
        String stateHash = InstallationStateService.computeSha256(rawState);

        InstallationStateEntity expired = InstallationStateEntity.builder()
                .stateHash(stateHash)
                .userId(testUserId)
                .sessionId(testSessionId)
                .status("ACTIVE")
                .issuedAt(Instant.now().minusSeconds(1200))
                .expiresAt(Instant.now().minusSeconds(600)) // expired 10m ago
                .build();
        installationStateRepository.save(expired);

        boolean valid = installationStateService.validateAndConsumeState(rawState, testUserId, testSessionId);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("AC-01: Should reject state when consumed by a different user or session")
    void testCrossUserOrSessionMismatchRejected() {
        String rawState = installationStateService.generateAndSaveState(testUserId, testSessionId);

        long attackerGithubId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        UserEntity attackerUser = userRepository.save(UserEntity.builder()
                .githubUserId(attackerGithubId)
                .login("attacker-" + attackerGithubId)
                .name("Attacker")
                .email("attacker@local")
                .createdAt(Instant.now())
                .build());

        UUID attackerUserId = attackerUser.getId();
        boolean crossUserResult = installationStateService.validateAndConsumeState(rawState, attackerUserId, testSessionId);
        assertThat(crossUserResult).isFalse();

        boolean crossSessionResult = installationStateService.validateAndConsumeState(rawState, testUserId, "attacker-session");
        assertThat(crossSessionResult).isFalse();

        // State remains unconsumed and can still be consumed by legitimate user
        boolean legitResult = installationStateService.validateAndConsumeState(rawState, testUserId, testSessionId);
        assertThat(legitResult).isTrue();
    }

    @Test
    @DisplayName("AC-01: Should validate input parameters fail-closed")
    void testInputValidation() {
        assertThat(installationStateService.validateAndConsumeState(null, testUserId, testSessionId)).isFalse();
        assertThat(installationStateService.validateAndConsumeState("", testUserId, testSessionId)).isFalse();
        assertThat(installationStateService.validateAndConsumeState("some-state", null, testSessionId)).isFalse();
        assertThat(installationStateService.validateAndConsumeState("some-state", testUserId, null)).isFalse();

        assertThatThrownBy(() -> installationStateService.generateAndSaveState(null, testSessionId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
