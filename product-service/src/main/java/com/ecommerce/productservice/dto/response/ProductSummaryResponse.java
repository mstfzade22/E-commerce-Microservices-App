package com.ecommerce.productservice.dto.response;

import com.ecommerce.productservice.entity.StockStatus;

import java.math.BigDecimal;

public record ProductSummaryResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        BigDecimal price,
        BigDecimal discountPrice,
        String primaryImageUrl,
        String categoryName,
        StockStatus stockStatus,
        Boolean isActive,
        Boolean isFeatured
) {}
