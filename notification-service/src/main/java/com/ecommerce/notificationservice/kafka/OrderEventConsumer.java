package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.config.KafkaTopicConfig;
import com.ecommerce.notificationservice.dto.event.OrderCancelledEvent;
import com.ecommerce.notificationservice.dto.event.OrderConfirmedEvent;
import com.ecommerce.notificationservice.dto.event.OrderDeliveredEvent;
import com.ecommerce.notificationservice.dto.event.OrderShippedEvent;
import com.ecommerce.notificationservice.entity.ProcessedEvent;
import com.ecommerce.notificationservice.repository.ProcessedEventRepository;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    public OrderEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                              NotificationService notificationService,
                              ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.NOTIFICATION_SERVICE_GROUP
    )
    public void consumeOrderEvents(JsonNode node) {
        try {
            String eventId = node.has("eventId") ? node.get("eventId").asText() : null;
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            if (eventId != null) {
                try {
                    processedEventRepository.saveAndFlush(ProcessedEvent.builder()
                            .eventId(eventId)
                            .eventType(eventType)
                            .processedAt(Instant.now())
                            .build());
                } catch (DataIntegrityViolationException e) {
                    log.info("Event {} already processed, skipping", eventId);
                    return;
                }
            }

            switch (eventType) {
                case "ORDER_CONFIRMED" -> {
                    OrderConfirmedEvent event = objectMapper.treeToValue(node, OrderConfirmedEvent.class);
                    log.info("Received ORDER_CONFIRMED event for order {}", event.orderNumber());
                    notificationService.handleOrderConfirmed(event);
                }
                case "ORDER_SHIPPED" -> {
                    OrderShippedEvent event = objectMapper.treeToValue(node, OrderShippedEvent.class);
                    log.info("Received ORDER_SHIPPED event for order {}", event.orderNumber());
                    notificationService.handleOrderShipped(event);
                }
                case "ORDER_DELIVERED" -> {
                    OrderDeliveredEvent event = objectMapper.treeToValue(node, OrderDeliveredEvent.class);
                    log.info("Received ORDER_DELIVERED event for order {}", event.orderNumber());
                    notificationService.handleOrderDelivered(event);
                }
                case "ORDER_CANCELLED" -> {
                    OrderCancelledEvent event = objectMapper.treeToValue(node, OrderCancelledEvent.class);
                    log.info("Received ORDER_CANCELLED event for order {}", event.orderNumber());
                    notificationService.handleOrderCancelled(event);
                }
                default -> log.debug("Ignoring order event with type: {}", eventType);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
