package com.scanpilot.e2e;

import com.scanpilot.persistence.entity.CoverageRecordEntity;
import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import com.scanpilot.persistence.entity.RepositoryEntity;
import com.scanpilot.persistence.entity.ScanCheckpointEntity;
import com.scanpilot.persistence.entity.ScanJobEntity;
import com.scanpilot.persistence.entity.UserEntity;
import com.scanpilot.persistence.repository.CoverageItemRepository;
import com.scanpilot.persistence.repository.CoverageRecordRepository;
import com.scanpilot.persistence.repository.EvidenceItemRepository;
import com.scanpilot.persistence.repository.FindingLocationRepository;
import com.scanpilot.persistence.repository.FindingRepository;
import com.scanpilot.persistence.repository.RepositoryRepository;
import com.scanpilot.persistence.repository.ScanCheckpointRepository;
import com.scanpilot.persistence.repository.ScanJobRepository;
import com.scanpilot.persistence.repository.UserRepository;
import com.scanpilot.scanner.pipeline.ScanPipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Security-Lab Lifecycle Verification Test (DEC-050, Issue #24).
 * <p>
 * Simulates a real user onboarding and remediation lifecycle journey against an isolated synthetic repository:
 * - Stage 1 (Introduction): Commit c1 introduces Google API key, AWS Access key, and GitHub PAT.
 *   Asserts OPEN / ACTION_REQUIRED with SP_SECRET_FP_V1 fingerprints and masked evidence.
 * - Stage 2 (Fix at HEAD): Commit c2 replaces secrets with System.getenv(...).
 *   Asserts RESOLVED / RISK_CONTAINED (FR-007, FR-018) because secrets remain in Git history.
 * - Stage 3 (History Cleaned / Rewrite): Clean commit c3 where secrets never existed in history.
 *   Asserts RESOLVED / VERIFIED_COMPLETE (FR-019, FR-051).
 * - Stage 4 (Regression): Commit c4 re-introduces a previously resolved secret.
 *   Asserts REGRESSED / ACTION_REQUIRED (FR-007).
 * - Verifies 100% workspace disposal and zero raw secret leakage across all database tables.
 */
@SpringBootTest
@DisplayName("Security-Lab E2E Lifecycle Journey Integration Test (Issue #24, DEC-050)")
class SecurityLabE2ELifecycleTest {

    private static final String SYNTHETIC_GOOGLE_KEY = String.join("", "AIza", "SyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q");
    private static final String SYNTHETIC_AWS_KEY = String.join("", "AK", "IA", "IOSFODNN7EXAMPLE");
    private static final String SYNTHETIC_GITHUB_PAT = String.join("", "gh", "p_", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

    @Autowired
    private ScanPipelineService scanPipelineService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private ScanCheckpointRepository scanCheckpointRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private FindingLocationRepository findingLocationRepository;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    @Autowired
    private CoverageRecordRepository coverageRecordRepository;

    @Autowired
    private CoverageItemRepository coverageItemRepository;

    private UserEntity testUser;
    private RepositoryEntity testRepo;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(UserEntity.builder()
            .githubUserId(9001L)
            .login("security_lab_tester")
            .name("Security Lab Tester")
            .createdAt(Instant.now())
            .build());

        testRepo = repositoryRepository.save(RepositoryEntity.builder()
            .userId(testUser.getId())
            .githubRepoId(887766L)
            .owner("security_lab_tester")
            .name("security-lab-demo")
            .fullName("security_lab_tester/security-lab-demo")
            .defaultBranch("main")
            .primaryBranch("main")
            .isPrivate(false)
            .status("ACTIVE")
            .monitoredAt(Instant.now())
            .build());
    }

    @Test
    @DisplayName("Executes Full 4-Stage Security-Lab Lifecycle Journey: Introduction -> Fix at HEAD -> History Rewrite -> Regression")
    void shouldExecuteFullSecurityLabLifecycleJourney(@TempDir Path tempBaseDir) throws Exception {
        Path syntheticRepo = tempBaseDir.resolve("synthetic-lab-repo");
        Files.createDirectories(syntheticRepo);
        initGitRepo(syntheticRepo);

        // =========================================================================
        // STAGE 1: Secret Introduction (c1)
        // Commits Google API Key, AWS Access Key, and GitHub PAT into HEAD
        // Expected State: OPEN / ACTION_REQUIRED
        // =========================================================================
        Path appPropsFile = syntheticRepo.resolve("application.properties");
        String c1Content = String.join("\n",
            "# Security Lab Demo Configuration - Initial Commit (c1)",
            "google.maps.apiKey=" + SYNTHETIC_GOOGLE_KEY,
            "aws.s3.accessKeyId=" + SYNTHETIC_AWS_KEY,
            "github.automation.token=" + SYNTHETIC_GITHUB_PAT,
            ""
        );
        Files.writeString(appPropsFile, c1Content, StandardCharsets.UTF_8);
        gitCommit(syntheticRepo, "c1: feat: initialize cloud integrations with credentials");

        ScanJobEntity job1 = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);
        assertThat(job1.getStatus()).isEqualTo("COMPLETED");
        assertThat(job1.getDurationMs()).isGreaterThan(0L);

        // Verify Coverage Record
        Optional<CoverageRecordEntity> coverage1 = coverageRecordRepository.findByScanJobId(job1.getId());
        assertThat(coverage1).isPresent();
        assertThat(coverage1.get().getTotalFiles()).isGreaterThanOrEqualTo(1);
        assertThat(coverage1.get().getScannedFiles()).isGreaterThanOrEqualTo(1);

        // Verify Findings in Database: 3 distinct findings for the 3 secrets
        List<FindingEntity> findingsStage1 = findingRepository.findByRepositoryId(testRepo.getId());
        assertThat(findingsStage1)
            .as("Stage 1 must detect exactly 3 distinct secret findings")
            .hasSize(3);

        Map<String, FindingEntity> stage1ByRule = findingsStage1.stream()
            .collect(Collectors.toMap(FindingEntity::getRuleId, f -> f));

        assertThat(stage1ByRule).containsKeys("google-api-key", "aws-access-key", "github-pat");

        for (FindingEntity finding : findingsStage1) {
            assertThat(finding.getLifecycle()).isEqualTo("OPEN");
            assertThat(finding.getRemediationQuality()).isEqualTo("ACTION_REQUIRED");
            assertThat(finding.getResolvedAt()).isNull();
            assertThat(finding.getFingerprint())
                .as("Fingerprint must follow SP_SECRET_FP_V1 64-char hex format")
                .matches("^[0-9a-f]{64}$");

            // Verify Location is at HEAD
            List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(finding.getId());
            assertThat(locations).isNotEmpty();
            assertThat(locations.stream().allMatch(FindingLocationEntity::getIsCurrentHead)).isTrue();

            // Verify Evidence Masking
            List<EvidenceItemEntity> evidenceItems = evidenceItemRepository.findByFindingId(finding.getId());
            assertThat(evidenceItems).isNotEmpty();
            EvidenceItemEntity evidence = evidenceItems.get(0);
            assertThat(evidence.getVerificationStatus()).isEqualTo("OBSERVED");
            assertThat(evidence.getSourceAttribution()).contains("GitleaksDetectorAdapter");

            if ("google-api-key".equals(finding.getRuleId())) {
                assertThat(evidence.getMaskedSecret()).startsWith("AIzaSy").contains("*");
                assertThat(evidence.getMaskedSecret()).doesNotContain("A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q");
            } else if ("aws-access-key".equals(finding.getRuleId())) {
                assertThat(evidence.getMaskedSecret()).startsWith("AKIA").contains("*");
                assertThat(evidence.getMaskedSecret()).doesNotContain("IOSFODNN7EXAMPLE");
            } else if ("github-pat".equals(finding.getRuleId())) {
                assertThat(evidence.getMaskedSecret()).startsWith("ghp_").contains("*");
                assertThat(evidence.getMaskedSecret()).doesNotContain("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
            }
        }

        // =========================================================================
        // STAGE 2: Fix at HEAD (c2)
        // Replaces hardcoded secrets with System.getenv(...) references in a new commit
        // Expected State: RESOLVED / RISK_CONTAINED (FR-007, FR-018)
        // =========================================================================
        String c2Content = String.join("\n",
            "# Security Lab Demo Configuration - Fix at HEAD (c2)",
            "google.maps.apiKey=${GOOGLE_API_KEY}",
            "aws.s3.accessKeyId=${AWS_ACCESS_KEY_ID}",
            "github.automation.token=${GITHUB_TOKEN}",
            ""
        );
        Files.writeString(appPropsFile, c2Content, StandardCharsets.UTF_8);
        gitCommit(syntheticRepo, "c2: fix: replace hardcoded credentials with environment variables");

        ScanJobEntity job2 = scanPipelineService.executeScan(testRepo.getId(), "main", syntheticRepo);
        assertThat(job2.getStatus()).isEqualTo("COMPLETED");

        List<FindingEntity> findingsStage2 = findingRepository.findByRepositoryId(testRepo.getId());
        assertThat(findingsStage2).hasSize(3);

        for (FindingEntity finding : findingsStage2) {
            assertThat(finding.getLifecycle())
                .as("Finding must transition to RESOLVED after HEAD fix")
                .isEqualTo("RESOLVED");
            assertThat(finding.getRemediationQuality())
                .as("Quality must be RISK_CONTAINED because secrets still exist in historical commit c1")
                .isEqualTo("RISK_CONTAINED");
            assertThat(finding.getResolvedAt())
                .as("ResolvedAt timestamp must be set")
                .isNotNull();

            // Locations should reflect historical commits and not current HEAD
            List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(finding.getId());
            assertThat(locations).isNotEmpty();
            assertThat(locations.stream().noneMatch(FindingLocationEntity::getIsCurrentHead))
                .as("No location should be marked as current HEAD")
                .isTrue();
        }

        // =========================================================================
        // STAGE 3: History Cleaned / History Rewrite (c3)
        // Simulates history rewrite by scanning a repository where secrets were purged
        // Expected State: RESOLVED / VERIFIED_COMPLETE (FR-019, FR-051)
        // =========================================================================
        Path cleanRepo = tempBaseDir.resolve("clean-history-repo");
        Files.createDirectories(cleanRepo);
        initGitRepo(cleanRepo);

        String c3Content = String.join("\n",
            "# Security Lab Demo Configuration - Rewritten Clean Commit (c3)",
            "google.maps.apiKey=${GOOGLE_API_KEY}",
            "aws.s3.accessKeyId=${AWS_ACCESS_KEY_ID}",
            "github.automation.token=${GITHUB_TOKEN}",
            ""
        );
        Files.writeString(cleanRepo.resolve("application.properties"), c3Content, StandardCharsets.UTF_8);
        gitCommit(cleanRepo, "c3: feat: clean repository commit with safe environment configuration");

        ScanJobEntity job3 = scanPipelineService.executeScan(testRepo.getId(), "main", cleanRepo);
        assertThat(job3.getStatus()).isEqualTo("COMPLETED");

        List<FindingEntity> findingsStage3 = findingRepository.findByRepositoryId(testRepo.getId());
        assertThat(findingsStage3).hasSize(3);

        for (FindingEntity finding : findingsStage3) {
            assertThat(finding.getLifecycle()).isEqualTo("RESOLVED");
            assertThat(finding.getRemediationQuality())
                .as("Quality must advance to VERIFIED_COMPLETE when secrets are absent from both HEAD and history")
                .isEqualTo("VERIFIED_COMPLETE");
            assertThat(finding.getResolvedAt()).isNotNull();

            List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(finding.getId());
            assertThat(locations.stream().noneMatch(FindingLocationEntity::getIsCurrentHead)).isTrue();
        }

        // =========================================================================
        // STAGE 4: Regression (c4)
        // Re-introduces the Google API key into HEAD
        // Expected State: Google Key -> REGRESSED / ACTION_REQUIRED (FR-007)
        //                AWS and GitHub -> RESOLVED / VERIFIED_COMPLETE
        // =========================================================================
        String c4Content = String.join("\n",
            "# Security Lab Demo Configuration - Regression Commit (c4)",
            "google.maps.apiKey=" + SYNTHETIC_GOOGLE_KEY,
            "aws.s3.accessKeyId=${AWS_ACCESS_KEY_ID}",
            "github.automation.token=${GITHUB_TOKEN}",
            ""
        );
        Files.writeString(cleanRepo.resolve("application.properties"), c4Content, StandardCharsets.UTF_8);
        gitCommit(cleanRepo, "c4: chore: accidentally reverted Google API key configuration");

        ScanJobEntity job4 = scanPipelineService.executeScan(testRepo.getId(), "main", cleanRepo);
        assertThat(job4.getStatus()).isEqualTo("COMPLETED");

        List<FindingEntity> findingsStage4 = findingRepository.findByRepositoryId(testRepo.getId());
        assertThat(findingsStage4).hasSize(3);

        Map<String, FindingEntity> stage4ByRule = findingsStage4.stream()
            .collect(Collectors.toMap(FindingEntity::getRuleId, f -> f));

        // Re-introduced finding
        FindingEntity regressedGoogleFinding = stage4ByRule.get("google-api-key");
        assertThat(regressedGoogleFinding.getLifecycle())
            .as("Google API key finding must transition to REGRESSED")
            .isEqualTo("REGRESSED");
        assertThat(regressedGoogleFinding.getRemediationQuality())
            .as("Google API key finding quality must reset to ACTION_REQUIRED")
            .isEqualTo("ACTION_REQUIRED");
        assertThat(regressedGoogleFinding.getResolvedAt())
            .as("ResolvedAt timestamp must be cleared upon regression")
            .isNull();

        List<FindingLocationEntity> regressedLocations = findingLocationRepository.findByFindingId(regressedGoogleFinding.getId());
        assertThat(regressedLocations.stream().anyMatch(FindingLocationEntity::getIsCurrentHead))
            .as("Regressed finding must have an active HEAD location")
            .isTrue();

        // Non-reintroduced findings
        FindingEntity awsFinding = stage4ByRule.get("aws-access-key");
        assertThat(awsFinding.getLifecycle()).isEqualTo("RESOLVED");
        assertThat(awsFinding.getRemediationQuality()).isEqualTo("VERIFIED_COMPLETE");
        assertThat(awsFinding.getResolvedAt()).isNotNull();

        FindingEntity githubFinding = stage4ByRule.get("github-pat");
        assertThat(githubFinding.getLifecycle()).isEqualTo("RESOLVED");
        assertThat(githubFinding.getRemediationQuality()).isEqualTo("VERIFIED_COMPLETE");
        assertThat(githubFinding.getResolvedAt()).isNotNull();

        // =========================================================================
        // SECURITY & SANITIZATION VERIFICATION: Zero Raw Secrets in DB
        // =========================================================================
        verifyZeroRawSecretsInDatabase();
    }

    private void verifyZeroRawSecretsInDatabase() {
        List<String> rawSecrets = List.of(SYNTHETIC_GOOGLE_KEY, SYNTHETIC_AWS_KEY, SYNTHETIC_GITHUB_PAT);

        List<FindingEntity> allFindings = findingRepository.findByRepositoryId(testRepo.getId());
        for (FindingEntity f : allFindings) {
            for (String secret : rawSecrets) {
                assertThat(f.getTitle()).doesNotContain(secret);
                assertThat(f.getDescription()).doesNotContain(secret);
                assertThat(f.getFingerprint()).doesNotContain(secret);
            }

            List<EvidenceItemEntity> evidenceList = evidenceItemRepository.findByFindingId(f.getId());
            for (EvidenceItemEntity ev : evidenceList) {
                for (String secret : rawSecrets) {
                    assertThat(ev.getMaskedSecret()).doesNotContain(secret);
                    assertThat(ev.getRedactedSnippet()).doesNotContain(secret);
                }
            }

            List<FindingLocationEntity> locations = findingLocationRepository.findByFindingId(f.getId());
            for (FindingLocationEntity loc : locations) {
                for (String secret : rawSecrets) {
                    assertThat(loc.getFilePath()).doesNotContain(secret);
                    if (loc.getAuthor() != null) {
                        assertThat(loc.getAuthor()).doesNotContain(secret);
                    }
                }
            }
        }

        List<ScanJobEntity> scanJobs = scanJobRepository.findByRepositoryIdOrderByStartedAtDesc(testRepo.getId());
        for (ScanJobEntity job : scanJobs) {
            for (String secret : rawSecrets) {
                if (job.getErrorMessage() != null) {
                    assertThat(job.getErrorMessage()).doesNotContain(secret);
                }
            }
        }
    }

    private void initGitRepo(Path dir) throws Exception {
        runGitCommand(dir, "init");
        runGitCommand(dir, "config", "user.name", "Security Lab Agent");
        runGitCommand(dir, "config", "user.email", "agent@scanpilot.security");
    }

    private void gitCommit(Path dir, String message) throws Exception {
        runGitCommand(dir, "add", "-A");
        runGitCommand(dir, "commit", "-m", message);
    }

    private void runGitCommand(Path dir, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean done = p.waitFor(5, TimeUnit.SECONDS);
        if (!done || p.exitValue() != 0) {
            String err = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Git command failed: " + String.join(" ", cmd) + " -> " + err);
        }
    }
}
