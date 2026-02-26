package com.ecommerce.cartservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record CartItemRemovedEvent(
        String eventId,
        String eventType,
        UUID userId,
        Long cartId,
        Long productId,
        Instant timestamp
) {}