package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.config.KafkaTopicConfig;
import com.ecommerce.cartservice.dto.event.PriceChangedEvent;
import com.ecommerce.cartservice.dto.event.ProductDeletedEvent;
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
public class ProductEventConsumer {

    private final ObjectMapper objectMapper;
    private final CartService cartService;
    private final ProcessedEventRepository processedEventRepository;

    public ProductEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                CartService cartService,
                                ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.cartService = cartService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfig.PRODUCT_EVENTS,
            groupId = KafkaTopicConfig.CART_SERVICE_GROUP
    )
    public void consumeProductEvents(JsonNode node) {
        try {
            String eventId = node.has("eventId") ? node.get("eventId").asText() : null;
            String eventType = node.has("newPrice") ? "PRICE_CHANGED" : node.has("deletedAt") ? "PRODUCT_DELETED" : "UNKNOWN";

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

            if (node.has("newPrice")) {
                PriceChangedEvent event = objectMapper.treeToValue(node, PriceChangedEvent.class);
                log.info("Received PriceChangedEvent for product {}", event.id());
                cartService.handlePriceChange(event);
            } else if (node.has("deletedAt")) {
                ProductDeletedEvent event = objectMapper.treeToValue(node, ProductDeletedEvent.class);
                log.info("Received ProductDeletedEvent for product {}", event.id());
                cartService.handleProductDeletion(event.id());
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (Exception e) {
            log.error("Error processing product event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process product event", e);
        }
    }
}
