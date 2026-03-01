package com.ecommerce.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        Long id,
        UUID userId,
        List<CartItemResponse> items,
        BigDecimal totalAmount,
        String status
) {}