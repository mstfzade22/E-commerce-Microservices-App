package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();

    void deleteByPublishedTrueAndPublishedAtBefore(Instant before);
}
