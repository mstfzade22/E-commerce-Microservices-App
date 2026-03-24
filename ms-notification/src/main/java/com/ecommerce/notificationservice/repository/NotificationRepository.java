package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findAllByStatusOrderByCreatedAtDesc(NotificationStatus status, Pageable pageable);

    List<Notification> findAllByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            NotificationStatus status, Integer maxRetries);

    boolean existsByTypeAndReferenceIdAndRecipientEmail(
            NotificationType type, String referenceId, String recipientEmail);
}
