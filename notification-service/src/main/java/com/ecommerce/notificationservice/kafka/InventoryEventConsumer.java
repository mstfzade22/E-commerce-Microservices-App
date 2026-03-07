package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.config.KafkaTopicConfig;
import com.ecommerce.notificationservice.dto.event.StockUpdatedEvent;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public InventoryEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                  NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.NOTIFICATION_SERVICE_GROUP
    )
    public void consumeInventoryEvents(JsonNode node) {
        try {
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            switch (eventType) {
                case "STOCK_UPDATED" -> {
                    StockUpdatedEvent event = objectMapper.treeToValue(node, StockUpdatedEvent.class);
                    log.info("Received STOCK_UPDATED event for product {}", event.productId());
                    notificationService.handleStockUpdated(event);
                }
                default -> log.debug("Ignoring inventory event with type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", e.getMessage(), e);
        }
    }
}
