package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.config.KafkaTopicConfig;
import com.ecommerce.cartservice.dto.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class CartEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CartEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendItemAddedEvent(CartItemAddedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendItemUpdatedEvent(CartItemUpdatedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendItemRemovedEvent(CartItemRemovedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendCartClearedEvent(CartClearedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendCartConvertedEvent(CartConvertedEvent event) {
        send(event.userId().toString(), event);
    }

    private void send(String key, Object event) {
        log.info("Sending event to topic {}: {}", KafkaTopicConfig.CART_EVENTS, event);
        kafkaTemplate.send(KafkaTopicConfig.CART_EVENTS, key, event);
    }
}