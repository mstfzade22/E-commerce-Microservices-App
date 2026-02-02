package com.ecommerce.productservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductCreatedEvent(
        Long id,
        String name,
        String slug,
        String sku,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        Boolean isActive,
        Instant createdAt,
        String eventId,
        Instant timestamp
) {
    public static ProductCreatedEvent of(
            Long id,
            String name,
            String slug,
            String sku,
            BigDecimal price,
            Long categoryId,
            String categoryName,
            Boolean isActive,
            Instant createdAt
    ) {
        return new ProductCreatedEvent(
                id,
                name,
                slug,
                sku,
                price,
                categoryId,
                categoryName,
                isActive,
                createdAt,
                UUID.randomUUID().toString(),
                Instant.now()
        );
    }
}
