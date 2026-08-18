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
@Table(name = "review_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReviewRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "finding_id")
    private UUID findingId;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "created_at")
    private Instant createdAt;
}
