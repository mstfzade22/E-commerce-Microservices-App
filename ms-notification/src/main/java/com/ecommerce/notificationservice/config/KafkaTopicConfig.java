package com.ecommerce.notificationservice.config;

public final class KafkaTopicConfig {

    public static final String ORDER_EVENTS_TOPIC = "order-events";
    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    public static final String INVENTORY_EVENTS_TOPIC = "inventory-events";
    public static final String NOTIFICATION_SERVICE_GROUP = "notification-service-group";

    private KafkaTopicConfig() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}
