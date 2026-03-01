package com.ecommerce.orderservice.exception;

public class StockReservationException extends RuntimeException {
    public StockReservationException(String message) {
        super(message);
    }
}