package com.scanpilot.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Flyway Schema Migration Verification Tests")
class FlywaySchemaMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should successfully apply V4 migration and record schema history")
    void shouldVerifyFlywayMigrationApplied() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("4");
        assertThat(current.getDescription()).isEqualTo("add scan events telemetry");
        assertThat(current.getState().isApplied()).isTrue();
    }

    @Test
    @DisplayName("Should verify that all 13 core tables exist in the database metadata")
    void shouldVerifyAllCoreTablesExist() throws Exception {
        Set<String> expectedTables = Set.of(
                "users",
                "user_sessions",
                "repositories",
                "monitored_branches",
                "scan_jobs",
                "scan_checkpoints",
                "findings",
                "finding_locations",
                "evidence_items",
                "coverage_records",
                "coverage_items",
                "review_requests",
                "scan_events"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }

        assertThat(actualTables).containsAll(expectedTables);
    }
}
