package com.scanpilot.scanner.issue;

import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Finding Issue Template Service Tests")
class FindingIssueTemplateServiceTest {

    private FindingIssueTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new FindingIssueTemplateService();
    }

    @Test
    @DisplayName("GIVEN relative path WHEN sanitizing THEN preserves relative path")
    void testRelativePathPreserved() {
        assertThat(templateService.sanitizePath("src/main/resources/application.yml"))
                .isEqualTo("src/main/resources/application.yml");
        assertThat(templateService.sanitizePath("config/keys.json"))
                .isEqualTo("config/keys.json");
    }

    @Test
    @DisplayName("GIVEN Windows absolute path WHEN sanitizing THEN returns [sanitized-path]")
    void testWindowsAbsolutePathSanitized() {
        assertThat(templateService.sanitizePath("C:\\Users\\admin\\repo\\src\\main\\App.java"))
                .isEqualTo("[sanitized-path]");
        assertThat(templateService.sanitizePath("D:/projects/repo/config.yml"))
                .isEqualTo("[sanitized-path]");
    }

    @Test
    @DisplayName("GIVEN Unix absolute path WHEN sanitizing THEN returns [sanitized-path]")
    void testUnixAbsolutePathSanitized() {
        assertThat(templateService.sanitizePath("/tmp/repo/secret.txt"))
                .isEqualTo("[sanitized-path]");
        assertThat(templateService.sanitizePath("/home/runner/work/repo/src/index.js"))
                .isEqualTo("[sanitized-path]");
    }

    @Test
    @DisplayName("GIVEN UNC path WHEN sanitizing THEN returns [sanitized-path]")
    void testUncPathSanitized() {
        assertThat(templateService.sanitizePath("\\\\server\\share\\secret.txt"))
                .isEqualTo("[sanitized-path]");
    }

    @Test
    @DisplayName("GIVEN path traversal WHEN sanitizing THEN returns [sanitized-path]")
    void testPathTraversalSanitized() {
        assertThat(templateService.sanitizePath("../../etc/passwd"))
                .isEqualTo("[sanitized-path]");
        assertThat(templateService.sanitizePath("src/../secret.txt"))
                .isEqualTo("[sanitized-path]");
    }

    @Test
    @DisplayName("GIVEN URL with credentials WHEN sanitizing THEN returns [sanitized-path]")
    void testCredentialUrlSanitized() {
        assertThat(templateService.sanitizePath("https://user:password123@github.com/repo/file.txt"))
                .isEqualTo("[sanitized-path]");
    }

    @Test
    @DisplayName("GIVEN finding with raw secret in context WHEN generating body THEN contains zero raw secret and only masked evidence")
    void testZeroRawSecretLeakageAndCanonicalStructure() {
        UUID findingId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        String rawSecret = "AKIAIOSFODNN7EXAMPLE";
        String maskedSecret = "AKIA****************";

        FindingEntity finding = FindingEntity.builder()
                .id(findingId)
                .ruleId("SP-CONFIG-001")
                .severity("HIGH")
                .title("AWS Access Key ID Exposure")
                .lifecycle("OPEN")
                .remediationQuality("ACTION_REQUIRED")
                .fingerprint("fp-1234567890")
                .build();

        FindingLocationEntity location = FindingLocationEntity.builder()
                .filePath("config/aws.yml")
                .startLine(42)
                .commitSha("4d4cadf2abcdef")
                .build();

        EvidenceItemEntity evidence = EvidenceItemEntity.builder()
                .evidenceType("AWS Access Key ID")
                .maskedSecret(maskedSecret)
                .redactedSnippet("aws_access_key: " + maskedSecret)
                .build();

        String title = templateService.buildTitle(finding, location.getFilePath());
        String body = templateService.buildBody(finding, location, evidence);

        assertThat(title).isEqualTo("[Security] SP-CONFIG-001: Potential secret exposure in config/aws.yml");
        assertThat(title).doesNotContain(rawSecret);

        // Body Assertions
        assertThat(body).contains("<!-- scan-pilot-finding-id: 3fa85f64-5717-4562-b3fc-2c963f66afa6 -->");
        assertThat(body).contains("### 🛡️ Security Finding: [SP-CONFIG-001] AWS Access Key ID Exposure");
        assertThat(body).contains("**Severity:** `HIGH`");
        assertThat(body).contains("**Status:** `ACTION_REQUIRED`");
        assertThat(body).contains("**Location:** `config/aws.yml` (Line 42)");
        assertThat(body).contains("**Detected Commit:** `4d4cadf`");
        assertThat(body).contains("Type: AWS Access Key ID");
        assertThat(body).contains("Masked: AKIA****************");
        assertThat(body).contains("#### 🛠️ Recommended Remediation Steps");
        assertThat(body).contains("Reported by [Scan Pilot]");

        // Strict security assertion: ZERO raw secret anywhere in title or body
        assertThat(body).doesNotContain(rawSecret);
        // Strict invariant: no clickable markdown URL for file path
        assertThat(body).doesNotContain("[config/aws.yml](");
    }
}
