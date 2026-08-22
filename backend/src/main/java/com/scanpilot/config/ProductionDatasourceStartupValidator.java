package com.scanpilot.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Startup validator ensuring the 'prod' profile runs exclusively against a verified PostgreSQL datasource.
 * Fails closed immediately upon context startup if any non-PostgreSQL (e.g. H2) or inaccessible datasource is detected.
 * Strictly avoids logging or leaking any credentials, tokens, or connection strings.
 */
@Component
public class ProductionDatasourceStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionDatasourceStartupValidator.class);
    private static final String PROD_PROFILE = "prod";
    private static final String POSTGRESQL_KEYWORD = "postgresql";

    private final Environment environment;
    private final DataSource dataSource;

    public ProductionDatasourceStartupValidator(Environment environment, DataSource dataSource) {
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void validateOnStartup() {
        if (isProdProfileActive()) {
            validateProductionDatasource();
        }
    }

    public boolean isProdProfileActive() {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> PROD_PROFILE.equalsIgnoreCase(p.trim()));
    }

    public void validateProductionDatasource() {
        if (dataSource == null) {
            throw new IllegalStateException(
                    "Production startup failed: Active profile 'prod' strictly mandates a configured PostgreSQL datasource. Detected: null");
        }

        String detectedProduct = null;
        try (Connection conn = dataSource.getConnection()) {
            if (conn == null) {
                throw new IllegalStateException(
                        "Production startup failed: Active profile 'prod' strictly mandates a configured PostgreSQL datasource. Detected: null connection");
            }
            DatabaseMetaData metaData = conn.getMetaData();
            if (metaData != null) {
                detectedProduct = metaData.getDatabaseProductName();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Production startup failed: Active profile 'prod' strictly mandates a configured PostgreSQL datasource. Detected: connection error (" + e.getClass().getSimpleName() + ")");
        }

        if (detectedProduct == null || !detectedProduct.toLowerCase(Locale.ROOT).contains(POSTGRESQL_KEYWORD)) {
            throw new IllegalStateException(
                    "Production startup failed: Active profile 'prod' strictly mandates a configured PostgreSQL datasource. Detected: " + detectedProduct);
        }

        log.info("Production PostgreSQL datasource successfully verified. Detected product: {}", detectedProduct);
    }
}
