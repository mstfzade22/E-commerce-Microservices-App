package com.ecommerce.productservice.dto.response;

public record ProductImageResponse(
        Long id,
        String imageUrl,
        String altText,
        boolean isPrimary,
        int displayOrder
) {}
