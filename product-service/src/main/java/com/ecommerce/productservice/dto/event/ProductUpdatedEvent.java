package com.ecommerce.productservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductUpdatedEvent(
        Long id,
        String name,
        String slug,
        String sku,
        BigDecimal price,
        Long categoryId,
        Boolean isActive,
        Instant updatedAt,
        String eventId,
        Instant timestamp
) {
    public static ProductUpdatedEvent of(
            Long id,
            String name,
            String slug,
            String sku,
            BigDecimal price,
            Long categoryId,
            Boolean isActive,
            Instant updatedAt
    ) {
        return new ProductUpdatedEvent(
                id,
                name,
                slug,
                sku,
                price,
                categoryId,
                isActive,
                updatedAt,
                UUID.randomUUID().toString(),
                Instant.now()
        );
    }
}
