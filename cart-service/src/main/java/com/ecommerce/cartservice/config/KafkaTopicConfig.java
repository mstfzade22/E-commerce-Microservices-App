package com.ecommerce.cartservice.config;

public final class KafkaTopicConfig {
    private KafkaTopicConfig() {}

    public static final String CART_EVENTS = "cart-events";
    public static final String PRODUCT_EVENTS = "product-events";
    public static final String INVENTORY_EVENTS = "inventory-events";

    public static final String CART_SERVICE_GROUP = "cart-service-group";
}