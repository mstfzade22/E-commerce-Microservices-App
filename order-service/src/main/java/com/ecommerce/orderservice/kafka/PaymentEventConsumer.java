package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.config.KafkaTopicConfig;
import com.ecommerce.orderservice.dto.event.PaymentFailedEvent;
import com.ecommerce.orderservice.dto.event.PaymentSuccessEvent;
import com.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public PaymentEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.PAYMENT_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.ORDER_SERVICE_GROUP
    )
    public void consumePaymentEvents(JsonNode node) {
        try {
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "";

            switch (eventType) {
                case "PAYMENT_SUCCESS" -> {
                    PaymentSuccessEvent event = objectMapper.treeToValue(node, PaymentSuccessEvent.class);
                    log.info("Received PaymentSuccessEvent for order {}", event.orderNumber());
                    orderService.handlePaymentSuccess(event.orderNumber());
                }
                case "PAYMENT_FAILED" -> {
                    PaymentFailedEvent event = objectMapper.treeToValue(node, PaymentFailedEvent.class);
                    log.info("Received PaymentFailedEvent for order {}: {}", event.orderNumber(), event.reason());
                    orderService.handlePaymentFailure(event.orderNumber(), event.reason());
                }
                default -> log.debug("Ignoring payment event with type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
}
