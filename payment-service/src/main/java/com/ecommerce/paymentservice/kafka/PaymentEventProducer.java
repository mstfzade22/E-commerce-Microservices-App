package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.config.KafkaTopicConfig;
import com.ecommerce.paymentservice.dto.event.PaymentFailedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentInitiatedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentRefundedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentSuccessEvent;
import com.ecommerce.paymentservice.entity.OutboxEvent;
import com.ecommerce.paymentservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventProducer {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper kafkaObjectMapper;

    public PaymentEventProducer(OutboxEventRepository outboxRepository,
                                @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaObjectMapper = kafkaObjectMapper;
    }

    public void sendPaymentInitiatedEvent(PaymentInitiatedEvent event) {
        saveToOutbox(event.orderNumber(), event, "PAYMENT_INITIATED");
    }

    public void sendPaymentSuccessEvent(PaymentSuccessEvent event) {
        saveToOutbox(event.orderNumber(), event, "PAYMENT_SUCCESS");
    }

    public void sendPaymentFailedEvent(PaymentFailedEvent event) {
        saveToOutbox(event.orderNumber(), event, "PAYMENT_FAILED");
    }

    public void sendPaymentRefundedEvent(PaymentRefundedEvent event) {
        saveToOutbox(event.orderNumber(), event, "PAYMENT_REFUNDED");
    }

    private void saveToOutbox(String key, Object event, String eventType) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("Payment")
                    .aggregateId(key)
                    .eventType(eventType)
                    .topic(KafkaTopicConfig.PAYMENT_EVENTS_TOPIC)
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
