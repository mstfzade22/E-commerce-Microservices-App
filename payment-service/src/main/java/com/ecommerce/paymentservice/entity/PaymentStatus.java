package com.ecommerce.paymentservice.entity;

public enum PaymentStatus {
    INITIATED,
    PROCESSING,
    APPROVED,
    DECLINED,
    CANCELLED,
    REFUNDED,
    ERROR
}
