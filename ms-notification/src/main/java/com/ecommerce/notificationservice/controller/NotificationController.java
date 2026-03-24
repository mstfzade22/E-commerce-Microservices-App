package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.response.NotificationResponse;
import com.ecommerce.notificationservice.dto.response.PagedResponse;
import com.ecommerce.notificationservice.service.NotificationService;
import com.ecommerce.notificationservice.service.SseEmitterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new IllegalStateException("Missing X-User-Id header");
        }
        UUID userId = UUID.fromString(userIdHeader);
        log.debug("SSE /notifications/stream for user {}", userId);
        return sseEmitterService.subscribe(userId);
    }

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
