package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.repositories.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;

    public OutboxPublisher(OutboxEventRepository outboxRepository,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaObjectMapper = kafkaObjectMapper;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(),
                        kafkaObjectMapper.readTree(event.getPayload())).get();
                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                break;
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupPublishedEvents() {
        outboxRepository.deleteByPublishedTrueAndPublishedAtBefore(
                Instant.now().minus(7, ChronoUnit.DAYS));
    }
}
