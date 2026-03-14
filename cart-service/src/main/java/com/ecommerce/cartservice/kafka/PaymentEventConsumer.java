package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.config.KafkaTopicConfig;
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
import java.util.UUID;

@Component
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final CartService cartService;
    private final ProcessedEventRepository processedEventRepository;

    public PaymentEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                CartService cartService,
                                ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.cartService = cartService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfig.PAYMENT_EVENTS,
            groupId = KafkaTopicConfig.CART_SERVICE_GROUP
    )
    public void consumePaymentEvents(JsonNode node) {
        try {
            String eventId = node.has("eventId") ? node.get("eventId").asText() : null;
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

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

            if ("PAYMENT_SUCCESS".equals(eventType)) {
                UUID userId = UUID.fromString(node.get("userId").asText());
                String orderNumber = node.get("orderNumber").asText();
                log.info("Received PAYMENT_SUCCESS for order {}, clearing cart for user {}", orderNumber, userId);
                cartService.clearCart(userId);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
