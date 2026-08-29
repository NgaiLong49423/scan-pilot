package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingRemediationPrLinkEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.FindingRemediationPrLinkRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class FindingRemediationPrLinkPersistenceTest {

    @Autowired
    private FindingRemediationPrLinkRepository linkRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Persists and updates finding remediation PR link state machine")
    void testPersistAndStateTransition() {
        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(12345L)
                .login("test-user")
                .name("Test User")
                .avatarUrl("https://example.com/avatar.png")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(99999L)
                .owner("test-org")
                .name("test-repo")
                .fullName("test-org/test-repo")
                .defaultBranch("main")
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        FindingEntity finding = findingRepository.save(FindingEntity.builder()
                .repositoryId(repo.getId())
                .ruleId("SP-CONFIG-001")
                .fingerprint("fp-12345678")
                .severity("HIGH")
                .title("Exposed Secret")
                .lifecycle("OPEN")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build());

        String commitSha = "abcdef1234567890abcdef1234567890abcdef12";
        String idempotency = "remediation-pr:" + finding.getId() + ":" + commitSha;

        FindingRemediationPrLinkEntity link = FindingRemediationPrLinkEntity.builder()
                .findingId(finding.getId())
                .repositoryId(repo.getId())
                .sourceRevisionCommit(commitSha)
                .targetBranch("main")
                .headBranch("scanpilot/remediation-test")
                .state("PENDING")
                .idempotencyMarker(idempotency)
                .createdByUserId(user.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        FindingRemediationPrLinkEntity saved = linkRepository.saveAndFlush(link);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getState()).isEqualTo("PENDING");

        // Transition to CREATED with PR details
        saved.setState("CREATED");
        saved.setGithubPrNumber(42);
        saved.setGithubPrUrl("https://github.com/test-org/test-repo/pull/42");
        saved.setUpdatedAt(Instant.now());

        FindingRemediationPrLinkEntity updated = linkRepository.saveAndFlush(saved);
        assertThat(updated.getState()).isEqualTo("CREATED");
        assertThat(updated.getGithubPrNumber()).isEqualTo(42);
        assertThat(updated.getGithubPrUrl()).isEqualTo("https://github.com/test-org/test-repo/pull/42");

        Optional<FindingRemediationPrLinkEntity> found = linkRepository.findByFindingId(finding.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getGithubPrNumber()).isEqualTo(42);
    }

    @Test
    @DisplayName("Enforces unique constraint on (finding_id, source_revision_commit)")
    void testUniqueConstraint() {
        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(54321L)
                .login("test-user-2")
                .name("Test User 2")
                .avatarUrl("https://example.com/avatar2.png")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(88888L)
                .owner("test-org")
                .name("test-repo-2")
                .fullName("test-org/test-repo-2")
                .defaultBranch("main")
                .status("ACTIVE")
                .monitoredAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        FindingEntity finding = findingRepository.save(FindingEntity.builder()
                .repositoryId(repo.getId())
                .ruleId("SP-CONFIG-001")
                .fingerprint("fp-unique-1234")
                .severity("CRITICAL")
                .title("Root AWS Key")
                .lifecycle("OPEN")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build());

        String commitSha = "1111222233334444555566667777888899990000";

        FindingRemediationPrLinkEntity link1 = FindingRemediationPrLinkEntity.builder()
                .findingId(finding.getId())
                .repositoryId(repo.getId())
                .sourceRevisionCommit(commitSha)
                .targetBranch("main")
                .headBranch("scanpilot/remediation-1")
                .state("PENDING")
                .idempotencyMarker("m1")
                .createdByUserId(user.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        linkRepository.saveAndFlush(link1);

        FindingRemediationPrLinkEntity link2 = FindingRemediationPrLinkEntity.builder()
                .findingId(finding.getId())
                .repositoryId(repo.getId())
                .sourceRevisionCommit(commitSha)
                .targetBranch("main")
                .headBranch("scanpilot/remediation-2")
                .state("PENDING")
                .idempotencyMarker("m2")
                .createdByUserId(user.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertThatThrownBy(() -> linkRepository.saveAndFlush(link2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}