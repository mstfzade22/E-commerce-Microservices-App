package com.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.Size;

public record ProductImageRequest(
        String imageUrl,

        @Size(max = 255, message = "Alt text cannot exceed 255 characters")
        String altText,

        boolean isPrimary,

        int displayOrder
) {
    public ProductImageRequest {
        if (displayOrder < 0) {
            displayOrder = 0;
        }
    }
}
