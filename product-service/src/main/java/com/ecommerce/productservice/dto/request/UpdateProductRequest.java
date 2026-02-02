package com.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    private String name;

    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase with hyphens only")
    private String slug;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    private String shortDescription;

    private Long categoryId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount price must be greater than or equal to 0")
    private BigDecimal discountPrice;

    @Size(max = 100, message = "SKU cannot exceed 100 characters")
    private String sku;


    @DecimalMin(value = "0.0", message = "Weight must be positive")
    private Double weightKg;

    @DecimalMin(value = "0.0", message = "Length must be positive")
    private Double lengthCm;

    @DecimalMin(value = "0.0", message = "Width must be positive")
    private Double widthCm;

    @DecimalMin(value = "0.0", message = "Height must be positive")
    private Double heightCm;

    private Boolean isActive;

    private Boolean isFeatured;

    private Map<String, Object> attributes;
}
