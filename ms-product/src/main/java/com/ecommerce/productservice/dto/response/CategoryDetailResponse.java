package com.ecommerce.productservice.dto.response;

import java.time.Instant;
import java.util.List;

public record CategoryDetailResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        CategorySummaryResponse parent,
        List<CategorySummaryResponse> children,
        Long productCount,
        Instant createdAt,
        Instant updatedAt
) {}
