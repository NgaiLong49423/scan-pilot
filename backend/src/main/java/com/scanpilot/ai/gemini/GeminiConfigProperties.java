package com.scanpilot.ai.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Gemini AI Explanation Service (FR-005, FR-048).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "scanpilot.ai.gemini")
public class GeminiConfigProperties {

    /**
     * Google Gemini REST API key.
     */
    private String apiKey = "";

    /**
     * Gemini model to use for explanation and remediation guidance.
     * Defaults to "gemini-1.5-flash".
     */
    private String model = "gemini-1.5-flash";

    /**
     * Base URL for Google GenAI REST API.
     */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    /**
     * Timeout in seconds for HTTP requests to Gemini API.
     */
    private int timeoutSeconds = 15;

    /**
     * Flag indicating whether Gemini AI integration is enabled.
     */
    private boolean enabled = true;

    /**
     * TTL in seconds for in-memory explanation cache.
     */
    private long cacheTtlSeconds = 3600;
}
