package com.ecommerce.productservice.exception;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String field, String reason) {
        super(String.format("Invalid value for '%s': %s", field, reason));
    }

}
