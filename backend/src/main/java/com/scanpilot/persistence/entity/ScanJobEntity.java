package com.scanpilot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "scan_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ScanJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "scan_mode", length = 64)
    private String scanMode;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "stage", length = 64)
    private String stage;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "worker_instance_id")
    private String workerInstanceId;

    @Column(name = "heartbeat_at")
    private Instant heartbeatAt;

    @Column(name = "next_event_sequence", nullable = false, updatable = false)
    @Builder.Default
    private long nextEventSequence = 0L;
}
