package com.ecommerce.orderservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record OrderDeliveredEvent(
        String eventId,
        String eventType,
        Long orderId,
        String orderNumber,
        UUID userId,
        Instant deliveredAt,
        Instant timestamp
) {}