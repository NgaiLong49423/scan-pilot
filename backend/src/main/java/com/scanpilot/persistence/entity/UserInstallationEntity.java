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
    name = "user_installations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_installations_user_inst", columnNames = {"user_id", "installation_id"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserInstallationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "github_user_id", nullable = false)
    private Long githubUserId;

    @Column(name = "installation_id", nullable = false)
    private Long installationId;

    @Column(name = "account_login", length = 255, nullable = false)
    private String accountLogin;

    @Column(name = "account_type", length = 64, nullable = false)
    private String accountType;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;
}
