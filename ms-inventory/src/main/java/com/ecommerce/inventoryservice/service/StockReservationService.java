package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.config.RedisConfig;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.ReservationStatus;
import com.ecommerce.inventoryservice.entity.StockReservation;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.kafka.InventoryEventProducer;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final InventoryEventProducer eventProducer;

    /**
     * Locks stock for an incoming order. 
     * Evicts the Redis cache so the next read fetches the newly calculated Available to Promise (ATP) stock.
     */
    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.INVENTORY, key = "#productId")
    public void reserveStock(String orderId, Long productId, Integer quantity) {
        log.info("Attempting to reserve {} items of product {} for order {}", quantity, productId, orderId);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));

        int availableQuantity = inventory.getQuantity() - inventory.getReservedQuantity();

        if (availableQuantity < quantity) {
            log.error("Insufficient stock for product {}. Requested: {}, Available: {}", productId, quantity, availableQuantity);
            throw new InsufficientStockException("Not enough stock available to fulfill reservation.");
        }

        // 1. Create Reservation (Locks for 15 minutes by default)
        StockReservation reservation = StockReservation.builder()
                .productId(productId)
                .orderId(orderId)
                .quantity(quantity)
                .status(ReservationStatus.PENDING)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        reservationRepository.save(reservation);

        // 2. Update Inventory
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);

        // 3. Publish Kafka Event
        eventProducer.sendStockReservedEvent(reservation, inventory);
        log.info("Successfully reserved {} items of product {} for order {}", quantity, productId, orderId);
    }

    /**
     * Releases the locked stock back to the available pool if an order is cancelled or payment fails.
     */
    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.INVENTORY, key = "#productId")
    public void releaseStock(String orderId, Long productId) {
        log.info("Attempting to release stock for product {} on order {}", productId, orderId);

        StockReservation reservation = getPendingReservation(orderId, productId);
        Inventory inventory = getInventory(productId);

        // 1. Update Reservation Status
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        // 2. Return reserved quantity to the pool
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        inventoryRepository.save(inventory);

        // 3. Publish Kafka Event
        eventProducer.sendStockReleasedEvent(reservation, inventory);
        log.info("Successfully released stock for product {} on order {}", productId, orderId);
    }

    /**
     * Confirms the stock deduction when a payment is successful. 
     * This physically removes the items from the warehouse count.
     */
    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.INVENTORY, key = "#productId")
    public void confirmStock(String orderId, Long productId) {
        log.info("Attempting to confirm stock deduction for product {} on order {}", productId, orderId);

        StockReservation reservation = getPendingReservation(orderId, productId);
        Inventory inventory = getInventory(productId);

        // 1. Update Reservation Status
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        // 2. Deduct from ACTUAL quantity and remove from reserved pool
        inventory.setQuantity(inventory.getQuantity() - reservation.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());

        // Note: The @PreUpdate in the Inventory entity will automatically recalculate the 
        // StockStatus (AVAILABLE, LOW_STOCK, OUT_OF_STOCK) when quantity drops!
        inventoryRepository.save(inventory);

        // 3. Publish Kafka Event
        eventProducer.sendStockConfirmedEvent(reservation, inventory);

        // 4. Important: We also publish a standard STOCK_UPDATED event because the actual warehouse quantity changed.
        // This ensures the Product Service updates its database.
        eventProducer.sendStockUpdatedEvent(inventory);

        log.info("Successfully confirmed stock deduction for product {} on order {}", productId, orderId);
    }

    /**
     * Background job to automatically release expired "abandoned cart" reservations.
     * Runs every minute. Note: Add @EnableScheduling to your main Application class to activate this.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredReservations() {
        List<StockReservation> expiredReservations = reservationRepository
                .findAllByStatusAndExpiresAtBefore(ReservationStatus.PENDING, Instant.now());

        for (StockReservation reservation : expiredReservations) {
            log.info("Releasing expired reservation {} for order {}", reservation.getId(), reservation.getOrderId());

            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            inventoryRepository.findByProductId(reservation.getProductId()).ifPresent(inventory -> {
                inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
                inventoryRepository.save(inventory);
                eventProducer.sendStockReleasedEvent(reservation, inventory);

                // Programmatic cache eviction since we aren't passing the productId as a method argument
                // In a real scenario, you'd inject the CacheManager here to evict the specific key.
            });
        }
    }


    private StockReservation getPendingReservation(String orderId, Long productId) {
        return reservationRepository.findByOrderIdAndProductId(orderId, productId)
                .filter(res -> res.getStatus() == ReservationStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No PENDING reservation found for order " + orderId + " and product " + productId));
    }

    private Inventory getInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
    }
}