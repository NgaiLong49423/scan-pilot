package com.scanpilot.github.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scanpilot.github")
public class GitHubAppConfigProperties {

    private String appId = "";
    private String appSlug = "scan-pilot";
    private String appPrivateKey = "";
}
