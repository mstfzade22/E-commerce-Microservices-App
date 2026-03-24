package com.ecommerce.orderservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        String eventId,
        String eventType,
        Long orderId,
        String orderNumber,
        UUID userId,
        List<OrderEventItem> items,
        BigDecimal totalAmount,
        BigDecimal finalAmount,
        String status,
        Instant timestamp
) {
    public record OrderEventItem(
            Long productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}