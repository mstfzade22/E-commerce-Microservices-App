package com.ecommerce.paymentservice.dto.kapitalbank;

public record KapitalBankOrderResponse(
        String status,
        String orderId,
        String sessionId,
        String url
) {}
