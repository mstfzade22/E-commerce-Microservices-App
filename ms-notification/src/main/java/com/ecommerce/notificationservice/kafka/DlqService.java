package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.config.KafkaTopicConfig;
import com.ecommerce.notificationservice.entity.FailedEvent;
import com.ecommerce.notificationservice.repository.FailedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class DlqService {

    private static final int MAX_RETRIES = 5;

    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper kafkaObjectMapper;
    private final OrderEventConsumer orderEventConsumer;
    private final PaymentEventConsumer paymentEventConsumer;
    private final InventoryEventConsumer inventoryEventConsumer;
    private final Map<String, Consumer<JsonNode>> topicHandlers = new HashMap<>();

    public DlqService(FailedEventRepository failedEventRepository,
                      @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper,
                      OrderEventConsumer orderEventConsumer,
                      PaymentEventConsumer paymentEventConsumer,
                      InventoryEventConsumer inventoryEventConsumer) {
        this.failedEventRepository = failedEventRepository;
        this.kafkaObjectMapper = kafkaObjectMapper;
        this.orderEventConsumer = orderEventConsumer;
        this.paymentEventConsumer = paymentEventConsumer;
        this.inventoryEventConsumer = inventoryEventConsumer;
    }

    @PostConstruct
    void initHandlers() {
        topicHandlers.put(KafkaTopicConfig.ORDER_EVENTS_TOPIC, orderEventConsumer::consumeOrderEvents);
        topicHandlers.put(KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, paymentEventConsumer::consumePaymentEvents);
        topicHandlers.put(KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, inventoryEventConsumer::consumeInventoryEvents);
    }

    public void saveFailedEvent(ConsumerRecord<?, ?> record, Exception exception) {
        try {
            FailedEvent failedEvent = FailedEvent.builder()
                    .topic(record.topic())
                    .partitionId(record.partition())
                    .offsetId(record.offset())
                    .recordKey(record.key() != null ? record.key().toString() : null)
                    .payload(record.value() != null ? record.value().toString() : "")
                    .consumerGroup(KafkaTopicConfig.NOTIFICATION_SERVICE_GROUP)
                    .errorMessage(exception.getMessage())
                    .stackTrace(truncateStackTrace(exception))
                    .maxRetries(MAX_RETRIES)
                    .nextRetryAt(Instant.now().plusSeconds(120))
                    .build();

            failedEventRepository.save(failedEvent);
            log.warn("DLQ: saved failed event - topic: {}, partition: {}, offset: {}",
                    record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            log.error("DLQ: failed to persist event - topic: {}, partition: {}, offset: {}",
                    record.topic(), record.partition(), record.offset(), e);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedEvents() {
        List<FailedEvent> retryable = failedEventRepository
                .findByStatusAndNextRetryAtBefore(FailedEvent.Status.FAILED, Instant.now());

        if (retryable.isEmpty()) return;
        log.info("DLQ retry: found {} events to retry", retryable.size());

        for (FailedEvent event : retryable) {
            retryEvent(event);
        }
    }

    private void retryEvent(FailedEvent event) {
        Consumer<JsonNode> handler = topicHandlers.get(event.getTopic());
        if (handler == null) {
            log.warn("DLQ: no handler for topic {}", event.getTopic());
            return;
        }

        try {
            JsonNode payload = kafkaObjectMapper.readTree(event.getPayload());
            handler.accept(payload);
            event.setStatus(FailedEvent.Status.RESOLVED);
            event.setResolvedAt(Instant.now());
            log.info("DLQ: event {} resolved on retry {}", event.getId(), event.getRetryCount() + 1);
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setErrorMessage(e.getMessage());
            if (event.getRetryCount() >= event.getMaxRetries()) {
                event.setStatus(FailedEvent.Status.EXHAUSTED);
                log.error("DLQ: event {} exhausted after {} retries for topic {}",
                        event.getId(), event.getRetryCount(), event.getTopic());
            } else {
                long backoffSeconds = (long) Math.pow(2, event.getRetryCount()) * 120;
                event.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds));
                log.warn("DLQ: event {} retry {} failed, next at {}",
                        event.getId(), event.getRetryCount(), event.getNextRetryAt());
            }
        }
        failedEventRepository.save(event);
    }

    private String truncateStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();
        return trace.length() > 4000 ? trace.substring(0, 4000) : trace;
    }
}
