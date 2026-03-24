package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseNotificationChannel implements NotificationChannel {

    private final SseEmitterService sseEmitterService;

    @Override
    public void send(Notification savedNotification) {
        try {
            sseEmitterService.pushToUser(savedNotification);
        } catch (Exception e) {
            log.warn("SSE push failed for notification {}: {}", savedNotification.getId(), e.getMessage());
        }
    }
}
