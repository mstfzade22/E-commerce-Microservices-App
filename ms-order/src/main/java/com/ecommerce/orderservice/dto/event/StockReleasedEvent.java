package com.ecommerce.orderservice.dto.event;

import java.time.Instant;

public record StockReleasedEvent(
        String eventId,
        String eventType,
        Long productId,
        String orderId,
        Integer releasedQuantity,
        Integer remainingQuantity,
        String stockStatus,
        Instant timestamp
) {}