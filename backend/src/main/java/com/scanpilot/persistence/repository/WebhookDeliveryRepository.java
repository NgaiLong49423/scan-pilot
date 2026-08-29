package com.scanpilot.persistence.repository;

import com.scanpilot.persistence.entity.WebhookDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDeliveryEntity, UUID>, WebhookDeliveryRepositoryCustom {

    Optional<WebhookDeliveryEntity> findByDeliveryId(String deliveryId);
}
