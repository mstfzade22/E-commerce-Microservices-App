package com.ecommerce.productservice.dto.response;

public record CategoryCreateResponse(
        Long id,
        String name,
        String slug,
        String message
) {
    public static CategoryCreateResponse of(Long id, String name, String slug) {
        return new CategoryCreateResponse(id, name, slug, "Category created successfully");
    }
}
