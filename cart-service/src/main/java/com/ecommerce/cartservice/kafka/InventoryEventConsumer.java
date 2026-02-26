package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.config.KafkaTopicConfig;
import com.ecommerce.cartservice.dto.event.StockUpdatedEvent;
import com.ecommerce.cartservice.service.CartService;
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
    private final CartService cartService;

    public InventoryEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                  CartService cartService) {
        this.objectMapper = objectMapper;
        this.cartService = cartService;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_EVENTS,
            groupId = KafkaTopicConfig.CART_SERVICE_GROUP
    )
    public void consumeInventoryEvents(JsonNode node) {
        try {
            StockUpdatedEvent event = objectMapper.treeToValue(node, StockUpdatedEvent.class);
            if ("STOCK_UPDATED".equals(event.eventType())) {
                log.info("Received StockUpdatedEvent for product {}: {}", event.productId(), event.stockStatus());
                cartService.handleStockUpdate(event);
            }
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", e.getMessage(), e);
        }
    }
}