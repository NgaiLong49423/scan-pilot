package com.scanpilot.persistence.repository;

import java.time.Instant;
import java.util.UUID;

public interface WebhookDeliveryRepositoryCustom {

    boolean insertIfAbsent(UUID id, String deliveryId, String eventType, Instant now);
}
