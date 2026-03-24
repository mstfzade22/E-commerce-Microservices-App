package com.ecommerce.cartservice.dto.response;

import java.util.List;

public record CartValidationResponse(
        Boolean valid,
        List<CartItemValidation> items
) {}