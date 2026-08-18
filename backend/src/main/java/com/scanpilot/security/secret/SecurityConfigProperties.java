package com.scanpilot.security.secret;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for security components including HMAC fingerprinting.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scanpilot.security")
public class SecurityConfigProperties {

    /**
     * Secret key for HMAC-SHA-256 fingerprint generation.
     */
    private String hmacSecretKey = "default-insecure-dev-hmac-key-for-local-testing-only-32bytes";
}
