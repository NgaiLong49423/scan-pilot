package com.scanpilot.config;

import com.scanpilot.security.secret.SecurityConfigProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionDatasourceStartupValidatorTest {

    @Mock
    private Environment environment;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Nested
    @DisplayName("Production Profile ('prod') Fail-Closed Validations")
    class ProductionProfileTests {

        @Test
        @DisplayName("GIVEN active profile 'prod' WHEN datasource is H2 THEN throws IllegalStateException")
        void testProdProfileWithH2DatasourceThrowsIllegalStateException() throws SQLException {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getDatabaseProductName()).thenReturn("H2");

            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(environment, dataSource);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateOnStartup);
            assertThat(ex.getMessage())
                    .contains("Production startup failed: Active profile 'prod' strictly mandates a configured PostgreSQL datasource. Detected: H2");
        }

        @Test
        @DisplayName("GIVEN active profile 'prod' WHEN datasource is PostgreSQL THEN succeeds without exception")
        void testProdProfileWithPostgreSqlDatasourcePasses() throws SQLException {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(environment, dataSource);

            assertDoesNotThrow(validator::validateOnStartup);
        }

        @Test
        @DisplayName("GIVEN active profile 'prod' WHEN database is MySQL THEN throws IllegalStateException")
        void testProdProfileWithMySQLDatasourceThrowsIllegalStateException() throws SQLException {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(metaData);
            when(metaData.getDatabaseProductName()).thenReturn("MySQL");

            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(environment, dataSource);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateOnStartup);
            assertThat(ex.getMessage())
                    .contains("Production startup failed: Active profile 'prod' strictly mandates a configured PostgreSQL datasource. Detected: MySQL");
        }

        @Test
        @DisplayName("GIVEN active profile 'prod' WHEN datasource is null THEN throws IllegalStateException")
        void testProdProfileWithNullDatasourceThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(environment, null);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateOnStartup);
            assertThat(ex.getMessage())
                    .contains("Production startup failed: Active profile 'prod' strictly mandates a configured PostgreSQL datasource. Detected: null");
        }

        @Test
        @DisplayName("GIVEN active profile 'prod' WHEN connection fails THEN throws IllegalStateException without exposing raw credentials")
        void testProdProfileWithConnectionFailureThrowsIllegalStateExceptionWithoutSecrets() throws SQLException {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            when(dataSource.getConnection()).thenThrow(new SQLException("FATAL: password authentication failed for user super_secret_user"));

            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(environment, dataSource);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateOnStartup);
            assertThat(ex.getMessage())
                    .contains("Production startup failed: Active profile 'prod' strictly mandates a configured PostgreSQL datasource. Detected: connection error (SQLException)")
                    .doesNotContain("super_secret_user")
                    .doesNotContain("password authentication failed");
        }
    }

    @Nested
    @DisplayName("Non-Production Profiles ('dev', 'test', default) Skip Validation")
    class NonProductionProfileTests {

        @Test
        @DisplayName("GIVEN active profile 'dev' WHEN validating THEN skips datasource check")
        void testDevProfileSkipsValidation() throws SQLException {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(environment, dataSource);

            assertDoesNotThrow(validator::validateOnStartup);
            verify(dataSource, never()).getConnection();
        }

        @Test
        @DisplayName("GIVEN active profile 'test' WHEN validating THEN skips datasource check")
        void testTestProfileSkipsValidation() throws SQLException {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(environment, dataSource);

            assertDoesNotThrow(validator::validateOnStartup);
            verify(dataSource, never()).getConnection();
        }

        @Test
        @DisplayName("GIVEN no active profiles (default) WHEN validating THEN skips datasource check")
        void testDefaultProfileSkipsValidation() throws SQLException {
            when(environment.getActiveProfiles()).thenReturn(new String[]{});

            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(environment, dataSource);

            assertDoesNotThrow(validator::validateOnStartup);
            verify(dataSource, never()).getConnection();
        }

        @Test
        @DisplayName("GIVEN null environment WHEN validating THEN skips datasource check")
        void testNullEnvironmentSkipsValidation() throws SQLException {
            ProductionDatasourceStartupValidator validator =
                    new ProductionDatasourceStartupValidator(null, dataSource);

            assertDoesNotThrow(validator::validateOnStartup);
            verify(dataSource, never()).getConnection();
        }
    }

    @Nested
    @DisplayName("Production HMAC Secret Key Fail-Closed & Profile Mapping Validations")
    class ProductionHmacSecretConfigTests {

        private static final String DEFAULT_DEV_HMAC_KEY = "default-insecure-dev-hmac-key-for-local-testing-only-32bytes";

        @Configuration
        @EnableConfigurationProperties(SecurityConfigProperties.class)
        static class TestSecurityConfig {
        }

        @Test
        @DisplayName("GIVEN active profile 'prod' without SCANPILOT_HMAC_SECRET_KEY WHEN resolving property THEN fails closed with IllegalArgumentException")
        void testProdProfileWithoutHmacSecretThrowsException() {
            try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(TestSecurityConfig.class)
                    .web(WebApplicationType.NONE)
                    .profiles("prod")
                    .run()) {

                Environment env = ctx.getEnvironment();
                assertThat(env.getActiveProfiles()).contains("prod");
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                        env.getRequiredProperty("scanpilot.security.hmac-secret-key")
                );
                assertThat(ex.getMessage()).contains("Could not resolve placeholder 'SCANPILOT_HMAC_SECRET_KEY'");
            }
        }

        @Test
        @DisplayName("GIVEN active profile 'prod' with SCANPILOT_HMAC_SECRET_KEY WHEN starting THEN loads provided key and never inherits default dev key")
        void testProdProfileWithHmacSecretLoadsProvidedKeyAndNeverInheritsDefault() {
            String customProdKey = "super-secret-prod-hmac-key-for-production-use-only-32bytes";

            try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(TestSecurityConfig.class)
                    .web(WebApplicationType.NONE)
                    .profiles("prod")
                    .properties("SCANPILOT_HMAC_SECRET_KEY=" + customProdKey)
                    .run()) {

                SecurityConfigProperties props = ctx.getBean(SecurityConfigProperties.class);
                assertThat(props.getHmacSecretKey())
                        .isEqualTo(customProdKey)
                        .isNotEqualTo(DEFAULT_DEV_HMAC_KEY);
            }
        }

        @Test
        @DisplayName("GIVEN default/dev profile WHEN starting without SCANPILOT_HMAC_SECRET_KEY THEN loads default dev key safely")
        void testDevProfileLoadsDefaultDevKey() {
            try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(TestSecurityConfig.class)
                    .web(WebApplicationType.NONE)
                    .profiles("dev")
                    .run()) {

                SecurityConfigProperties props = ctx.getBean(SecurityConfigProperties.class);
                assertThat(props.getHmacSecretKey()).isEqualTo(DEFAULT_DEV_HMAC_KEY);
            }
        }
    }

    @Nested
    @DisplayName("Cloud SQL Postgres Socket Factory Classpath Verification")
    class CloudSqlSocketFactoryClasspathTests {

        @Test
        @DisplayName("GIVEN Cloud SQL postgres-socket-factory dependency WHEN checking classpath THEN SocketFactory class is present")
        void testCloudSqlPostgresSocketFactoryPresentOnClasspath() {
            assertDoesNotThrow(() -> {
                Class<?> clazz = Class.forName("com.google.cloud.sql.postgres.SocketFactory");
                assertThat(clazz).isNotNull();
            });
        }
    }
}
