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
@Table(name = "coverage_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CoverageRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scan_job_id", nullable = false)
    private UUID scanJobId;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "total_files")
    private Integer totalFiles;

    @Column(name = "scanned_files")
    private Integer scannedFiles;

    @Column(name = "skipped_files")
    private Integer skippedFiles;

    @Column(name = "text_files")
    private Integer textFiles;

    @Column(name = "binary_files")
    private Integer binaryFiles;

    @Column(name = "undetermined_files")
    private Integer undeterminedFiles;

    @Column(name = "total_bytes")
    private Long totalBytes;

    @Column(name = "coverage_impact", length = 64)
    private String coverageImpact;

    @Column(name = "created_at")
    private Instant createdAt;
}
