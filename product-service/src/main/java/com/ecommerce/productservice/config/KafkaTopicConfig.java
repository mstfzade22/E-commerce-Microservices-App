package com.ecommerce.productservice.config;

public final class KafkaTopicConfig {

    private KafkaTopicConfig() {

    }

    public static final String PRODUCT_EVENTS_TOPIC = "product-events";

    public static final String INVENTORY_EVENTS_TOPIC = "inventory-events";

}
