package com.ecommerce.orderservice.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}