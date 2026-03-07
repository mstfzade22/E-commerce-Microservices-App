package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.response.NotificationResponse;
import com.ecommerce.notificationservice.dto.response.PagedResponse;
import com.ecommerce.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /notifications page={}, size={}", page, size);
        return ResponseEntity.ok(notificationService.getAllNotifications(page, size));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<NotificationResponse>> getNotificationsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /notifications/user/{} page={}, size={}", userId, page, size);
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId, page, size));
    }

    @GetMapping("/failed")
    public ResponseEntity<PagedResponse<NotificationResponse>> getFailedNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /notifications/failed page={}, size={}", page, size);
        return ResponseEntity.ok(notificationService.getFailedNotifications(page, size));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<NotificationResponse> retryNotification(@PathVariable Long id) {
        log.debug("POST /notifications/{}/retry", id);
        return ResponseEntity.ok(notificationService.retryNotification(id));
    }
}
