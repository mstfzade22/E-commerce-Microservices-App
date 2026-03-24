package com.ecommerce.inventoryservice.kafka;

import com.ecommerce.inventoryservice.config.KafkaTopicConfig;
import com.ecommerce.inventoryservice.dto.event.ProductCreatedEvent;
import com.ecommerce.inventoryservice.dto.event.ProductDeletedEvent;
import com.ecommerce.inventoryservice.dto.event.ProductUpdatedEvent;
import com.ecommerce.inventoryservice.entity.ProcessedEvent;
import com.ecommerce.inventoryservice.repository.ProcessedEventRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
public class ProductEventConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final ProcessedEventRepository processedEventRepository;

    public ProductEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                InventoryService inventoryService,
                                ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(topics = KafkaTopicConfig.PRODUCT_EVENTS_TOPIC, groupId = KafkaTopicConfig.INVENTORY_SERVICE_GROUP)
    public void consumeProductEvents(JsonNode payload) {
        try {
            String eventId = payload.has("eventId") ? payload.get("eventId").asText() : null;

            if (eventId != null) {
                try {
                    processedEventRepository.saveAndFlush(ProcessedEvent.builder()
                            .eventId(eventId)
                            .eventType("PRODUCT_EVENT")
                            .processedAt(Instant.now())
                            .build());
                } catch (DataIntegrityViolationException e) {
                    log.info("Event {} already processed, skipping", eventId);
                    return;
                }
            }

            if (payload.has("deletedAt")) {
                ProductDeletedEvent event = objectMapper.treeToValue(payload, ProductDeletedEvent.class);
                log.info("Consumed ProductDeletedEvent for productId: {}", event.id());
                inventoryService.deleteInventory(event.id());

            } else if (payload.has("updatedAt") && !payload.get("updatedAt").isNull()) {
                ProductUpdatedEvent event = objectMapper.treeToValue(payload, ProductUpdatedEvent.class);
                log.info("Consumed ProductUpdatedEvent for productId: {}", event.id());
                if (event.stock() != null) {
                    inventoryService.updateOrCreateStock(event.id(), event.stock());
                }

            } else if (payload.has("categoryId")) {
                ProductCreatedEvent event = objectMapper.treeToValue(payload, ProductCreatedEvent.class);
                log.info("Consumed ProductCreatedEvent for productId: {}", event.id());
                inventoryService.createInventory(event);

            } else {
                log.debug("Consumed an unhandled product event type. Payload: {}", payload);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (Exception e) {
            log.error("Failed to process consumed product event. Payload: {}", payload, e);
            throw new RuntimeException("Failed to process product event", e);
        }
    }
}
