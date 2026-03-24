package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.config.KafkaTopicConfig;
import com.ecommerce.orderservice.dto.event.*;
import com.ecommerce.orderservice.entity.OutboxEvent;
import com.ecommerce.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventProducer {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper kafkaObjectMapper;

    public OrderEventProducer(OutboxEventRepository outboxRepository,
                              @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaObjectMapper = kafkaObjectMapper;
    }

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        saveToOutbox(event.orderNumber(), event, "ORDER_CREATED");
    }

    public void sendOrderConfirmedEvent(OrderConfirmedEvent event) {
        saveToOutbox(event.orderNumber(), event, "ORDER_CONFIRMED");
    }

    public void sendOrderCancelledEvent(OrderCancelledEvent event) {
        saveToOutbox(event.orderNumber(), event, "ORDER_CANCELLED");
    }

    public void sendOrderShippedEvent(OrderShippedEvent event) {
        saveToOutbox(event.orderNumber(), event, "ORDER_SHIPPED");
    }

    public void sendOrderDeliveredEvent(OrderDeliveredEvent event) {
        saveToOutbox(event.orderNumber(), event, "ORDER_DELIVERED");
    }

    private void saveToOutbox(String key, Object event, String eventType) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(key)
                    .eventType(eventType)
                    .topic(KafkaTopicConfig.ORDER_EVENTS_TOPIC)
                    .partitionKey(key)
                    .payload(payload)
                    .build());
            log.info("Saved {} to outbox for key '{}'", eventType, key);
        } catch (Exception e) {
            log.error("Failed to save {} to outbox: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event for outbox", e);
        }
    }
}
