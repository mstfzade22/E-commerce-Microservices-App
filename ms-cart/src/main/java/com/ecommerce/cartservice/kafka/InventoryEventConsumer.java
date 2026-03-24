package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.config.KafkaTopicConfig;
import com.ecommerce.cartservice.dto.event.StockUpdatedEvent;
import com.ecommerce.cartservice.entity.ProcessedEvent;
import com.ecommerce.cartservice.repository.ProcessedEventRepository;
import com.ecommerce.cartservice.service.CartService;
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
    private final CartService cartService;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                  CartService cartService,
                                  ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.cartService = cartService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_EVENTS,
            groupId = KafkaTopicConfig.CART_SERVICE_GROUP
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

            if ("STOCK_UPDATED".equals(eventType)) {
                StockUpdatedEvent event = objectMapper.treeToValue(node, StockUpdatedEvent.class);
                log.info("Received StockUpdatedEvent for product {}: {}", event.productId(), event.stockStatus());
                cartService.handleStockUpdate(event);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }
}
