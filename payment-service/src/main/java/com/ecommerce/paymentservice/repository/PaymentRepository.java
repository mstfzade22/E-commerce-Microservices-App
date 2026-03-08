package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderNumber(String orderNumber);

    Optional<Payment> findByKapitalOrderId(String kapitalOrderId);

    Page<Payment> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Payment> findAllByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    boolean existsByOrderNumberAndStatusNot(String orderNumber, PaymentStatus status);

    boolean existsByOrderNumberAndStatusIn(String orderNumber, java.util.Collection<PaymentStatus> statuses);
}
