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
@Table(name = "evidence_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EvidenceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "finding_id")
    private UUID findingId;

    @Column(name = "evidence_type", length = 64)
    private String evidenceType;

    @Column(name = "masked_secret", length = 512)
    private String maskedSecret;

    @Column(name = "redacted_snippet", columnDefinition = "TEXT")
    private String redactedSnippet;

    @Column(name = "verification_status", length = 64)
    private String verificationStatus;

    @Column(name = "source_attribution", length = 255)
    private String sourceAttribution;

    @Column(name = "created_at")
    private Instant createdAt;
}
