package com.ecommerce.orderservice.dto.response;

import com.ecommerce.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreateResponse(
        String orderNumber,
        OrderStatus status,
        BigDecimal finalAmount,
        Integer itemCount,
        String message,
        Instant createdAt
) {}