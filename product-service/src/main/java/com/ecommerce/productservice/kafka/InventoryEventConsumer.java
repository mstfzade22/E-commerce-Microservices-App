package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.config.KafkaTopicConfig;
import com.ecommerce.productservice.dto.event.StockUpdatedEvent;
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

    @KafkaListener(
            topics = KafkaTopicConfig.INVENTORY_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockUpdatedEvent(
            @Payload StockUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received StockUpdatedEvent - productId: {}, stockStatus: {}, partition: {}, offset: {}",
                event.productId(), event.stockStatus(), partition, offset);

        try {
            productService.updateStockStatus(event.productId(), event.stockStatus());
            log.info("Successfully processed StockUpdatedEvent for product: {}", event.productId());

        } catch (Exception e) {
            log.error("Failed to process StockUpdatedEvent for product: {} - {}",
                    event.productId(), e.getMessage(), e);
            throw e;
        }
    }

}
