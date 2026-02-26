package com.ecommerce.cartservice.exception;

import org.springframework.http.HttpStatus;

public class ProductNotAvailableException extends BaseException {
    public ProductNotAvailableException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}