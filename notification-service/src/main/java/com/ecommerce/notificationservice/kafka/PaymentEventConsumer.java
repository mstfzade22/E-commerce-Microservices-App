package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.config.KafkaTopicConfig;
import com.ecommerce.notificationservice.dto.event.PaymentFailedEvent;
import com.ecommerce.notificationservice.dto.event.PaymentRefundedEvent;
import com.ecommerce.notificationservice.dto.event.PaymentSuccessEvent;
import com.ecommerce.notificationservice.entity.ProcessedEvent;
import com.ecommerce.notificationservice.repository.ProcessedEventRepository;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    public PaymentEventConsumer(@Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                                NotificationService notificationService,
                                ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(
            topics = KafkaTopicConfig.PAYMENT_EVENTS_TOPIC,
            groupId = KafkaTopicConfig.NOTIFICATION_SERVICE_GROUP
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

            switch (eventType) {
                case "PAYMENT_SUCCESS" -> {
                    PaymentSuccessEvent event = objectMapper.treeToValue(node, PaymentSuccessEvent.class);
                    log.info("Received PAYMENT_SUCCESS event for order {}", event.orderNumber());
                    notificationService.handlePaymentSuccess(event);
                }
                case "PAYMENT_FAILED" -> {
                    PaymentFailedEvent event = objectMapper.treeToValue(node, PaymentFailedEvent.class);
                    log.info("Received PAYMENT_FAILED event for order {}", event.orderNumber());
                    notificationService.handlePaymentFailed(event);
                }
                case "PAYMENT_REFUNDED" -> {
                    PaymentRefundedEvent event = objectMapper.treeToValue(node, PaymentRefundedEvent.class);
                    log.info("Received PAYMENT_REFUNDED event for order {}", event.orderNumber());
                    notificationService.handlePaymentRefunded(event);
                }
                default -> log.debug("Ignoring payment event with type: {}", eventType);
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, skipping");
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
