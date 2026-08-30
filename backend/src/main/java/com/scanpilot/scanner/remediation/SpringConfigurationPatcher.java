package com.scanpilot.scanner.remediation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic patch engine for SP-CONFIG-001 Spring Boot configuration files.
 * Supports exclusively application*.properties, application*.yml, application*.yaml.
 * Fails closed with MANUAL_REMEDIATION_REQUIRED for any unsupported, multiline, or ambiguous cases.
 */
@Slf4j
@Component
public class SpringConfigurationPatcher {

    public static final String SUPPORTED_FILES_REGEX = "^(.*/)?application(-[a-zA-Z0-9_.-]+)?\\.(properties|yml|yaml)$";
    private static final Pattern SUPPORTED_FILES_PATTERN = Pattern.compile(SUPPORTED_FILES_REGEX, Pattern.CASE_INSENSITIVE);

    private static final Pattern PROPERTIES_KV_PATTERN = Pattern.compile("^(\\s*)([a-zA-Z0-9_.-]+)(\\s*[:=]\\s*)(.+)$");
    private static final Pattern YAML_KV_PATTERN = Pattern.compile("^(\\s*)([a-zA-Z0-9_.-]+)(\\s*:\\s*)(['\"]?)(.+?)\\4(\\s*(?:#.*)?)$");
    private static final Pattern YAML_PARENT_KEY_PATTERN = Pattern.compile("^(\\s*)([a-zA-Z0-9_.-]+)\\s*:\\s*(?:#.*)?$");

    public record PatchResult(
        boolean success,
        String patchedContent,
        String originalLineMasked,
        String patchedLine,
        String envVariableName,
        String failureCode
    ) {
        public static PatchResult failed(String failureCode) {
            return new PatchResult(false, null, null, null, null, failureCode);
        }

        public static PatchResult ok(String patchedContent, String originalLineMasked, String patchedLine, String envVariableName) {
            return new PatchResult(true, patchedContent, originalLineMasked, patchedLine, envVariableName, null);
        }
    }

    /**
     * Checks if a file path is a supported Spring Boot configuration file.
     */
    public boolean isSupportedConfigFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        return SUPPORTED_FILES_PATTERN.matcher(filePath.trim().replace('\\', '/')).matches();
    }

    /**
     * Derives a standardized environment variable name from a Spring property path.
     * e.g. "spring.datasource.password" -> "SPRING_DATASOURCE_PASSWORD"
     * e.g. "aws.secret-key" -> "AWS_SECRET_KEY"
     */
    public String deriveEnvVarName(String propertyPath) {
        if (propertyPath == null || propertyPath.isBlank()) {
            return "REMEDIATED_SECRET";
        }
        String cleaned = propertyPath.trim()
                .replaceAll("[^a-zA-Z0-9_.-]", "_")
                .replace('.', '_')
                .replace('-', '_')
                .replaceAll("_+", "_")
                .toUpperCase();

        if (cleaned.startsWith("_")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("_")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isBlank() ? "REMEDIATED_SECRET" : cleaned;
    }

    /**
     * Masks secret literals safely for previews and diff displays without exposing raw credentials.
     */
    public String maskSecret(String literal) {
        if (literal == null || literal.isBlank()) {
            return "***";
        }
        return "***";
    }

    /**
     * Creates an in-memory single-match patch for a target configuration line.
     */
    public PatchResult createPatch(String filePath, String content, int targetLine, String secretSnippet) {
        if (!isSupportedConfigFile(filePath)) {
            log.info("Refusing remediation PR for unsupported file type: {}", filePath);
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        if (content == null || content.isBlank() || targetLine <= 0) {
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        String normalizedContent = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalizedContent.split("\n", -1);

        if (targetLine > lines.length) {
            log.warn("Target line {} out of bounds for file {} (total lines: {})", targetLine, filePath, lines.length);
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        String targetLineContent = lines[targetLine - 1];
        String normalizedPath = filePath.toLowerCase().replace('\\', '/');

        if (normalizedPath.endsWith(".properties")) {
            return patchPropertiesLine(lines, targetLine - 1, targetLineContent);
        } else if (normalizedPath.endsWith(".yml") || normalizedPath.endsWith(".yaml")) {
            return patchYamlLine(lines, targetLine - 1, targetLineContent);
        }

        return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
    }

    private PatchResult patchPropertiesLine(String[] lines, int lineIndex, String lineContent) {
        // Multiline continuation with trailing backslash is complex -> fail closed
        if (lineContent.endsWith("\\") || (lineIndex > 0 && lines[lineIndex - 1].endsWith("\\"))) {
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        Matcher matcher = PROPERTIES_KV_PATTERN.matcher(lineContent);
        if (!matcher.matches()) {
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        String indent = matcher.group(1);
        String key = matcher.group(2);
        String delimiter = matcher.group(3);
        String value = matcher.group(4);

        if (value.isBlank()) {
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        String envVarName = deriveEnvVarName(key);
        String originalLineMasked = indent + key + delimiter + maskSecret(value);
        String patchedLine = indent + key + delimiter + "${" + envVarName + "}";

        lines[lineIndex] = patchedLine;
        String patchedContent = String.join("\n", lines);

        return PatchResult.ok(patchedContent, originalLineMasked, patchedLine, envVarName);
    }

    private PatchResult patchYamlLine(String[] lines, int lineIndex, String lineContent) {
        // Disallow YAML anchors, aliases, multiline indicators, or comments-only lines
        if (lineContent.contains("&") || lineContent.contains("*") || lineContent.trim().endsWith("|") || lineContent.trim().endsWith(">")) {
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        Matcher matcher = YAML_KV_PATTERN.matcher(lineContent);
        if (!matcher.matches()) {
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        String indent = matcher.group(1);
        String key = matcher.group(2);
        String delimiter = matcher.group(3);
        String value = matcher.group(5);
        String trailingComment = matcher.group(6) != null ? matcher.group(6) : "";

        if (value.isBlank()) {
            return PatchResult.failed("MANUAL_REMEDIATION_REQUIRED");
        }

        // Reconstruct YAML hierarchy by inspecting parent line indentations
        List<String> hierarchy = new ArrayList<>();
        hierarchy.add(key);
        int currentIndentLen = indent.length();

        for (int i = lineIndex - 1; i >= 0; i--) {
            String prevLine = lines[i];
            if (prevLine.isBlank() || prevLine.trim().startsWith("#")) {
                continue;
            }

            Matcher parentMatcher = YAML_PARENT_KEY_PATTERN.matcher(prevLine);
            if (parentMatcher.matches()) {
                int parentIndentLen = parentMatcher.group(1).length();
                if (parentIndentLen < currentIndentLen) {
                    hierarchy.add(parentMatcher.group(2));
                    currentIndentLen = parentIndentLen;
                    if (currentIndentLen == 0) {
                        break;
                    }
                }
            }
        }

        Collections.reverse(hierarchy);
        String fullPropertyPath = String.join(".", hierarchy);
        String envVarName = deriveEnvVarName(fullPropertyPath);

        String originalLineMasked = indent + key + delimiter + maskSecret(value) + trailingComment;
        String patchedLine = indent + key + delimiter + "${" + envVarName + "}" + trailingComment;

        lines[lineIndex] = patchedLine;
        String patchedContent = String.join("\n", lines);

        return PatchResult.ok(patchedContent, originalLineMasked, patchedLine, envVarName);
    }
}