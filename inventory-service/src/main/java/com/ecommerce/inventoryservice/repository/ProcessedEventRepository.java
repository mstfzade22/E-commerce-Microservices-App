package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByEventId(String eventId);
}
