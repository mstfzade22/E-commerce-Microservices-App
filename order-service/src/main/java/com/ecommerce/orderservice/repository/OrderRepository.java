package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "statusHistory"})
    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"items", "statusHistory"})
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, UUID userId);

    Page<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURRENT_DATE", nativeQuery = true)
    long countByCreatedAtToday();
}