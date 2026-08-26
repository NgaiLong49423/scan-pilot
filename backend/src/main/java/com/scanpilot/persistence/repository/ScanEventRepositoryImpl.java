package com.scanpilot.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ScanEventRepositoryImpl implements ScanEventRepositoryCustom {

    private static final String CTE_SQL = """
        WITH allocated AS (
          UPDATE scan_jobs
          SET next_event_sequence = next_event_sequence + 1
          WHERE id = ?
            AND next_event_sequence < ?
          RETURNING next_event_sequence
        )
        INSERT INTO scan_events (
          id, scan_job_id, sequence_number, stage,
          event_type, message_code, payload_json, created_at
        )
        SELECT
          ?, ?, allocated.next_event_sequence, ?,
          ?, ?, ?, ?
        FROM allocated
        RETURNING sequence_number
        """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<Long> insertEventAtomicCTE(
        UUID jobId,
        long maxLimit,
        UUID eventId,
        String stage,
        String eventType,
        String messageCode,
        String payloadJson,
        Instant createdAt
    ) {
        if (jobId == null || eventId == null) {
            return Optional.empty();
        }

        try {
            Long allocatedSeq = jdbcTemplate.query(
                CTE_SQL,
                ps -> {
                    ps.setObject(1, jobId);
                    ps.setLong(2, maxLimit);
                    ps.setObject(3, eventId);
                    ps.setObject(4, jobId);
                    ps.setString(5, stage);
                    ps.setString(6, eventType);
                    ps.setString(7, messageCode);
                    ps.setString(8, payloadJson);
                    ps.setTimestamp(9, Timestamp.from(createdAt != null ? createdAt : Instant.now()));
                },
                rs -> rs.next() ? rs.getLong(1) : null
            );

            return Optional.ofNullable(allocatedSeq);
        } catch (Exception e) {
            log.warn("Event persistence error for eventType={}", eventType);
            return Optional.empty();
        }
    }
}
