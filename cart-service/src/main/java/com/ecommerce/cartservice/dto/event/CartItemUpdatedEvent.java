package com.ecommerce.cartservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record CartItemUpdatedEvent(
        String eventId,
        String eventType, // e.g., "CART_ITEM_UPDATED"
        UUID userId,
        Long cartId,
        Long productId,
        Integer oldQuantity,
        Integer newQuantity,
        Instant timestamp
) {}