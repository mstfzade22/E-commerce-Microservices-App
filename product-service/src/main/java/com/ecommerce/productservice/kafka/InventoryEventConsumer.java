package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.config.KafkaTopicConfig;
import com.ecommerce.productservice.dto.event.InventoryStockUpdatedEvent;
import com.ecommerce.productservice.entity.ProcessedEvent;
import com.ecommerce.productservice.entity.StockStatus;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.repositories.ProcessedEventRepository;
import com.ecommerce.productservice.service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
public class InventoryEventConsumer {

    private final ProductService productService;
    private final ObjectMapper kafkaObjectMapper;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryEventConsumer(ProductService productService,
                                  @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper,
                                  ProcessedEventRepository processedEventRepository) {
        this.productService = productService;
        this.kafkaObjectMapper = kafkaObjectMapper;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.PRODUCT_SERVICE_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockUpdatedEvent(
            @Payload JsonNode message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            InventoryStockUpdatedEvent event = kafkaObjectMapper.treeToValue(message, InventoryStockUpdatedEvent.class);

            log.info("Received InventoryStockUpdatedEvent - eventId: {}, productId: {}, stockStatus: {}, partition: {}, offset: {}",
                    event.eventId(), event.productId(), event.stockStatus(), partition, offset);

            String eventId = event.eventId();
            if (eventId != null) {
                try {
                    processedEventRepository.saveAndFlush(ProcessedEvent.builder()
                            .eventId(eventId)
                            .eventType("STOCK_UPDATED")
                            .processedAt(Instant.now())
                            .build());
                } catch (DataIntegrityViolationException e) {
                    log.info("Event {} already processed, skipping", eventId);
                    return;
                }
            }

            if (event.productId() == null) {
                log.error("Received event with null productId, skipping - eventId: {}", event.eventId());
                return;
            }

            StockStatus effectiveStatus = event.getEffectiveStockStatus();
            productService.updateStockStatus(event.productId(), effectiveStatus);

            log.info("Successfully updated stock status for product: {} to {}",
                    event.productId(), effectiveStatus);

        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (ResourceNotFoundException e) {
            log.warn("Product not found for stock update - partition: {}, offset: {}. Skipping event.",
                    partition, offset);
        } catch (Exception e) {
            log.error("Failed to process InventoryStockUpdatedEvent - partition: {}, offset: {}, error: {}",
                    partition, offset, e.getMessage(), e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }
}
