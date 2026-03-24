package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.config.KafkaTopicConfig;
import com.ecommerce.orderservice.dto.event.PriceChangedEvent;
import com.ecommerce.orderservice.dto.event.ProductDeletedEvent;
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

    public ProductEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.PRODUCT_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.ORDER_SERVICE_GROUP
    )
    public void consumeProductEvents(JsonNode node) {
        try {
            if (node.has("newPrice")) {
                PriceChangedEvent event = objectMapper.treeToValue(node, PriceChangedEvent.class);
                log.info("Received PriceChangedEvent for product {}: {} -> {} (does not affect existing orders)",
                        event.id(), event.oldPrice(), event.newPrice());
            } else if (node.has("deletedAt")) {
                ProductDeletedEvent event = objectMapper.treeToValue(node, ProductDeletedEvent.class);
                log.info("Received ProductDeletedEvent for product {} (does not affect existing orders)", event.id());
            }
        } catch (Exception e) {
            log.error("Error processing product event: {}", e.getMessage(), e);
        }
    }
}
