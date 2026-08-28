package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingIssueLinkEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.FindingIssueLinkRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@DisplayName("Finding Issue Link Persistence Tests")
class FindingIssueLinkPersistenceTest {

    @Autowired
    private FindingIssueLinkRepository linkRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;
    private RepositoryEntity testRepo;
    private FindingEntity testFinding;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(UserEntity.builder()
                .githubUserId(12345L)
                .login("test-user")
                .name("Test User")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(testUser.getId())
                .githubRepoId(999L)
                .owner("test-user")
                .name("test-repo")
                .fullName("test-user/test-repo")
                .defaultBranch("main")
                .primaryBranch("main")
                .isPrivate(false)
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        testFinding = findingRepository.save(FindingEntity.builder()
                .repositoryId(testRepo.getId())
                .ruleId("SP-CONFIG-001")
                .fingerprint("fp-" + UUID.randomUUID())
                .severity("HIGH")
                .title("Test Secret Exposure")
                .lifecycle("OPEN")
                .remediationQuality("ACTION_REQUIRED")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Should successfully persist and retrieve finding issue link")
    void shouldPersistAndRetrieveLink() {
        FindingIssueLinkEntity link = FindingIssueLinkEntity.builder()
                .findingId(testFinding.getId())
                .repositoryId(testRepo.getId())
                .state("PENDING")
                .idempotencyMarker("scanpilot-finding-" + testFinding.getId())
                .createdByUserId(testUser.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        FindingIssueLinkEntity saved = linkRepository.save(link);
        assertThat(saved.getId()).isNotNull();

        Optional<FindingIssueLinkEntity> retrieved = linkRepository.findByFindingId(testFinding.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getState()).isEqualTo("PENDING");
        assertThat(retrieved.get().getRepositoryId()).isEqualTo(testRepo.getId());
    }

    @Test
    @DisplayName("Should enforce UNIQUE constraint on finding_id")
    void shouldEnforceUniqueConstraintOnFindingId() {
        FindingIssueLinkEntity link1 = FindingIssueLinkEntity.builder()
                .findingId(testFinding.getId())
                .repositoryId(testRepo.getId())
                .state("PENDING")
                .idempotencyMarker("marker-1")
                .createdByUserId(testUser.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        linkRepository.saveAndFlush(link1);

        FindingIssueLinkEntity link2 = FindingIssueLinkEntity.builder()
                .findingId(testFinding.getId())
                .repositoryId(testRepo.getId())
                .state("CREATED")
                .idempotencyMarker("marker-2")
                .createdByUserId(testUser.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> linkRepository.saveAndFlush(link2));
    }

    @Test
    @DisplayName("Should execute atomic conditional state update correctly")
    void shouldExecuteConditionalUpdate() {
        FindingIssueLinkEntity link = FindingIssueLinkEntity.builder()
                .findingId(testFinding.getId())
                .repositoryId(testRepo.getId())
                .state("UNKNOWN")
                .idempotencyMarker("scanpilot-finding-" + testFinding.getId())
                .createdByUserId(testUser.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        linkRepository.saveAndFlush(link);

        // Update when matching oldState UNKNOWN -> PENDING
        int updated = linkRepository.updateStateConditional(testFinding.getId(), "UNKNOWN", "PENDING", Instant.now());
        assertThat(updated).isEqualTo(1);

        FindingIssueLinkEntity reloaded = linkRepository.findByFindingId(testFinding.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo("PENDING");

        // Attempt conditional update when oldState does not match (expecting 0 rows updated)
        int staleUpdate = linkRepository.updateStateConditional(testFinding.getId(), "UNKNOWN", "CREATED", Instant.now());
        assertThat(staleUpdate).isEqualTo(0);
    }
}
