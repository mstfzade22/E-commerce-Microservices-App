package com.ecommerce.productservice.dto.common;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public static ValidationErrorResponse of(int status, String message, Map<String, String> fieldErrors) {
        return new ValidationErrorResponse(
                Instant.now(),
                status,
                "Validation Failed",
                message,
                fieldErrors
        );
    }
}
