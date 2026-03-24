package com.ecommerce.cartservice.dto.response;

import java.math.BigDecimal;

public record CartItemValidation(
        Long productId,
        String productName,
        Boolean available,
        Boolean inStock,
        Boolean priceChanged,
        BigDecimal currentPrice,
        BigDecimal cartPrice,
        Integer requestedQuantity,
        Integer availableQuantity
) {}