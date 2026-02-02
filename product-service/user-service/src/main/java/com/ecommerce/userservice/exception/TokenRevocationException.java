package com.ecommerce.userservice.exception;

public class TokenRevocationException extends RuntimeException {
    public TokenRevocationException(String message) {
        super(message);
    }
}