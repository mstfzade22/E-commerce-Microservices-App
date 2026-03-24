package com.ecommerce.inventoryservice.dto.event;

import com.ecommerce.inventoryservice.entity.StockStatus;
import java.time.Instant;

public record StockReservedEvent(
        String eventId,
        String eventType,
        Long productId,
        String orderId,
        Integer reservedQuantity,
        Integer remainingQuantity,
        StockStatus stockStatus,
        Instant timestamp
) {}