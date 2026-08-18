package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Finding and Finding Location Persistence Tests")
class FindingAndLocationPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private FindingLocationRepository locationRepository;

    private RepositoryEntity testRepo;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(3001L)
                .login("finding_owner")
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(77001L)
                .fullName("finding_owner/vulnerable-app")
                .build());
    }

    @Nested
    @DisplayName("Finding Entity CRUD & Fingerprint Constraint")
    class FindingCrudTests {

        @Test
        @DisplayName("Should persist finding and find by repositoryId and fingerprint")
        void shouldPersistAndFindFinding() {
            Instant now = Instant.now();
            FindingEntity finding = FindingEntity.builder()
                    .repositoryId(testRepo.getId())
                    .ruleId("SP-CONFIG-001")
                    .fingerprint("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                    .severity("CRITICAL")
                    .title("Exposed Google API Key in client code")
                    .description("A high-entropy Google API key was detected.")
                    .lifecycle("OPEN")
                    .remediationQuality("ACTION_REQUIRED")
                    .firstSeenAt(now)
                    .lastSeenAt(now)
                    .build();

            FindingEntity saved = findingRepository.save(finding);
            assertThat(saved.getId()).isNotNull();

            Optional<FindingEntity> found = findingRepository
                    .findByRepositoryIdAndFingerprint(testRepo.getId(), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
            assertThat(found).isPresent();
            assertThat(found.get().getRuleId()).isEqualTo("SP-CONFIG-001");
            assertThat(found.get().getLifecycle()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("Should enforce UNIQUE(repository_id, fingerprint) constraint")
        void shouldEnforceRepositoryFingerprintUniqueness() {
            String sharedFingerprint = "abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234";

            FindingEntity f1 = FindingEntity.builder()
                    .repositoryId(testRepo.getId())
                    .ruleId("SP-CONFIG-001")
                    .fingerprint(sharedFingerprint)
                    .severity("HIGH")
                    .lifecycle("OPEN")
                    .build();
            findingRepository.saveAndFlush(f1);

            FindingEntity f2 = FindingEntity.builder()
                    .repositoryId(testRepo.getId())
                    .ruleId("SP-CONFIG-001")
                    .fingerprint(sharedFingerprint)
                    .severity("HIGH")
                    .lifecycle("OPEN")
                    .build();

            assertThatThrownBy(() -> findingRepository.saveAndFlush(f2))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should update finding lifecycle and remediation quality")
        void shouldUpdateFindingLifecycle() {
            Instant now = Instant.now();
            FindingEntity finding = findingRepository.save(FindingEntity.builder()
                    .repositoryId(testRepo.getId())
                    .ruleId("SP-CONFIG-001")
                    .fingerprint("fp_update_lifecycle_001")
                    .severity("CRITICAL")
                    .lifecycle("OPEN")
                    .remediationQuality("ACTION_REQUIRED")
                    .firstSeenAt(now)
                    .lastSeenAt(now)
                    .build());

            // Transition to RESOLVED with RISK_CONTAINED
            finding.setLifecycle("RESOLVED");
            finding.setRemediationQuality("RISK_CONTAINED");
            finding.setResolvedAt(Instant.now());
            findingRepository.saveAndFlush(finding);

            FindingEntity resolved = findingRepository.findById(finding.getId()).orElseThrow();
            assertThat(resolved.getLifecycle()).isEqualTo("RESOLVED");
            assertThat(resolved.getRemediationQuality()).isEqualTo("RISK_CONTAINED");
            assertThat(resolved.getResolvedAt()).isNotNull();

            // Check count queries
            long openCount = findingRepository.countByRepositoryIdAndLifecycle(testRepo.getId(), "OPEN");
            long resolvedCount = findingRepository.countByRepositoryIdAndLifecycle(testRepo.getId(), "RESOLVED");
            assertThat(openCount).isZero();
            assertThat(resolvedCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Finding Location Entity Tests")
    class FindingLocationTests {

        @Test
        @DisplayName("Should persist locations and query current head locations")
        void shouldManageFindingLocations() {
            FindingEntity finding = findingRepository.save(FindingEntity.builder()
                    .repositoryId(testRepo.getId())
                    .ruleId("SP-CONFIG-001")
                    .fingerprint("fp_loc_001")
                    .severity("HIGH")
                    .lifecycle("OPEN")
                    .build());

            FindingLocationEntity locHead = FindingLocationEntity.builder()
                    .findingId(finding.getId())
                    .filePath("src/main/resources/application.properties")
                    .startLine(14)
                    .endLine(14)
                    .startColumn(20)
                    .endColumn(59)
                    .commitSha("head_sha_999")
                    .author("dev@example.com")
                    .isCurrentHead(true)
                    .detectedAt(Instant.now())
                    .build();

            FindingLocationEntity locHistory = FindingLocationEntity.builder()
                    .findingId(finding.getId())
                    .filePath("old/config.json")
                    .startLine(5)
                    .endLine(5)
                    .commitSha("historic_sha_111")
                    .isCurrentHead(false)
                    .detectedAt(Instant.now())
                    .build();

            locationRepository.saveAll(List.of(locHead, locHistory));

            List<FindingLocationEntity> allLocations = locationRepository.findByFindingId(finding.getId());
            assertThat(allLocations).hasSize(2);

            List<FindingLocationEntity> headLocations = locationRepository
                    .findByFindingIdAndIsCurrentHeadTrue(finding.getId());
            assertThat(headLocations).hasSize(1);
            assertThat(headLocations.get(0).getFilePath()).isEqualTo("src/main/resources/application.properties");
        }
    }
}
