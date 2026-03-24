package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {
    List<FailedEvent> findByStatusAndNextRetryAtBefore(FailedEvent.Status status, Instant now);
    long countByStatus(FailedEvent.Status status);
}
