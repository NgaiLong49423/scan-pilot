package com.scanpilot.scanner.issue;

import com.scanpilot.persistence.entity.EvidenceItemEntity;
import com.scanpilot.persistence.entity.FindingEntity;
import com.scanpilot.persistence.entity.FindingLocationEntity;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for generating secret-safe, sanitized canonical markdown templates for GitHub Issues.
 */
@Service
public class FindingIssueTemplateService {

    public static final String MARKER_PREFIX = "<!-- scan-pilot-finding-id: ";
    public static final String MARKER_SUFFIX = " -->";

    private static final Pattern DRIVE_OR_UNC_PATTERN = Pattern.compile("^(?:[a-zA-Z]:|\\\\\\\\)");
    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");

    /**
     * Sanitizes a file path to ensure it is strictly repository-relative and free of credentials,
     * absolute host paths, UNC paths, or traversal vectors.
     */
    public String sanitizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "unknown/file";
        }

        String path = rawPath.trim();

        // Check for URL schemes or credential-bearing URLs
        if (URL_SCHEME_PATTERN.matcher(path).find()) {
            return "[sanitized-path]";
        }

        // Check for Windows drive letters (C:\, D:/) or UNC paths (\\server\share)
        if (DRIVE_OR_UNC_PATTERN.matcher(path).find()) {
            return "[sanitized-path]";
        }

        // Check for leading Unix slash (/home/user, /tmp)
        if (path.startsWith("/") || path.startsWith("\\")) {
            return "[sanitized-path]";
        }

        // Check for path traversal (..)
        if (path.contains("..")) {
            return "[sanitized-path]";
        }

        // Normalize backslashes to forward slashes for relative paths
        path = path.replace('\\', '/');

        // Check if segments are clean
        for (String segment : path.split("/")) {
            if (segment.equals(".") || segment.equals("..")) {
                return "[sanitized-path]";
            }
        }

        if (path.isBlank()) {
            return "unknown/file";
        }

        return path;
    }

    /**
     * Constructs the canonical issue title.
     */
    public String buildTitle(FindingEntity finding, String rawFilePath) {
        String ruleId = (finding != null && finding.getRuleId() != null && !finding.getRuleId().isBlank())
                ? finding.getRuleId().trim()
                : "SP-CONFIG-001";
        String sanitizedPath = sanitizePath(rawFilePath);
        return String.format("[Security] %s: Potential secret exposure in %s", ruleId, sanitizedPath);
    }

    /**
     * Constructs the canonical issue markdown body with zero raw secret leakage and repository-relative paths.
     */
    public String buildBody(FindingEntity finding, FindingLocationEntity location, EvidenceItemEntity evidence) {
        String findingIdStr = (finding != null && finding.getId() != null) ? finding.getId().toString() : "unknown";
        String ruleId = (finding != null && finding.getRuleId() != null && !finding.getRuleId().isBlank())
                ? finding.getRuleId().trim()
                : "SP-CONFIG-001";
        String title = (finding != null && finding.getTitle() != null && !finding.getTitle().isBlank())
                ? finding.getTitle().trim()
                : "Source Code Secret Exposure";
        String severity = (finding != null && finding.getSeverity() != null && !finding.getSeverity().isBlank())
                ? finding.getSeverity().trim()
                : "HIGH";
        String status = (finding != null && finding.getRemediationQuality() != null && !finding.getRemediationQuality().isBlank())
                ? finding.getRemediationQuality().trim()
                : (finding != null && finding.getLifecycle() != null ? finding.getLifecycle().trim() : "ACTION_REQUIRED");

        String rawPath = location != null ? location.getFilePath() : null;
        String sanitizedPath = sanitizePath(rawPath);
        String locationLine = (location != null && location.getStartLine() != null)
                ? String.format("`%s` (Line %d)", sanitizedPath, location.getStartLine())
                : String.format("`%s`", sanitizedPath);

        String rawCommit = location != null ? location.getCommitSha() : null;
        String commitSha = (rawCommit != null && !rawCommit.isBlank())
                ? (rawCommit.length() > 7 ? rawCommit.substring(0, 7) : rawCommit)
                : "HEAD";

        String evidenceType = (evidence != null && evidence.getEvidenceType() != null && !evidence.getEvidenceType().isBlank())
                ? evidence.getEvidenceType().trim()
                : ruleId;

        String maskedSecret = (evidence != null && evidence.getMaskedSecret() != null && !evidence.getMaskedSecret().isBlank())
                ? evidence.getMaskedSecret().trim()
                : (finding != null && finding.getFingerprint() != null && finding.getFingerprint().length() > 8
                    ? finding.getFingerprint().substring(0, 6) + "************"
                    : "************");

        StringBuilder sb = new StringBuilder();
        sb.append(MARKER_PREFIX).append(findingIdStr).append(MARKER_SUFFIX).append("\n");
        sb.append("### 🛡️ Security Finding: [").append(ruleId).append("] ").append(title).append("\n\n");
        sb.append("**Severity:** `").append(severity).append("`  \n");
        sb.append("**Status:** `").append(status).append("`  \n");
        sb.append("**Location:** ").append(locationLine).append("  \n");
        sb.append("**Detected Commit:** `").append(commitSha).append("`  \n\n");
        sb.append("---\n\n");
        sb.append("#### 🔍 Masked Evidence\n");
        sb.append("```text\n");
        sb.append("Type: ").append(evidenceType).append("\n");
        sb.append("Masked: ").append(maskedSecret).append("\n");
        sb.append("```\n");
        sb.append("> ⚠️ **Security Notice:** Raw secret keys, source code lines, and absolute system paths are intentionally redacted to prevent credential disclosure.\n\n");
        sb.append("---\n\n");
        sb.append("#### 🛠️ Recommended Remediation Steps\n");
        sb.append("1. **Rotate Credential:** Immediately invalidate, rotate, or revoke the exposed secret in the provider console.\n");
        sb.append("2. **Remove from Repository:** Remove the secret from source code and replace it with environment variables or a secure secret manager.\n");
        sb.append("3. **Audit Commit History:** Ensure historical commits containing the key are rotated or purged.\n\n");
        sb.append("---\n");
        sb.append("*Reported by [Scan Pilot](https://github.com/NgaiLong49423/scan-pilot) — Continuous Health & Security Monitoring for AI Software.*");

        return sb.toString();
    }
}
