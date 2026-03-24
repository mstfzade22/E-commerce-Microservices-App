package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByEventId(String eventId);
    Optional<ProcessedEvent> findByEventId(String eventId);
}
