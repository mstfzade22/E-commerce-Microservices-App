package com.ecommerce.cartservice.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BaseException {
    public InsufficientStockException(Long productId, Integer requested, Integer available) {
        super(String.format("Insufficient stock for product %d. Requested: %d, Available: %d",
                productId, requested, available), HttpStatus.CONFLICT);
    }
}