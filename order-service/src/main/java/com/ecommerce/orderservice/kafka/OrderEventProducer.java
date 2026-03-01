package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.config.KafkaTopicConfig;
import com.ecommerce.orderservice.dto.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendOrderConfirmedEvent(OrderConfirmedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendOrderCancelledEvent(OrderCancelledEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendOrderShippedEvent(OrderShippedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendOrderDeliveredEvent(OrderDeliveredEvent event) {
        send(event.userId().toString(), event);
    }

    private void send(String key, Object event) {
        log.info("Sending event to topic {}: {}", KafkaTopicConfig.ORDER_EVENTS_TOPIC, event);
        kafkaTemplate.send(KafkaTopicConfig.ORDER_EVENTS_TOPIC, key, event);
    }
}
