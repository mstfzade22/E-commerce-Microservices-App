package com.ecommerce.orderservice.config;

public final class KafkaTopicConfig {

    public static final String ORDER_EVENTS_TOPIC = "order-events";
    public static final String INVENTORY_EVENTS_TOPIC = "inventory-events";
    public static final String PRODUCT_EVENTS_TOPIC = "product-events";
    public static final String ORDER_SERVICE_GROUP = "order-service-group";

    private KafkaTopicConfig() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}