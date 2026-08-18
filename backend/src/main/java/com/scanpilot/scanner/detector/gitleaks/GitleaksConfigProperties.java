package com.scanpilot.scanner.detector.gitleaks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Gitleaks detector adapter.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "scanpilot.gitleaks")
public class GitleaksConfigProperties {

    /**
     * Binary path or executable name for Gitleaks.
     * Defaults to "gitleaks" (expected in system PATH or container).
     */
    private String binaryPath = "gitleaks";

    /**
     * Timeout in seconds for Gitleaks process execution.
     */
    private int timeoutSeconds = 60;

    /**
     * Classpath resource path to the trusted Gitleaks policy TOML file.
     */
    private String policyResourcePath = "policies/sp-config-001-gitleaks.toml";
}
