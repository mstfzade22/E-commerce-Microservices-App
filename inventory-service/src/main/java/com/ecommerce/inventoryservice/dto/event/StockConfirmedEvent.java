package com.ecommerce.inventoryservice.dto.event;

import com.ecommerce.inventoryservice.entity.StockStatus;
import java.time.Instant;

public record StockConfirmedEvent(
        String eventId,
        String eventType, // "STOCK_CONFIRMED"
        Long productId,
        String orderId,
        Integer confirmedQuantity,
        Integer remainingQuantity,
        StockStatus stockStatus,
        Instant timestamp
) {}