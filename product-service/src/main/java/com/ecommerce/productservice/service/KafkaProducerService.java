package com.ecommerce.productservice.service;

import com.ecommerce.productservice.config.KafkaTopicConfig;
import com.ecommerce.productservice.dto.event.PriceChangedEvent;
import com.ecommerce.productservice.dto.event.ProductCreatedEvent;
import com.ecommerce.productservice.dto.event.ProductDeletedEvent;
import com.ecommerce.productservice.dto.event.ProductUpdatedEvent;
import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repositories.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class KafkaProducerService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper kafkaObjectMapper;

    public KafkaProducerService(OutboxEventRepository outboxRepository,
                                @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaObjectMapper = kafkaObjectMapper;
    }

    public void publishProductCreatedEvent(Product product, Integer initialStock) {
        ProductCreatedEvent event = ProductCreatedEvent.of(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getPrice(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getIsActive(),
                initialStock,
                product.getCreatedAt()
        );

        saveToOutbox(product.getId().toString(), event, "PRODUCT_CREATED");
    }

    public void publishProductUpdatedEvent(Product product, Integer stock) {
        ProductUpdatedEvent event = ProductUpdatedEvent.of(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getPrice(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getIsActive(),
                stock,
                product.getUpdatedAt()
        );

        saveToOutbox(product.getId().toString(), event, "PRODUCT_UPDATED");
    }

    public void publishProductDeletedEvent(Long productId, String slug, String sku) {
        ProductDeletedEvent event = ProductDeletedEvent.of(productId, slug, sku);

        saveToOutbox(productId.toString(), event, "PRODUCT_DELETED");
    }

    public void publishPriceChangedEvent(Product product, BigDecimal oldPrice, BigDecimal oldDiscountPrice) {
        PriceChangedEvent event = PriceChangedEvent.of(
                product.getId(),
                product.getSlug(),
                oldPrice,
                product.getPrice(),
                oldDiscountPrice,
                product.getDiscountPrice()
        );

        saveToOutbox(product.getId().toString(), event, "PRICE_CHANGED");
    }

    private void saveToOutbox(String key, Object event, String eventType) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("Product")
                    .aggregateId(key)
                    .eventType(eventType)
                    .topic(KafkaTopicConfig.PRODUCT_EVENTS_TOPIC)
                    .partitionKey(key)
                    .payload(payload)
                    .build());
            log.info("Saved {} to outbox for key '{}'", eventType, key);
        } catch (Exception e) {
            log.error("Failed to save {} to outbox: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event for outbox", e);
        }
    }
}
