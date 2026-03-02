package com.ecommerce.paymentservice.dto.response;

import com.ecommerce.paymentservice.entity.PaymentStatus;

import java.time.Instant;

public record PaymentInitiatedResponse(
        Long paymentId,
        String orderNumber,
        PaymentStatus status,
        String paymentUrl,
        Instant createdAt
) {}
