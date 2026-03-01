package com.ecommerce.orderservice.dto.common;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {}