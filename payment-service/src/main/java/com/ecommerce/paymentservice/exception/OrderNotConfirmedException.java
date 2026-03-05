package com.ecommerce.paymentservice.exception;

public class OrderNotConfirmedException extends RuntimeException {
    public OrderNotConfirmedException(String message) {
        super(message);
    }
}
