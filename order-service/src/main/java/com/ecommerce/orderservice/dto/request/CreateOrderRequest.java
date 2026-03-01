package com.ecommerce.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank(message = "Shipping address line 1 is required")
        String shippingAddressLine1,

        String shippingAddressLine2,

        @NotBlank(message = "City is required")
        String shippingCity,

        String shippingState,

        @NotBlank(message = "Postal code is required")
        String shippingPostalCode,

        @NotBlank(message = "Country is required")
        String shippingCountry,

        String notes
) {}