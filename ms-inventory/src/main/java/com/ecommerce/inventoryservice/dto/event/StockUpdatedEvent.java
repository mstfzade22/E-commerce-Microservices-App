package com.ecommerce.inventoryservice.dto.event;

import com.ecommerce.inventoryservice.entity.StockStatus;
import java.time.Instant;



public record StockUpdatedEvent(
        String eventId,
        String eventType,
        Long productId,
        Integer newQuantity,
        StockStatus stockStatus,
        Instant timestamp
) {}