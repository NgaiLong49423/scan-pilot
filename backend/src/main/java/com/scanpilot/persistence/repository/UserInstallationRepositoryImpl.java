package com.scanpilot.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public class UserInstallationRepositoryImpl implements UserInstallationRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private volatile Boolean isH2 = null;

    private boolean checkIsH2() {
        if (isH2 == null) {
            try {
                String dbName = entityManager.unwrap(org.hibernate.Session.class)
                        .doReturningWork(conn -> conn.getMetaData().getDatabaseProductName());
                isH2 = dbName != null && dbName.toUpperCase().contains("H2");
            } catch (Exception e) {
                isH2 = false;
            }
        }
        return Boolean.TRUE.equals(isH2);
    }

    @Override
    @Transactional
    public int upsertUserInstallation(
            UUID id,
            UUID userId,
            Long githubUserId,
            Long installationId,
            String accountLogin,
            String accountType,
            Instant verifiedAt
    ) {
        String sql;
        if (checkIsH2()) {
            sql = """
                MERGE INTO user_installations (id, user_id, github_user_id, installation_id, account_login, account_type, verified_at)
                KEY (user_id, installation_id)
                VALUES (:id, :userId, :githubUserId, :installationId, :accountLogin, :accountType, :verifiedAt)
            """;
        } else {
            sql = """
                INSERT INTO user_installations (id, user_id, github_user_id, installation_id, account_login, account_type, verified_at)
                VALUES (:id, :userId, :githubUserId, :installationId, :accountLogin, :accountType, :verifiedAt)
                ON CONFLICT (user_id, installation_id)
                DO UPDATE SET
                    github_user_id = EXCLUDED.github_user_id,
                    account_login = EXCLUDED.account_login,
                    account_type = EXCLUDED.account_type,
                    verified_at = EXCLUDED.verified_at
            """;
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", id);
        query.setParameter("userId", userId);
        query.setParameter("githubUserId", githubUserId);
        query.setParameter("installationId", installationId);
        query.setParameter("accountLogin", accountLogin);
        query.setParameter("accountType", accountType);
        query.setParameter("verifiedAt", verifiedAt);
        return query.executeUpdate();
    }
}
