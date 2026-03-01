package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.config.KafkaTopicConfig;
import com.ecommerce.orderservice.dto.event.StockConfirmedEvent;
import com.ecommerce.orderservice.dto.event.StockReleasedEvent;
import com.ecommerce.orderservice.dto.event.StockReservedEvent;
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

    public InventoryEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.ORDER_SERVICE_GROUP
    )
    public void consumeInventoryEvents(JsonNode node) {
        try {
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            switch (eventType) {
                case "STOCK_RESERVED" -> {
                    StockReservedEvent event = objectMapper.treeToValue(node, StockReservedEvent.class);
                    log.info("Received StockReservedEvent for product {} on order {}: reserved {} units",
                            event.productId(), event.orderId(), event.reservedQuantity());
                }
                case "STOCK_CONFIRMED" -> {
                    StockConfirmedEvent event = objectMapper.treeToValue(node, StockConfirmedEvent.class);
                    log.info("Received StockConfirmedEvent for product {} on order {}: confirmed {} units",
                            event.productId(), event.orderId(), event.confirmedQuantity());
                }
                case "STOCK_RELEASED" -> {
                    StockReleasedEvent event = objectMapper.treeToValue(node, StockReleasedEvent.class);
                    log.info("Received StockReleasedEvent for product {} on order {}: released {} units",
                            event.productId(), event.orderId(), event.releasedQuantity());
                }
                default -> log.debug("Ignoring inventory event with type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", e.getMessage(), e);
        }
    }
}
