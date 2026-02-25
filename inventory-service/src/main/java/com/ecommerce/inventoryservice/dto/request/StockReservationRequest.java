package com.ecommerce.inventoryservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockReservationRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotBlank(message = "Order ID is required")
        String orderId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {}
