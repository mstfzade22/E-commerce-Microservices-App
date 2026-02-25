package com.ecommerce.inventoryservice.config;

public final class KafkaTopicConfig {

    private KafkaTopicConfig() {
        // Private constructor to prevent instantiation as per requirements
        throw new UnsupportedOperationException("This is a utility/constants class and cannot be instantiated");
    }

    public static final String INVENTORY_EVENTS_TOPIC = "inventory-events";
    public static final String PRODUCT_EVENTS_TOPIC = "product-events";
    public static final String INVENTORY_SERVICE_GROUP = "inventory-service-group";
}