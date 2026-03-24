package com.ecommerce.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "failed_events", indexes = {
        @Index(name = "idx_failed_event_status_retry", columnList = "status, next_retry_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedEvent {

    public enum Status {
        FAILED, RESOLVED, EXHAUSTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "partition_id")
    private Integer partitionId;

    @Column(name = "offset_id")
    private Long offsetId;

    @Column(name = "record_key", length = 200)
    private String recordKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Builder.Default
    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 5;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.FAILED;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
