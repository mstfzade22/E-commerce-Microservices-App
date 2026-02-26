package com.ecommerce.cartservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record CartItemRemovedEvent(
        String eventId,
        String eventType, // e.g., "CART_ITEM_REMOVED"
        UUID userId,
        Long cartId,
        Long productId,
        Instant timestamp
) {}