package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.config.KafkaTopicConfig;
import com.ecommerce.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public OrderEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                              PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.PAYMENT_SERVICE_GROUP
    )
    public void consumeOrderEvents(JsonNode node) {
        try {
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            switch (eventType) {
                case "ORDER_CANCELLED" -> {
                    String orderNumber = node.get("orderNumber").asText();
                    String reason = node.has("reason") ? node.get("reason").asText() : "Order cancelled";
                    log.info("Received ORDER_CANCELLED event for order: {}", orderNumber);
                    paymentService.handleOrderCancelled(orderNumber, reason);
                }
                default -> log.debug("Ignoring order event with type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }
}
