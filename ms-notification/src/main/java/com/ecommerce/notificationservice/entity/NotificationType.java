package com.ecommerce.notificationservice.entity;

public enum NotificationType {
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    LOW_STOCK_ALERT
}
