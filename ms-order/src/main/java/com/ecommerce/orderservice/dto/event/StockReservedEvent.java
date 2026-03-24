package com.ecommerce.orderservice.dto.event;

import java.time.Instant;

public record StockReservedEvent(
        String eventId,
        String eventType,
        Long productId,
        String orderId,
        Integer reservedQuantity,
        Integer remainingQuantity,
        String stockStatus,
        Instant timestamp
) {}