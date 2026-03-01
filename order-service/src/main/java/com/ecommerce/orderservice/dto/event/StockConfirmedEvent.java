package com.ecommerce.orderservice.dto.event;

import java.time.Instant;

public record StockConfirmedEvent(
        String eventId,
        String eventType,
        Long productId,
        String orderId,
        Integer confirmedQuantity,
        Integer remainingQuantity,
        String stockStatus,
        Instant timestamp
) {}