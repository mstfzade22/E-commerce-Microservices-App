package com.ecommerce.orderservice.dto.response;

import java.util.List;

public record CartValidationResponse(
        boolean valid,
        String message,
        List<String> errors
) {}