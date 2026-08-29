package com.scanpilot.migration;

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
@DisplayName("Flyway V6 Migration Verification Tests")
class FlywayV6MigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should successfully apply V6 migration and record schema history")
    void shouldVerifyFlywayV6MigrationApplied() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(Integer.parseInt(current.getVersion().getVersion())).isGreaterThanOrEqualTo(6);
        assertThat(current.getState().isApplied()).isTrue();

        MigrationInfo v6 = java.util.Arrays.stream(flyway.info().applied())
                .filter(m -> "6".equals(m.getVersion().getVersion()))
                .findFirst()
                .orElse(null);

        assertThat(v6).isNotNull();
        assertThat(v6.getDescription()).isEqualTo("add verified installations and states");
        assertThat(v6.getState().isApplied()).isTrue();
    }

    @Test
    @DisplayName("Should verify that installation_states, user_installations and all 16 core tables exist")
    void shouldVerifyAllCoreTablesIncludingV6Exist() throws Exception {
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
                "scan_events",
                "finding_issue_links",
                "installation_states",
                "user_installations"
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
