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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("Flyway V8 Migration Verification Tests")
class FlywayV8MigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should successfully apply V8 migration and record schema history")
    void shouldVerifyFlywayV8MigrationApplied() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(Integer.parseInt(current.getVersion().getVersion())).isGreaterThanOrEqualTo(8);
        assertThat(current.getState().isApplied()).isTrue();

        MigrationInfo v8 = java.util.Arrays.stream(flyway.info().applied())
                .filter(m -> "8".equals(m.getVersion().getVersion()))
                .findFirst()
                .orElse(null);

        assertThat(v8).isNotNull();
        assertThat(v8.getDescription()).isEqualTo("add webhook scan dispatch links");
        assertThat(v8.getState().isApplied()).isTrue();
    }

    @Test
    @DisplayName("Should verify that scan_jobs table contains V8 columns")
    void shouldVerifyV8ColumnsInScanJobs() throws Exception {
        Set<String> columns = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "SCAN_JOBS", null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            if (columns.isEmpty()) {
                try (ResultSet rs = meta.getColumns(null, null, "scan_jobs", null)) {
                    while (rs.next()) {
                        columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }
        }

        assertThat(columns).contains(
                "webhook_delivery_id",
                "trigger_type",
                "expected_commit_sha",
                "pr_number"
        );
    }

    @Test
    @DisplayName("Should allow multiple NULL webhook_delivery_id but reject duplicate non-null delivery IDs (Portable Uniqueness)")
    void testV8UniqueDeliveryConstraintAllowsMultipleNullsAndRejectsDuplicates() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID repoId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        try (Connection conn = dataSource.getConnection()) {
            // Setup repository and delivery records first for foreign keys
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (id, github_user_id, login, created_at) VALUES (?, ?, ?, ?)")) {
                ps.setObject(1, userId);
                ps.setLong(2, 999111L);
                ps.setString(3, "test-user-v8");
                ps.setTimestamp(4, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO repositories (id, user_id, github_repo_id, full_name, status, updated_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setObject(1, repoId);
                ps.setObject(2, userId);
                ps.setLong(3, 888111L);
                ps.setString(4, "owner/v8-repo");
                ps.setString(5, "ACTIVE");
                ps.setTimestamp(6, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO webhook_deliveries (id, delivery_id, event_type, status, reason_code, received_at, processed_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setObject(1, deliveryId);
                ps.setString(2, "del-v8-test");
                ps.setString(3, "push");
                ps.setString(4, "ACCEPTED");
                ps.setString(5, "OK");
                ps.setTimestamp(6, Timestamp.from(Instant.now()));
                ps.setTimestamp(7, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }

            // 1. Insert multiple scan_jobs with NULL webhook_delivery_id (Manual scans) - MUST SUCCEED
            for (int i = 0; i < 3; i++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO scan_jobs (id, repository_id, status, trigger_type, webhook_delivery_id, created_at, next_event_sequence) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, repoId);
                    ps.setString(3, "COMPLETED");
                    ps.setString(4, "MANUAL");
                    ps.setNull(5, java.sql.Types.OTHER);
                    ps.setTimestamp(6, Timestamp.from(Instant.now()));
                    ps.setLong(7, 0L);
                    int inserted = ps.executeUpdate();
                    assertThat(inserted).isEqualTo(1);
                }
            }

            // 2. Insert first scan_job with non-null webhook_delivery_id - MUST SUCCEED
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scan_jobs (id, repository_id, status, trigger_type, webhook_delivery_id, created_at, next_event_sequence) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, repoId);
                ps.setString(3, "QUEUED");
                ps.setString(4, "WEBHOOK_PUSH");
                ps.setObject(5, deliveryId);
                ps.setTimestamp(6, Timestamp.from(Instant.now()));
                ps.setLong(7, 0L);
                int inserted = ps.executeUpdate();
                assertThat(inserted).isEqualTo(1);
            }

            // 3. Insert second scan_job with same webhook_delivery_id - MUST FAIL WITH UNIQUE CONSTRAINT VIOLATION
            assertThatThrownBy(() -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO scan_jobs (id, repository_id, status, trigger_type, webhook_delivery_id, created_at, next_event_sequence) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, repoId);
                    ps.setString(3, "QUEUED");
                    ps.setString(4, "WEBHOOK_PUSH");
                    ps.setObject(5, deliveryId);
                    ps.setTimestamp(6, Timestamp.from(Instant.now()));
                    ps.setLong(7, 0L);
                    ps.executeUpdate();
                }
            }).isInstanceOf(SQLException.class);
        }
    }
}
