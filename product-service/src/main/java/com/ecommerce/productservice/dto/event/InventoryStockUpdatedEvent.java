package com.ecommerce.productservice.dto.event;

import com.ecommerce.productservice.entity.StockStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Event consumed FROM Inventory Service when stock status changes.
 * This is different from StockUpdatedEvent which is produced BY Product Service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryStockUpdatedEvent(
        String eventId,
        String eventType,
        Long productId,
        Integer newQuantity,
        StockStatus stockStatus,
        Instant timestamp
) {
    /**
     * Converts quantity to StockStatus if stockStatus is not provided.
     */
    public StockStatus getEffectiveStockStatus() {
        if (stockStatus != null) {
            return stockStatus;
        }
        if (newQuantity != null) {
            return StockStatus.fromQuantity(newQuantity);
        }
        return StockStatus.OUT_OF_STOCK;
    }
}

