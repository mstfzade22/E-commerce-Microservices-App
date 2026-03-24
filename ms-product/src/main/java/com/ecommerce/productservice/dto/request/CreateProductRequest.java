package com.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase with hyphens only")
        String slug,

        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description,

        @Size(max = 500, message = "Short description cannot exceed 500 characters")
        String shortDescription,

        Long categoryId,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
        BigDecimal price,

        @DecimalMin(value = "0.0", inclusive = true, message = "Discount price must be greater than or equal to 0")
        BigDecimal discountPrice,

        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU cannot exceed 100 characters")
        String sku,


        @DecimalMin(value = "0.0", message = "Weight must be positive")
        Double weightKg,

        @DecimalMin(value = "0.0", message = "Length must be positive")
        Double lengthCm,

        @DecimalMin(value = "0.0", message = "Width must be positive")
        Double widthCm,

        @DecimalMin(value = "0.0", message = "Height must be positive")
        Double heightCm,

        Boolean isActive,

        Boolean isFeatured,

        @Min(value = 0, message = "Initial stock must be non-negative")
        Integer initialStock,

        Map<String, Object> attributes
) {}
