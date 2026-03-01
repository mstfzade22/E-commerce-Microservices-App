package com.ecommerce.orderservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
        String eventId,
        String eventType,
        Long orderId,
        String orderNumber,
        UUID userId,
        Instant shippedAt,
        Instant timestamp
) {}