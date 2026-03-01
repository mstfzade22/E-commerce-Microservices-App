package com.ecommerce.orderservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        String eventId,
        String eventType,
        Long orderId,
        String orderNumber,
        UUID userId,
        String reason,
        Instant cancelledAt,
        Instant timestamp
) {}