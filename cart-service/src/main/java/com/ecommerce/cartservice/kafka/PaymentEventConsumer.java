package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.config.KafkaTopicConfig;
import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final CartService cartService;

    public PaymentEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                CartService cartService) {
        this.objectMapper = objectMapper;
        this.cartService = cartService;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.PAYMENT_EVENTS,
            groupId = KafkaTopicConfig.CART_SERVICE_GROUP
    )
    public void consumePaymentEvents(JsonNode node) {
        try {
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            if ("PAYMENT_SUCCESS".equals(eventType)) {
                UUID userId = UUID.fromString(node.get("userId").asText());
                String orderNumber = node.get("orderNumber").asText();
                log.info("Received PAYMENT_SUCCESS for order {}, clearing cart for user {}", orderNumber, userId);
                cartService.clearCart(userId);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
}
