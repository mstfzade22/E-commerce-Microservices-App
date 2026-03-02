package com.ecommerce.paymentservice.dto.kapitalbank;

public record KapitalBankStatusResponse(
        String status,
        String orderStatus,
        String orderId,
        String sessionId
) {}
