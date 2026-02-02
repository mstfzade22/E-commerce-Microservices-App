package com.ecommerce.productservice.dto.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
) {
    public static PriceChangedEvent of(
            Long id,
            String slug,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            BigDecimal oldDiscountPrice,
            BigDecimal newDiscountPrice
    ) {
        Instant now = Instant.now();
        return new PriceChangedEvent(
                id,
                slug,
                oldPrice,
                newPrice,
                oldDiscountPrice,
                newDiscountPrice,
                now,
                UUID.randomUUID().toString(),
                now
        );
    }
}
