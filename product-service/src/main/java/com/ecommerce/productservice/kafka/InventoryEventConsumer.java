package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.config.KafkaTopicConfig;
import com.ecommerce.productservice.dto.event.InventoryStockUpdatedEvent;
import com.ecommerce.productservice.entity.StockStatus;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final ProductService productService;

    /**
     * Consumes stock update events from Inventory Service.
     * Updates the product's stockStatus field based on the new quantity.
     */
    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockUpdatedEvent(
            @Payload InventoryStockUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received InventoryStockUpdatedEvent - eventId: {}, productId: {}, stockStatus: {}, partition: {}, offset: {}",
                event.eventId(), event.productId(), event.stockStatus(), partition, offset);

        if (event.productId() == null) {
            log.error("Received event with null productId, skipping - eventId: {}", event.eventId());
            return;
        }

        try {
            // Get effective stock status (either from event or calculated from quantity)
            StockStatus effectiveStatus = event.getEffectiveStockStatus();

            productService.updateStockStatus(event.productId(), effectiveStatus);

            log.info("Successfully updated stock status for product: {} to {}",
                    event.productId(), effectiveStatus);

        } catch (ResourceNotFoundException e) {
            // Product doesn't exist in our DB - this can happen if product was deleted
            // or if Inventory Service has stale data. Log and skip.
            log.warn("Product not found for stock update - productId: {}, eventId: {}. Skipping event.",
                    event.productId(), event.eventId());

        } catch (Exception e) {
            log.error("Failed to process InventoryStockUpdatedEvent for product: {} - {}",
                    event.productId(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry via error handler
        }
    }

}
