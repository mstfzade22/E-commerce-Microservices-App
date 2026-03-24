package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.client.UserServiceClient;
import com.ecommerce.notificationservice.dto.UserInfo;
import com.ecommerce.notificationservice.dto.event.*;
import com.ecommerce.notificationservice.dto.response.NotificationResponse;
import com.ecommerce.notificationservice.dto.response.PagedResponse;
import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.exception.NotificationNotFoundException;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final UserServiceClient userServiceClient;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final List<NotificationChannel> channels;

    @Value("${notification.admin-email}")
    private String adminEmail;

    public NotificationService(EmailService emailService,
                               UserServiceClient userServiceClient,
                               NotificationRepository notificationRepository,
                               @Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                               List<NotificationChannel> channels) {
        this.emailService = emailService;
        this.userServiceClient = userServiceClient;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
        this.channels = channels;
    }

    // ========== Order Event Handlers ==========

    @Async
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderNumber", event.orderNumber());

        notifyCustomer(event.userId(), NotificationType.ORDER_CONFIRMED,
                "Order Confirmed - " + event.orderNumber(),
                "order-confirmed", vars, event.orderNumber());

        notifyAdmin(NotificationType.ORDER_CONFIRMED,
                "New Order - " + event.orderNumber(),
                "admin-new-order", vars, event.orderNumber());
    }

    @Async
    public void handleOrderShipped(OrderShippedEvent event) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderNumber", event.orderNumber());

        notifyCustomer(event.userId(), NotificationType.ORDER_SHIPPED,
                "Order Shipped - " + event.orderNumber(),
                "order-shipped", vars, event.orderNumber());
    }

    @Async
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderNumber", event.orderNumber());

        notifyCustomer(event.userId(), NotificationType.ORDER_DELIVERED,
                "Order Delivered - " + event.orderNumber(),
                "order-delivered", vars, event.orderNumber());
    }

    @Async
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderNumber", event.orderNumber());
        vars.put("reason", event.reason());

        notifyCustomer(event.userId(), NotificationType.ORDER_CANCELLED,
                "Order Cancelled - " + event.orderNumber(),
                "order-cancelled", vars, event.orderNumber());
    }

    // ========== Payment Event Handlers ==========

    @Async
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderNumber", event.orderNumber());
        vars.put("amount", event.amount());

        notifyCustomer(event.userId(), NotificationType.PAYMENT_SUCCESS,
                "Payment Successful - " + event.orderNumber(),
                "payment-success", vars, event.orderNumber());
    }

    @Async
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderNumber", event.orderNumber());
        vars.put("reason", event.reason());

        notifyCustomer(event.userId(), NotificationType.PAYMENT_FAILED,
                "Payment Failed - " + event.orderNumber(),
                "payment-failed", vars, event.orderNumber());

        notifyAdmin(NotificationType.PAYMENT_FAILED,
                "Payment Failed - " + event.orderNumber(),
                "admin-payment-failed", vars, event.orderNumber());
    }

    @Async
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderNumber", event.orderNumber());
        vars.put("amount", event.amount());

        notifyCustomer(event.userId(), NotificationType.PAYMENT_REFUNDED,
                "Refund Processed - " + event.orderNumber(),
                "payment-refunded", vars, event.orderNumber());
    }

    // ========== Inventory Event Handlers ==========

    private static final int LOW_STOCK_THRESHOLD = 10;

    @Async
    public void handleStockUpdated(StockUpdatedEvent event) {
        if ("LOW_STOCK".equals(event.stockStatus()) || "OUT_OF_STOCK".equals(event.stockStatus())) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("productId", event.productId());
            vars.put("availableQuantity", event.newQuantity());
            vars.put("threshold", LOW_STOCK_THRESHOLD);

            notifyAdmin(NotificationType.LOW_STOCK_ALERT,
                    "Low Stock Alert - Product #" + event.productId(),
                    "admin-low-stock", vars, String.valueOf(event.productId()));
        }
    }

    // ========== Admin Query Methods ==========

    public PagedResponse<NotificationResponse> getAllNotifications(int page, int size) {
        return PagedResponse.from(
                notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)),
                NotificationResponse::from
        );
    }

    public PagedResponse<NotificationResponse> getNotificationsByUser(UUID userId, int page, int size) {
        return PagedResponse.from(
                notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)),
                NotificationResponse::from
        );
    }

    public PagedResponse<NotificationResponse> getFailedNotifications(int page, int size) {
        return PagedResponse.from(
                notificationRepository.findAllByStatusOrderByCreatedAtDesc(
                        NotificationStatus.FAILED, PageRequest.of(page, size)),
                NotificationResponse::from
        );
    }

    public NotificationResponse retryNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException("Only failed notifications can be retried");
        }

        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            throw new IllegalStateException("Maximum retry attempts reached for notification: " + id);
        }

        Map<String, Object> variables = deserializeVariables(notification.getTemplateVariables());
        try {
            emailService.sendHtmlEmail(notification.getRecipientEmail(), notification.getSubject(),
                    notification.getTemplateName(), variables);
            notification.setStatus(NotificationStatus.SENT);
            notification.setErrorMessage(null);
        } catch (Exception e) {
            notification.setErrorMessage(e.getMessage());
        }
        notification.setRetryCount(notification.getRetryCount() + 1);

        return NotificationResponse.from(notificationRepository.save(notification));
    }

    // ========== Core Notification Logic ==========

    private void notifyCustomer(UUID userId, NotificationType type, String subject,
                                String templateName, Map<String, Object> variables, String referenceId) {
        userServiceClient.getUserById(userId).ifPresentOrElse(
                user -> {
                    variables.put("firstName", user.firstName());
                    sendAndLog(user.email(), userId, type, subject,
                            templateName, variables, referenceId);
                },
                () -> log.warn("Could not fetch user {} for notification {}", userId, type)
        );
    }

    private void notifyAdmin(NotificationType type, String subject,
                             String templateName, Map<String, Object> variables, String referenceId) {
        sendAndLog(adminEmail, null, type, subject, templateName, variables, referenceId);
    }

    private void sendAndLog(String email, UUID userId, NotificationType type, String subject,
                            String templateName, Map<String, Object> variables, String referenceId) {
        if (isDuplicate(type, referenceId, email)) {
            log.info("Duplicate notification detected: type={}, ref={}, email={}. Skipping.", type, referenceId, email);
            return;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .recipientEmail(email)
                .type(type)
                .subject(subject)
                .templateName(templateName)
                .templateVariables(serializeVariables(variables))
                .referenceId(referenceId)
                .build();

        try {
            emailService.sendHtmlEmail(email, subject, templateName, variables);
            notification.setStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }

        Notification saved = notificationRepository.save(notification);

        for (NotificationChannel channel : channels) {
            try {
                channel.send(saved);
            } catch (Exception e) {
                log.warn("Channel {} failed for notification {}: {}",
                        channel.getClass().getSimpleName(), saved.getId(), e.getMessage());
            }
        }
    }

    private boolean isDuplicate(NotificationType type, String referenceId, String email) {
        return notificationRepository.existsByTypeAndReferenceIdAndRecipientEmail(
                type, referenceId, email);
    }

    private String serializeVariables(Map<String, Object> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize template variables: {}", e.getMessage());
            return "{}";
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
