package com.ecommerce.inventoryservice.dto.common;

import java.util.Map;

public record ValidationErrorResponse(
        int status,
        String message,
        Map<String, String> errors
) {
    public static ValidationErrorResponse of(int status, String message, Map<String, String> errors) {
        return new ValidationErrorResponse(status, message, errors);
    }
}
