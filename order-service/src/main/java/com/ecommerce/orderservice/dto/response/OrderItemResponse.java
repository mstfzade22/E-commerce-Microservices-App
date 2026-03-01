package com.ecommerce.orderservice.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        String sku,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {}