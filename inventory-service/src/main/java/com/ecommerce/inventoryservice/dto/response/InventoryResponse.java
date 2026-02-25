package com.ecommerce.inventoryservice.dto.response;

import com.ecommerce.inventoryservice.entity.StockStatus;
import java.time.Instant;

public record InventoryResponse(
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer lowStockThreshold,
        StockStatus stockStatus,
        Instant createdAt,
        Instant updatedAt
) {}