package com.ecommerce.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_number", columnList = "order_number"),
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_kapital_order_id", columnList = "kapital_order_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "AZN";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "kapital_order_id", unique = true, length = 50)
    private String kapitalOrderId;

    @Column(name = "kapital_password", length = 100)
    private String kapitalPassword;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(length = 500)
    private String description;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<PaymentStatusHistory> statusHistory = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void addStatusHistory(PaymentStatusHistory history) {
        statusHistory.add(history);
        history.setPayment(this);
    }
}
