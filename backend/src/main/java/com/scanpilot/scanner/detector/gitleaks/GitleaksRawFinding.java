package com.scanpilot.scanner.detector.gitleaks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw secret finding extracted from Gitleaks JSON report or embedded detection engine.
 * Contains unredacted information maintained strictly within the detector adapter boundary.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitleaksRawFinding(
    @JsonProperty("RuleID") String ruleID,
    @JsonProperty("Description") String description,
    @JsonProperty("StartLine") int startLine,
    @JsonProperty("EndLine") int endLine,
    @JsonProperty("StartColumn") int startColumn,
    @JsonProperty("EndColumn") int endColumn,
    @JsonProperty("Match") String match,
    @JsonProperty("Secret") String secret,
    @JsonProperty("File") String file,
    @JsonProperty("Commit") String commit,
    @JsonProperty("Author") String author,
    @JsonProperty("Email") String email,
    @JsonProperty("Date") String date,
    @JsonProperty("Message") String message,
    @JsonProperty("Fingerprint") String fingerprint
) {}
