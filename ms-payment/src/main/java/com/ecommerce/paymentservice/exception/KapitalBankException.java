package com.ecommerce.paymentservice.exception;

public class KapitalBankException extends RuntimeException {
    public KapitalBankException(String message) {
        super(message);
    }

    public KapitalBankException(String message, Throwable cause) {
        super(message, cause);
    }
}
