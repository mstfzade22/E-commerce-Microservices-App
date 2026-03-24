package com.ecommerce.paymentservice.dto.response;

import com.ecommerce.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        Long id,
        String orderNumber,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String kapitalOrderId,
        String description,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {}
