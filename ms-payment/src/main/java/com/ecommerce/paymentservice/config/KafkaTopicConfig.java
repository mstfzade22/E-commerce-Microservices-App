package com.ecommerce.paymentservice.config;

public final class KafkaTopicConfig {

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    public static final String ORDER_EVENTS_TOPIC = "order-events";
    public static final String PAYMENT_SERVICE_GROUP = "payment-service-group";

    private KafkaTopicConfig() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}
