package com.ecommerce.productservice.dto.event;

import com.ecommerce.productservice.entity.StockStatus;

import java.time.Instant;
import java.util.UUID;

public record StockUpdatedEvent(
        Long productId,
        StockStatus stockStatus,
        Instant updatedAt,
        String eventId,
        Instant timestamp
) {
    public static StockUpdatedEvent of(Long productId, StockStatus stockStatus) {
        Instant now = Instant.now();
        return new StockUpdatedEvent(
                productId,
                stockStatus,
                now,
                UUID.randomUUID().toString(),
                now
        );
    }

    public static StockUpdatedEvent fromQuantity(Long productId, int quantity) {
        return of(productId, StockStatus.fromQuantity(quantity));
    }
}
