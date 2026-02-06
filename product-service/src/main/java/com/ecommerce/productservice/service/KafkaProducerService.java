package com.ecommerce.productservice.service;

import com.ecommerce.productservice.config.KafkaTopicConfig;
import com.ecommerce.productservice.dto.event.PriceChangedEvent;
import com.ecommerce.productservice.dto.event.ProductCreatedEvent;
import com.ecommerce.productservice.dto.event.ProductDeletedEvent;
import com.ecommerce.productservice.dto.event.ProductUpdatedEvent;
import com.ecommerce.productservice.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductCreatedEvent(Product product) {
        ProductCreatedEvent event = ProductCreatedEvent.of(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getPrice(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getIsActive(),
                product.getCreatedAt()
        );

        sendEvent(KafkaTopicConfig.PRODUCT_EVENTS_TOPIC, product.getId().toString(), event, "ProductCreatedEvent");
    }

    public void publishProductUpdatedEvent(Product product) {
        ProductUpdatedEvent event = ProductUpdatedEvent.of(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getPrice(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getIsActive(),
                product.getUpdatedAt()
        );

        sendEvent(KafkaTopicConfig.PRODUCT_EVENTS_TOPIC, product.getId().toString(), event, "ProductUpdatedEvent");
    }

    public void publishProductDeletedEvent(Long productId, String slug, String sku) {
        ProductDeletedEvent event = ProductDeletedEvent.of(productId, slug, sku);

        sendEvent(KafkaTopicConfig.PRODUCT_EVENTS_TOPIC, productId.toString(), event, "ProductDeletedEvent");
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

        sendEvent(KafkaTopicConfig.PRODUCT_EVENTS_TOPIC, product.getId().toString(), event, "PriceChangedEvent");
    }

    private void sendEvent(String topic, String key, Object event, String eventType) {
        log.info("Publishing {} to topic '{}' with key '{}'", eventType, topic, key);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} with key '{}': {}", eventType, key, ex.getMessage(), ex);
            } else {
                log.info("Successfully published {} with key '{}', partition: {}, offset: {}",
                        eventType,
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

}
