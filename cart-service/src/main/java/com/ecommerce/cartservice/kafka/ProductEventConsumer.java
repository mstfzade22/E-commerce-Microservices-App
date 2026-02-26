package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.config.KafkaTopicConfig;
import com.ecommerce.cartservice.dto.event.PriceChangedEvent;
import com.ecommerce.cartservice.dto.event.ProductDeletedEvent;
import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductEventConsumer {

    private final ObjectMapper objectMapper;
    private final CartService cartService;

    public ProductEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                CartService cartService) {
        this.objectMapper = objectMapper;
        this.cartService = cartService;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.PRODUCT_EVENTS,
            groupId = KafkaTopicConfig.CART_SERVICE_GROUP
    )
    public void consumeProductEvents(JsonNode node) {
        try {
            if (node.has("newPrice")) {
                PriceChangedEvent event = objectMapper.treeToValue(node, PriceChangedEvent.class);
                log.info("Received PriceChangedEvent for product {}", event.id());
                cartService.handlePriceChange(event);
            } else if (node.has("deletedAt")) {
                ProductDeletedEvent event = objectMapper.treeToValue(node, ProductDeletedEvent.class);
                log.info("Received ProductDeletedEvent for product {}", event.id());
                cartService.handleProductDeletion(event.id());
            }
        } catch (Exception e) {
            log.error("Error processing product event: {}", e.getMessage(), e);
        }
    }
}