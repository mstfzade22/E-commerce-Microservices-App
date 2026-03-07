package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NotificationRetryScheduler {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public NotificationRetryScheduler(NotificationRepository notificationRepository,
                                      EmailService emailService,
                                      @Qualifier("kafkaObjectMapper") ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${notification.retry.interval-ms:900000}")
    public void retryFailedNotifications() {
        List<Notification> failed = notificationRepository
                .findAllByStatusAndRetryCountLessThanOrderByCreatedAtAsc(NotificationStatus.FAILED, 3);

        if (failed.isEmpty()) return;

        log.info("Retrying {} failed notifications", failed.size());

        for (Notification notification : failed) {
            Map<String, Object> variables = deserializeVariables(notification.getTemplateVariables());
            try {
                emailService.sendHtmlEmail(notification.getRecipientEmail(), notification.getSubject(),
                        notification.getTemplateName(), variables);
                notification.setStatus(NotificationStatus.SENT);
                notification.setErrorMessage(null);
                log.info("Retry successful for notification id={}", notification.getId());
            } catch (Exception e) {
                notification.setErrorMessage(e.getMessage());
                log.warn("Retry failed for notification id={}: {}", notification.getId(), e.getMessage());
            }
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeVariables(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize template variables: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
