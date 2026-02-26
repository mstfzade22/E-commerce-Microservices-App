package com.ecommerce.cartservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CartItemResponse(
        Long productId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Boolean inStock,
        Instant addedAt
) {}