package com.ecommerce.paymentservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRefundedEvent(
        String eventId,
        String eventType,
        Long paymentId,
        String orderNumber,
        UUID userId,
        BigDecimal amount,
        Instant refundedAt,
        Instant timestamp
) {}
