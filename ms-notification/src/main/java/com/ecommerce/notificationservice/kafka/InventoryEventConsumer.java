package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.config.KafkaTopicConfig;
import com.ecommerce.notificationservice.dto.event.StockUpdatedEvent;
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
public class InventoryEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                  NotificationService notificationService,
                                  ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.NOTIFICATION_SERVICE_GROUP
    )
    public void consumeInventoryEvents(JsonNode node) {
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
                case "STOCK_UPDATED" -> {
                    StockUpdatedEvent event = objectMapper.treeToValue(node, StockUpdatedEvent.class);
                    log.info("Received STOCK_UPDATED event for product {}", event.productId());
                    notificationService.handleStockUpdated(event);
                }
                default -> log.debug("Ignoring inventory event with type: {}", eventType);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }
}
