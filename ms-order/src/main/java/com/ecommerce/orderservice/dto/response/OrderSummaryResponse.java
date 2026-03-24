package com.ecommerce.orderservice.dto.response;

import com.ecommerce.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(
        String orderNumber,
        OrderStatus status,
        Integer itemCount,
        BigDecimal finalAmount,
        String currency,
        Instant createdAt
) {}