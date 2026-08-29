package com.scanpilot.github.dto;

import lombok.Builder;

@Builder
public record WebhookDeliveryResponseDto(
    String deliveryId,
    String status,
    String reason
) {
}
