package com.ecommerce.cartservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemAddedEvent(
        String eventId,
        String eventType,
        UUID userId,
        Long cartId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        Instant timestamp
) {}