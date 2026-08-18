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

import java.util.UUID;

@Entity
@Table(name = "coverage_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CoverageItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "coverage_record_id", nullable = false)
    private UUID coverageRecordId;

    @Column(name = "file_path", length = 1024)
    private String filePath;

    @Column(name = "classification", length = 64)
    private String classification;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "impact", length = 64)
    private String impact;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}
