package com.ecommerce.productservice.dto.event;

import com.ecommerce.productservice.entity.StockStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryStockUpdatedEvent(
        String eventId,
        String eventType,
        Long productId,
        Integer newQuantity,
        StockStatus stockStatus,
        Instant timestamp
) {

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

