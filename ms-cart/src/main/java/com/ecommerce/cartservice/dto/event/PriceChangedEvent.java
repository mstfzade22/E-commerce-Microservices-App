package com.ecommerce.cartservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceChangedEvent(
        Long id,
        String slug,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        BigDecimal oldDiscountPrice,
        BigDecimal newDiscountPrice,
        Instant changedAt,
        String eventId,
        Instant timestamp
) {}