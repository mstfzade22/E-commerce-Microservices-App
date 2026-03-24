package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<OrderStatusHistory> findByOrderOrderNumberOrderByCreatedAtDesc(String orderNumber);
}