package com.ecommerce.paymentservice.dto.response;

import com.ecommerce.paymentservice.entity.PaymentStatus;

import java.time.Instant;

public record PaymentStatusHistoryResponse(
        PaymentStatus previousStatus,
        PaymentStatus newStatus,
        String reason,
        Instant changedAt
) {}
