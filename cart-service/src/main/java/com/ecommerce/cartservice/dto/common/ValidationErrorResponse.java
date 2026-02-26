package com.ecommerce.cartservice.dto.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ValidationErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Map<String, List<String>> validationErrors,
        Instant timestamp
) {}