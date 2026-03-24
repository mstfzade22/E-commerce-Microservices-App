package com.ecommerce.notificationservice.dto.response;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        Long id,
        UUID userId,
        String recipientEmail,
        NotificationType type,
        String subject,
        String referenceId,
        NotificationStatus status,
        String errorMessage,
        Integer retryCount,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getRecipientEmail(),
                notification.getType(),
                notification.getSubject(),
                notification.getReferenceId(),
                notification.getStatus(),
                notification.getErrorMessage(),
                notification.getRetryCount(),
                notification.getCreatedAt()
        );
    }
}
