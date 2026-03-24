package com.ecommerce.productservice.dto.request;

import java.math.BigDecimal;
import java.util.Map;

public record ProductFilterRequest(
        String keyword,
        Long categoryId,
        Boolean featured,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String stockStatus,
        Map<String, String> attributes,
        String sortBy,
        String sortDir
) {}
