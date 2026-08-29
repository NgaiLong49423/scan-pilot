package com.scanpilot.persistence.repository;

import java.time.Instant;
import java.util.UUID;

public interface UserInstallationRepositoryCustom {

    int upsertUserInstallation(
            UUID id,
            UUID userId,
            Long githubUserId,
            Long installationId,
            String accountLogin,
            String accountType,
            Instant verifiedAt
    );
}
