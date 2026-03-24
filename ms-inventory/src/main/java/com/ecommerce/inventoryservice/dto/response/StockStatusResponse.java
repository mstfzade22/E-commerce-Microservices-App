package com.ecommerce.inventoryservice.dto.response;

import com.ecommerce.inventoryservice.entity.StockStatus;

public record StockStatusResponse(
        Long productId,
        StockStatus stockStatus,
        Integer availableQuantity
) {}