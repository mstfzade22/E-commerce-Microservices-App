package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.config.KafkaTopicConfig;
import com.ecommerce.paymentservice.dto.event.PaymentFailedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentInitiatedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentRefundedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPaymentInitiatedEvent(PaymentInitiatedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendPaymentSuccessEvent(PaymentSuccessEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendPaymentFailedEvent(PaymentFailedEvent event) {
        send(event.userId().toString(), event);
    }

    public void sendPaymentRefundedEvent(PaymentRefundedEvent event) {
        send(event.userId().toString(), event);
    }

    private void send(String key, Object event) {
        log.info("Sending event to topic {}: {}", KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, event);
        kafkaTemplate.send(KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, key, event);
    }
}
