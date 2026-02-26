package com.ecommerce.cartservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record CartClearedEvent(
        String eventId,
        String eventType,
        UUID userId,
        Long cartId,
        Integer itemCount,
        Instant timestamp
) {}