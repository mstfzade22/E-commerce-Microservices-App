package com.ecommerce.productservice.dto.response;

import com.ecommerce.productservice.entity.StockStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProductDetailResponse(
        Long id,
        String name,
        String slug,
        String description,
        String shortDescription,
        BigDecimal price,
        BigDecimal discountPrice,
        String sku,
        StockStatus stockStatus,
        Double weightKg,
        Double lengthCm,
        Double widthCm,
        Double heightCm,
        Boolean isActive,
        Boolean isFeatured,
        CategorySummaryResponse category,
        List<ProductImageResponse> images,
        Map<String, Object> attributes,
        Instant createdAt,
        Instant updatedAt
) {}
