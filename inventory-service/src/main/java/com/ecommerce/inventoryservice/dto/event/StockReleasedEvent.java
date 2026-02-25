package com.ecommerce.inventoryservice.dto.event;

import com.ecommerce.inventoryservice.entity.StockStatus;
import java.time.Instant;

public record StockReleasedEvent(
        String eventId,
        String eventType, // "STOCK_RELEASED"
        Long productId,
        String orderId,
        Integer releasedQuantity,
        Integer remainingQuantity,
        StockStatus stockStatus,
        Instant timestamp
) {}