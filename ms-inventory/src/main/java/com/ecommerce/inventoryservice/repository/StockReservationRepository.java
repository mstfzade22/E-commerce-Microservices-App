package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.ReservationStatus;
import com.ecommerce.inventoryservice.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    // Find specific reservation to CONFIRM or RELEASE
    Optional<StockReservation> findByOrderIdAndProductId(String orderId, Long productId);

    List<StockReservation> findAllByOrderId(String orderId);

    // For the background task that releases stock back to inventory
    List<StockReservation> findAllByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}