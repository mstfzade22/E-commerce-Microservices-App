package com.ecommerce.inventoryservice.dto.event;

import com.ecommerce.inventoryservice.entity.StockStatus;
import java.time.Instant;

/**
 * Matches Product Service's InventoryStockUpdatedEvent exactly
 */

public record StockUpdatedEvent(
        String eventId,
        String eventType, // "STOCK_UPDATED"
        Long productId,
        Integer newQuantity,
        StockStatus stockStatus,
        Instant timestamp
) {}