package com.ecommerce.inventoryservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductCreatedEvent(
        Long id,
        String name,
        String slug,
        String sku,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        Boolean isActive,
        Integer initialStock,
        Instant createdAt,
        String eventId,
        Instant timestamp
) {}