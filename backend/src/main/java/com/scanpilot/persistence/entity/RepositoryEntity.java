package com.scanpilot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "repositories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_repositories_user_github_repo", columnNames = {"user_id", "github_repo_id"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "github_repo_id")
    private Long githubRepoId;

    @Column(name = "installation_id")
    private Long installationId;

    @Column(name = "owner", length = 255)
    private String owner;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "full_name", length = 512)
    private String fullName;

    @Column(name = "default_branch", length = 255)
    private String defaultBranch;

    @Column(name = "primary_branch", length = 255)
    private String primaryBranch;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "monitored_at")
    private Instant monitoredAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
