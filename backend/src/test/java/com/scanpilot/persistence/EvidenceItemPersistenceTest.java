package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Evidence Item Persistence Tests")
class EvidenceItemPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    private FindingEntity testFinding;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(4001L)
                .login("evidence_owner")
                .build());

        RepositoryEntity repo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(66001L)
                .fullName("evidence_owner/secure-pipeline")
                .build());

        testFinding = findingRepository.save(FindingEntity.builder()
                .repositoryId(repo.getId())
                .ruleId("SP-CONFIG-001")
                .fingerprint("fp_evidence_001")
                .severity("HIGH")
                .lifecycle("OPEN")
                .build());
    }

    @Test
    @DisplayName("Should persist append-oriented evidence items and retrieve them in chronological order")
    void shouldPersistAndRetrieveEvidenceItems() {
        Instant now = Instant.now();

        EvidenceItemEntity item1 = EvidenceItemEntity.builder()
                .findingId(testFinding.getId())
                .evidenceType("TECHNICAL")
                .maskedSecret("AIzaSy*******************************")
                .redactedSnippet("const apiKey = \"[REDACTED_SECRET]\";")
                .verificationStatus("OBSERVED")
                .sourceAttribution("GitleaksDetectorAdapter:SP-CONFIG-001")
                .createdAt(now.minus(10, ChronoUnit.MINUTES))
                .build();

        EvidenceItemEntity item2 = EvidenceItemEntity.builder()
                .findingId(testFinding.getId())
                .evidenceType("USER_ASSERTION")
                .maskedSecret(null)
                .redactedSnippet("Key was rotated and deactivated on GCP Console.")
                .verificationStatus("USER_ASSERTED")
                .sourceAttribution("AttributedUser:evidence_owner")
                .createdAt(now)
                .build();

        evidenceItemRepository.saveAll(List.of(item1, item2));

        List<EvidenceItemEntity> items = evidenceItemRepository
                .findByFindingIdOrderByCreatedAtAsc(testFinding.getId());

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getVerificationStatus()).isEqualTo("OBSERVED");
        assertThat(items.get(0).getMaskedSecret()).isEqualTo("AIzaSy*******************************");
        assertThat(items.get(1).getVerificationStatus()).isEqualTo("USER_ASSERTED");
    }
}
