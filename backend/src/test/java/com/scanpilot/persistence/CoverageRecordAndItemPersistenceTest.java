package com.scanpilot.persistence;

import com.scanpilot.persistence.entity.CoverageItemEntity;
import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Coverage Record and Item Persistence Tests")
class CoverageRecordAndItemPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private CoverageRecordRepository coverageRecordRepository;

    @Autowired
    private CoverageItemRepository coverageItemRepository;

    private RepositoryEntity testRepo;
    private ScanJobEntity testJob;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(UserEntity.builder()
                .githubUserId(5001L)
                .login("coverage_owner")
                .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
                .userId(user.getId())
                .githubRepoId(55001L)
                .fullName("coverage_owner/large-mono-repo")
                .build());

        testJob = scanJobRepository.save(ScanJobEntity.builder()
                .repositoryId(testRepo.getId())
                .branchName("main")
                .scanMode("SNAPSHOT")
                .status("COMPLETED")
                .build());
    }

    @Test
    @DisplayName("Should persist coverage records with summary stats and breakdown items")
    void shouldPersistCoverageSummaryAndItems() {
        CoverageRecordEntity record = CoverageRecordEntity.builder()
                .scanJobId(testJob.getId())
                .repositoryId(testRepo.getId())
                .branchName("main")
                .totalFiles(100)
                .scannedFiles(92)
                .skippedFiles(8)
                .textFiles(92)
                .binaryFiles(6)
                .undeterminedFiles(2)
                .totalBytes(52428800L)
                .coverageImpact("INCOMPLETE")
                .reasonCode("REPOSITORY_TOO_LARGE")
                .limitHitValue(20971520L)
                .createdAt(Instant.now())
                .build();

        CoverageRecordEntity savedRecord = coverageRecordRepository.save(record);
        assertThat(savedRecord.getId()).isNotNull();
        assertThat(savedRecord.getReasonCode()).isEqualTo("REPOSITORY_TOO_LARGE");
        assertThat(savedRecord.getLimitHitValue()).isEqualTo(20971520L);

        CoverageItemEntity scannedItem = CoverageItemEntity.builder()
                .coverageRecordId(savedRecord.getId())
                .filePath("src/main/java/App.java")
                .classification("TEXT")
                .sizeBytes(2048L)
                .status("SCANNED")
                .reasonCode("ELIGIBLE_TEXT")
                .impact("NONE")
                .build();

        CoverageItemEntity skippedBinary = CoverageItemEntity.builder()
                .coverageRecordId(savedRecord.getId())
                .filePath("docs/manual.pdf")
                .classification("BINARY")
                .sizeBytes(15728640L)
                .status("SKIPPED")
                .reasonCode("UNSUPPORTED_BINARY_DOCUMENT")
                .impact("LOW")
                .details("PDF document unsupported in MVP")
                .build();

        CoverageItemEntity skippedSizeLimit = CoverageItemEntity.builder()
                .coverageRecordId(savedRecord.getId())
                .filePath("large_dump.sql")
                .classification("TEXT")
                .sizeBytes(62914560L)
                .status("SKIPPED")
                .reasonCode("RELEASE_FILE_SIZE_CEILING_EXCEEDED")
                .impact("HIGH")
                .details("File exceeds 50 MiB ceiling")
                .build();

        coverageItemRepository.saveAll(List.of(scannedItem, skippedBinary, skippedSizeLimit));

        Optional<CoverageRecordEntity> foundRecord = coverageRecordRepository.findByScanJobId(testJob.getId());
        assertThat(foundRecord).isPresent();
        assertThat(foundRecord.get().getTotalFiles()).isEqualTo(100);
        assertThat(foundRecord.get().getSkippedFiles()).isEqualTo(8);
        assertThat(foundRecord.get().getReasonCode()).isEqualTo("REPOSITORY_TOO_LARGE");
        assertThat(foundRecord.get().getLimitHitValue()).isEqualTo(20971520L);

        List<CoverageItemEntity> items = coverageItemRepository.findByCoverageRecordId(savedRecord.getId());
        assertThat(items).hasSize(3);

        List<CoverageItemEntity> skippedItems = coverageItemRepository
                .findByCoverageRecordIdAndStatus(savedRecord.getId(), "SKIPPED");
        assertThat(skippedItems).hasSize(2);
    }
}
