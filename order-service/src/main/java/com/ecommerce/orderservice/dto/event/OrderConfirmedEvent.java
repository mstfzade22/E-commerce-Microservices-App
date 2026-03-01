package com.ecommerce.orderservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        String eventId,
        String eventType,
        Long orderId,
        String orderNumber,
        UUID userId,
        Instant confirmedAt,
        Instant timestamp
) {}