package com.ecommerce.orderservice.dto.event;

import java.time.Instant;

public record ProductDeletedEvent(
        Long id,
        String slug,
        String sku,
        Instant deletedAt,
        String eventId,
        Instant timestamp
) {}