package com.ecommerce.cartservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemAddedEvent(
        String eventId,
        String eventType, // e.g., "CART_ITEM_ADDED"
        UUID userId,
        Long cartId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        Instant timestamp
) {}