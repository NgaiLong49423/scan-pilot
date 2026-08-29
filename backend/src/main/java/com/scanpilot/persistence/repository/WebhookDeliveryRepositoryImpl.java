package com.scanpilot.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public class WebhookDeliveryRepositoryImpl implements WebhookDeliveryRepositoryCustom {

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
    public boolean insertIfAbsent(UUID id, String deliveryId, String eventType, Instant now) {
        String sql;
        if (checkIsH2()) {
            sql = """
                MERGE INTO webhook_deliveries target
                USING (SELECT CAST(:id AS UUID) AS id,
                              CAST(:deliveryId AS VARCHAR) AS delivery_id,
                              CAST(:eventType AS VARCHAR) AS event_type,
                              'PROCESSING' AS status,
                              'PROCESSING' AS reason_code,
                              CAST(:now AS TIMESTAMP WITH TIME ZONE) AS received_at,
                              CAST(:now AS TIMESTAMP WITH TIME ZONE) AS processed_at
                       FROM DUAL) src
                ON (target.delivery_id = src.delivery_id)
                WHEN NOT MATCHED THEN
                INSERT (id, delivery_id, event_type, status, reason_code, received_at, processed_at)
                VALUES (src.id, src.delivery_id, src.event_type, src.status, src.reason_code, src.received_at, src.processed_at)
            """;
        } else {
            sql = """
                INSERT INTO webhook_deliveries (
                    id, delivery_id, event_type, status, reason_code, received_at, processed_at
                ) VALUES (
                    :id, :deliveryId, :eventType, 'PROCESSING', 'PROCESSING', :now, :now
                ) ON CONFLICT (delivery_id) DO NOTHING
            """;
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", id);
        query.setParameter("deliveryId", deliveryId);
        query.setParameter("eventType", eventType);
        query.setParameter("now", now);

        int rowsAffected = query.executeUpdate();
        return rowsAffected > 0;
    }
}
