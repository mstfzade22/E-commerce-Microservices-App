package com.ecommerce.inventoryservice.kafka;

import com.ecommerce.inventoryservice.config.KafkaTopicConfig;
import com.ecommerce.inventoryservice.dto.event.ProductCreatedEvent;
import com.ecommerce.inventoryservice.dto.event.ProductDeletedEvent;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductEventConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;

    public ProductEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                InventoryService inventoryService) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = KafkaTopicConfig.PRODUCT_EVENTS_TOPIC, groupId = KafkaTopicConfig.INVENTORY_SERVICE_GROUP)
    public void consumeProductEvents(JsonNode payload) {
        try {
            // Differentiate events based on unique payload fields from product-service
            if (payload.has("deletedAt")) {
                ProductDeletedEvent event = objectMapper.treeToValue(payload, ProductDeletedEvent.class);
                log.info("Consumed ProductDeletedEvent from {} for productId: {}", KafkaTopicConfig.PRODUCT_EVENTS_TOPIC, event.id());
                inventoryService.deleteInventory(event.id());

            } else if (payload.has("categoryId")) {
                ProductCreatedEvent event = objectMapper.treeToValue(payload, ProductCreatedEvent.class);
                log.info("Consumed ProductCreatedEvent from {} for productId: {}", KafkaTopicConfig.PRODUCT_EVENTS_TOPIC, event.id());
                inventoryService.createInventory(event);

            } else {
                log.debug("Consumed an unhandled product event type. Payload: {}", payload);
            }
        } catch (Exception e) {
            log.error("Failed to process consumed product event. Payload: {}", payload, e);
            // In a production environment, you would route to a Dead Letter Queue (DLQ) here
        }
    }
}