package com.ecommerce.cartservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartConvertedEvent(
        String eventId,
        String eventType,
        UUID userId,
        Long cartId,
        String orderId,
        BigDecimal totalAmount,
        Integer itemCount,
        Instant timestamp
) {}