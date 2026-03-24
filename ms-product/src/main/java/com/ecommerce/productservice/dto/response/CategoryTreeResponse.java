package com.ecommerce.productservice.dto.response;

import java.util.List;

public record CategoryTreeResponse(
        Long id,
        String name,
        String slug,
        String imageUrl,
        Long productCount,
        List<CategoryTreeResponse> children
) {}
