package com.ecommerce.productservice.dto.response;

public record CategorySummaryResponse(
        Long id,
        String name,
        String slug
) {}
