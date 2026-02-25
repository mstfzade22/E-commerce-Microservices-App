package com.ecommerce.inventoryservice.kafka;

import com.ecommerce.inventoryservice.config.KafkaTopicConfig;
import com.ecommerce.inventoryservice.dto.event.*;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.StockReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendStockUpdatedEvent(Inventory inventory) {
        StockUpdatedEvent event = new StockUpdatedEvent(
                UUID.randomUUID().toString(),
                "STOCK_UPDATED",
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getStockStatus(),
                Instant.now()
        );
        log.info("Producing STOCK_UPDATED event to {} for productId: {}", KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, event.productId());
        kafkaTemplate.send(KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, event.productId().toString(), event);
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
        log.info("Producing STOCK_RESERVED event to {} for orderId: {}", KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, event.orderId());
        kafkaTemplate.send(KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, event.productId().toString(), event);
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
        log.info("Producing STOCK_RELEASED event to {} for orderId: {}", KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, event.orderId());
        kafkaTemplate.send(KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, event.productId().toString(), event);
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
        log.info("Producing STOCK_CONFIRMED event to {} for orderId: {}", KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, event.orderId());
        kafkaTemplate.send(KafkaTopicConfig.INVENTORY_EVENTS_TOPIC, event.productId().toString(), event);
    }
}