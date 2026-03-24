package com.ecommerce.productservice.dto.event;

import java.time.Instant;
import java.util.UUID;

public record ProductDeletedEvent(
        Long id,
        String slug,
        String sku,
        Instant deletedAt,
        String eventId,
        Instant timestamp
) {
    public static ProductDeletedEvent of(Long id, String slug, String sku) {
        Instant now = Instant.now();
        return new ProductDeletedEvent(
                id,
                slug,
                sku,
                now,
                UUID.randomUUID().toString(),
                now
        );
    }
}
