package com.ecommerce.cartservice.exception;

import java.util.UUID;

public class CartNotFoundException extends ResourceNotFoundException {
    public CartNotFoundException(UUID userId) {
        super("Cart not found for user: " + userId);
    }

    public CartNotFoundException(Long cartId) {
        super("Cart not found with ID: " + cartId);
    }
}