package com.ecommerce.productservice.dto.response;

public record ProductCreateResponse(
        Long id,
        String name,
        String slug,
        String message
) {
    public static ProductCreateResponse of(Long id, String name, String slug) {
        return new ProductCreateResponse(id, name, slug, "Product created successfully");
    }
}
