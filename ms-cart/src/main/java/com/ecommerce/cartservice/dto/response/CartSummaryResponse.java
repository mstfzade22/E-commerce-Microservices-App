package com.ecommerce.cartservice.dto.response;

import java.math.BigDecimal;

public record CartSummaryResponse(
        Integer totalItems,
        Integer uniqueProducts,
        BigDecimal totalPrice
) {}