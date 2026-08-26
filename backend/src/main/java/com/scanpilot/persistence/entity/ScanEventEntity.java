package com.scanpilot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scan_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ScanEventEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scan_job_id", nullable = false)
    private UUID scanJobId;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "stage", length = 64, nullable = false)
    private String stage;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(name = "message_code", length = 64, nullable = false)
    private String messageCode;

    @Column(name = "payload_json", length = 1024)
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
