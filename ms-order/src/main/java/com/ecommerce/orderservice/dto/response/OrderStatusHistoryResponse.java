package com.ecommerce.orderservice.dto.response;

import com.ecommerce.orderservice.entity.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistoryResponse(
        OrderStatus previousStatus,
        OrderStatus newStatus,
        UUID changedBy,
        String reason,
        Instant changedAt
) {}