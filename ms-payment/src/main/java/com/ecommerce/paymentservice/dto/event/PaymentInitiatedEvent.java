package com.ecommerce.paymentservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInitiatedEvent(
        String eventId,
        String eventType,
        Long paymentId,
        String orderNumber,
        UUID userId,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {}
