package com.ecommerce.cartservice.dto.response;

import com.ecommerce.cartservice.entity.CartStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        Long cartId,
        UUID userId,
        List<CartItemResponse> items,
        BigDecimal totalPrice,
        Integer totalItems,
        CartStatus status,
        Instant updatedAt
) {}