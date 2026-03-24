package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();

    void deleteByPublishedTrueAndPublishedAtBefore(Instant before);
}
