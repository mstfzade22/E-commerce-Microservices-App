package com.ecommerce.orderservice.dto.response;

import com.ecommerce.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        Long id,
        String orderNumber,
        UUID userId,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String currency,
        ShippingAddressResponse shippingAddress,
        String notes,
        String cancelledReason,
        Instant createdAt,
        Instant updatedAt
) {}