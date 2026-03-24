package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.config.KafkaTopicConfig;
import com.ecommerce.paymentservice.entity.ProcessedEvent;
import com.ecommerce.paymentservice.repository.ProcessedEventRepository;
import com.ecommerce.paymentservice.service.PaymentService;
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
    private final PaymentService paymentService;
    private final ProcessedEventRepository processedEventRepository;

    public OrderEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                              PaymentService paymentService,
                              ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.PAYMENT_SERVICE_GROUP
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
                case "ORDER_CANCELLED" -> {
                    String orderNumber = node.get("orderNumber").asText();
                    String reason = node.has("reason") ? node.get("reason").asText() : "Order cancelled";
                    log.info("Received ORDER_CANCELLED event for order: {}", orderNumber);
                    paymentService.handleOrderCancelled(orderNumber, reason);
                }
                default -> log.debug("Ignoring order event with type: {}", eventType);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }
}
