package com.ecommerce.orderservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        String eventId,
        String eventType,
        Long paymentId,
        String orderNumber,
        UUID userId,
        String reason,
        Instant failedAt,
        Instant timestamp
) {}
