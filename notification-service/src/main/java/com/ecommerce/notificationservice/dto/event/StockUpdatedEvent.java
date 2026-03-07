package com.ecommerce.notificationservice.dto.event;

import java.time.Instant;

public record StockUpdatedEvent(
        String eventId,
        String eventType,
        Long productId,
        Integer newQuantity,
        String stockStatus,
        Instant timestamp
) {}
