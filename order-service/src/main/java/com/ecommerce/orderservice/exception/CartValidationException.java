package com.ecommerce.orderservice.exception;

import java.util.List;

public class CartValidationException extends RuntimeException {
    private final List<String> errors;

    public CartValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}