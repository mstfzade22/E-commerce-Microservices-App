package com.ecommerce.cartservice.exception;

public class CartItemNotFoundException extends ResourceNotFoundException {
    public CartItemNotFoundException(Long productId, Long cartId) {
        super("Cart item with product " + productId + " not found in cart " + cartId);
    }
}
