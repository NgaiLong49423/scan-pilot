package com.scanpilot.scanner.remediation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringConfigurationPatcherTest {

    private SpringConfigurationPatcher patcher;

    @BeforeEach
    void setUp() {
        patcher = new SpringConfigurationPatcher();
    }

    @Test
    @DisplayName("Identifies supported Spring Boot configuration files")
    void testSupportedFiles() {
        assertThat(patcher.isSupportedConfigFile("application.properties")).isTrue();
        assertThat(patcher.isSupportedConfigFile("src/main/resources/application.properties")).isTrue();
        assertThat(patcher.isSupportedConfigFile("application-prod.properties")).isTrue();
        assertThat(patcher.isSupportedConfigFile("application-staging.yml")).isTrue();
        assertThat(patcher.isSupportedConfigFile("application.yaml")).isTrue();

        assertThat(patcher.isSupportedConfigFile("Application.java")).isFalse();
        assertThat(patcher.isSupportedConfigFile("config.json")).isFalse();
        assertThat(patcher.isSupportedConfigFile("pom.xml")).isFalse();
        assertThat(patcher.isSupportedConfigFile("other.yml")).isFalse();
        assertThat(patcher.isSupportedConfigFile(".env")).isFalse();
    }

    @Test
    @DisplayName("Derives standardized uppercase environment variable names")
    void testDeriveEnvVarName() {
        assertThat(patcher.deriveEnvVarName("spring.datasource.password")).isEqualTo("SPRING_DATASOURCE_PASSWORD");
        assertThat(patcher.deriveEnvVarName("aws.secret-key")).isEqualTo("AWS_SECRET_KEY");
        assertThat(patcher.deriveEnvVarName("jwt_token_secret")).isEqualTo("JWT_TOKEN_SECRET");
        assertThat(patcher.deriveEnvVarName("app.service.apiKey")).isEqualTo("APP_SERVICE_APIKEY");
    }

    @Test
    @DisplayName("Patches properties file single-line secret")
    void testPatchProperties() {
        String content = """
                spring.application.name=demo
                spring.datasource.url=jdbc:postgresql://localhost:5432/db
                spring.datasource.password=superSecret123
                server.port=8080
                """;

        SpringConfigurationPatcher.PatchResult result = patcher.createPatch(
                "src/main/resources/application.properties",
                content,
                3,
                null
        );

        assertThat(result.success()).isTrue();
        assertThat(result.envVariableName()).isEqualTo("SPRING_DATASOURCE_PASSWORD");
        assertThat(result.originalLineMasked()).isEqualTo("spring.datasource.password=***");
        assertThat(result.patchedLine()).isEqualTo("spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}");
        assertThat(result.patchedContent()).contains("spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}");
        assertThat(result.patchedContent()).doesNotContain("superSecret123");
    }

    @Test
    @DisplayName("Patches YAML file with reconstructed hierarchy")
    void testPatchYamlHierarchy() {
        String yamlContent = """
                spring:
                  datasource:
                    url: jdbc:postgresql://localhost/test
                    password: myRootPassword
                  jpa:
                    hibernate:
                      ddl-auto: validate
                """;

        SpringConfigurationPatcher.PatchResult result = patcher.createPatch(
                "src/main/resources/application.yml",
                yamlContent,
                4,
                null
        );

        assertThat(result.success()).isTrue();
        assertThat(result.envVariableName()).isEqualTo("SPRING_DATASOURCE_PASSWORD");
        assertThat(result.originalLineMasked()).isEqualTo("    password: ***");
        assertThat(result.patchedLine()).isEqualTo("    password: ${SPRING_DATASOURCE_PASSWORD}");
        assertThat(result.patchedContent()).contains("    password: ${SPRING_DATASOURCE_PASSWORD}");
        assertThat(result.patchedContent()).doesNotContain("myRootPassword");
    }

    @Test
    @DisplayName("Fails closed with MANUAL_REMEDIATION_REQUIRED for unsupported files")
    void testFailClosedOnUnsupportedFile() {
        String javaContent = "public class App { String key = \"secret\"; }";
        SpringConfigurationPatcher.PatchResult result = patcher.createPatch(
                "src/main/java/App.java",
                javaContent,
                1,
                null
        );

        assertThat(result.success()).isFalse();
        assertThat(result.failureCode()).isEqualTo("MANUAL_REMEDIATION_REQUIRED");
    }

    @Test
    @DisplayName("Fails closed on out-of-bounds line number")
    void testOutOfBoundsLine() {
        String content = "key=value";
        SpringConfigurationPatcher.PatchResult result = patcher.createPatch(
                "application.properties",
                content,
                99,
                null
        );

        assertThat(result.success()).isFalse();
        assertThat(result.failureCode()).isEqualTo("MANUAL_REMEDIATION_REQUIRED");
    }
}