package com.ecommerce.notificationservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSuccessEvent(
        String eventId,
        String eventType,
        Long paymentId,
        String orderNumber,
        UUID userId,
        BigDecimal amount,
        String kapitalOrderId,
        Instant approvedAt,
        Instant timestamp
) {}
