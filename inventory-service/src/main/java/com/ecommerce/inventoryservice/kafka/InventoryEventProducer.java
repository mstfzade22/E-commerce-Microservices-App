package com.ecommerce.inventoryservice.kafka;

import com.ecommerce.inventoryservice.config.KafkaTopicConfig;
import com.ecommerce.inventoryservice.dto.event.*;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.OutboxEvent;
import com.ecommerce.inventoryservice.entity.StockReservation;
import com.ecommerce.inventoryservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class InventoryEventProducer {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper kafkaObjectMapper;

    public InventoryEventProducer(OutboxEventRepository outboxRepository,
                                  @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaObjectMapper = kafkaObjectMapper;
    }

    public void sendStockUpdatedEvent(Inventory inventory) {
        StockUpdatedEvent event = new StockUpdatedEvent(
                UUID.randomUUID().toString(),
                "STOCK_UPDATED",
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getStockStatus(),
                Instant.now()
        );
        saveToOutbox(event.productId().toString(), event, "STOCK_UPDATED");
    }

    public void sendStockReservedEvent(StockReservation reservation, Inventory inventory) {
        StockReservedEvent event = new StockReservedEvent(
                UUID.randomUUID().toString(),
                "STOCK_RESERVED",
                reservation.getProductId(),
                reservation.getOrderId(),
                reservation.getQuantity(),
                inventory.getQuantity() - inventory.getReservedQuantity(),
                inventory.getStockStatus(),
                Instant.now()
        );
        saveToOutbox(event.productId().toString(), event, "STOCK_RESERVED");
    }

    public void sendStockReleasedEvent(StockReservation reservation, Inventory inventory) {
        StockReleasedEvent event = new StockReleasedEvent(
                UUID.randomUUID().toString(),
                "STOCK_RELEASED",
                reservation.getProductId(),
                reservation.getOrderId(),
                reservation.getQuantity(),
                inventory.getQuantity() - inventory.getReservedQuantity(),
                inventory.getStockStatus(),
                Instant.now()
        );
        saveToOutbox(event.productId().toString(), event, "STOCK_RELEASED");
    }

    public void sendStockConfirmedEvent(StockReservation reservation, Inventory inventory) {
        StockConfirmedEvent event = new StockConfirmedEvent(
                UUID.randomUUID().toString(),
                "STOCK_CONFIRMED",
                reservation.getProductId(),
                reservation.getOrderId(),
                reservation.getQuantity(),
                inventory.getQuantity() - inventory.getReservedQuantity(),
                inventory.getStockStatus(),
                Instant.now()
        );
        saveToOutbox(event.productId().toString(), event, "STOCK_CONFIRMED");
    }

    private void saveToOutbox(String key, Object event, String eventType) {
        try {
            String payload = kafkaObjectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("Inventory")
                    .aggregateId(key)
                    .eventType(eventType)
                    .topic(KafkaTopicConfig.INVENTORY_EVENTS_TOPIC)
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
