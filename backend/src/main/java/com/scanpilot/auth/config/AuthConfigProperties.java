package com.scanpilot.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scanpilot.auth")
public class AuthConfigProperties {

    public static final String DEFAULT_SESSION_COOKIE_NAME = "SCANPILOT_SESSION";

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "http://localhost:8080/api/v1/auth/github/callback";
    private String frontendUrl = "http://localhost:5173";
    private boolean cookieSecure = false;
    private long sessionTtlSeconds = 604800; // 7 days
    private long stateTtlSeconds = 600; // 10 minutes
    private String cookieName = DEFAULT_SESSION_COOKIE_NAME;
    private String scopes = ""; // Empty by default for GitHub App user-to-server auth
}
