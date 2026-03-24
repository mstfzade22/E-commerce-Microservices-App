package com.ecommerce.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InitiatePaymentRequest(
        @NotBlank String orderNumber
) {}
