package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.client.UserServiceClient;
import com.ecommerce.notificationservice.dto.UserInfo;
import com.ecommerce.notificationservice.dto.event.OrderConfirmedEvent;
import com.ecommerce.notificationservice.dto.event.PaymentSuccessEvent;
import com.ecommerce.notificationservice.dto.event.StockUpdatedEvent;
import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.entity.NotificationType;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private NotificationChannel channel;

    private NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();

    private void setUp() {
        notificationService = new NotificationService(
                emailService, userServiceClient, notificationRepository,
                objectMapper, List.of(channel));
        ReflectionTestUtils.setField(notificationService, "adminEmail", "admin@test.com");
    }

    @Test
    void handleOrderConfirmed_sendsToCustomerAndAdmin() {
        setUp();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                "evt-1", "ORDER_CONFIRMED", 1L, "ORD-001", userId, Instant.now(), Instant.now());

        UserInfo user = new UserInfo(userId, "customer@test.com", "John", "Doe");
        when(userServiceClient.getUserById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.existsByTypeAndReferenceIdAndRecipientEmail(
                any(NotificationType.class), anyString(), anyString())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handleOrderConfirmed(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(2)).save(captor.capture());

        List<Notification> saved = captor.getAllValues();
        assertThat(saved).anyMatch(n -> "customer@test.com".equals(n.getRecipientEmail()));
        assertThat(saved).anyMatch(n -> "admin@test.com".equals(n.getRecipientEmail()));
    }

    @Test
    void handlePaymentSuccess_sendsToCustomerOnly() {
        setUp();
        PaymentSuccessEvent event = new PaymentSuccessEvent(
                "evt-1", "PAYMENT_SUCCESS", 1L, "ORD-001", userId,
                BigDecimal.valueOf(100), "KAP-001", Instant.now(), Instant.now());

        UserInfo user = new UserInfo(userId, "customer@test.com", "John", "Doe");
        when(userServiceClient.getUserById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.existsByTypeAndReferenceIdAndRecipientEmail(
                any(NotificationType.class), anyString(), anyString())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handlePaymentSuccess(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void handleStockUpdated_lowStock_sendsAdminAlert() {
        setUp();
        StockUpdatedEvent event = new StockUpdatedEvent(
                "evt-1", "STOCK_UPDATED", 1L, 5, "LOW_STOCK", Instant.now());

        when(notificationRepository.existsByTypeAndReferenceIdAndRecipientEmail(
                any(NotificationType.class), anyString(), anyString())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handleStockUpdated(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientEmail()).isEqualTo("admin@test.com");
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.LOW_STOCK_ALERT);
    }

    @Test
    void handleStockUpdated_availableStock_noNotification() {
        setUp();
        StockUpdatedEvent event = new StockUpdatedEvent(
                "evt-1", "STOCK_UPDATED", 1L, 100, "AVAILABLE", Instant.now());

        notificationService.handleStockUpdated(event);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void duplicateNotification_skipped() {
        setUp();
        PaymentSuccessEvent event = new PaymentSuccessEvent(
                "evt-1", "PAYMENT_SUCCESS", 1L, "ORD-001", userId,
                BigDecimal.valueOf(100), "KAP-001", Instant.now(), Instant.now());

        UserInfo user = new UserInfo(userId, "customer@test.com", "John", "Doe");
        when(userServiceClient.getUserById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.existsByTypeAndReferenceIdAndRecipientEmail(
                NotificationType.PAYMENT_SUCCESS, "ORD-001", "customer@test.com")).thenReturn(true);

        notificationService.handlePaymentSuccess(event);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void emailSendFailure_savedWithFailedStatus() {
        setUp();
        PaymentSuccessEvent event = new PaymentSuccessEvent(
                "evt-1", "PAYMENT_SUCCESS", 1L, "ORD-001", userId,
                BigDecimal.valueOf(100), "KAP-001", Instant.now(), Instant.now());

        UserInfo user = new UserInfo(userId, "customer@test.com", "John", "Doe");
        when(userServiceClient.getUserById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.existsByTypeAndReferenceIdAndRecipientEmail(
                any(NotificationType.class), anyString(), anyString())).thenReturn(false);

        doThrow(new RuntimeException("SMTP error")).when(emailService)
                .sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handlePaymentSuccess(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("SMTP error");
    }

    @Test
    void channelSend_calledAfterSave() {
        setUp();
        PaymentSuccessEvent event = new PaymentSuccessEvent(
                "evt-1", "PAYMENT_SUCCESS", 1L, "ORD-001", userId,
                BigDecimal.valueOf(100), "KAP-001", Instant.now(), Instant.now());

        UserInfo user = new UserInfo(userId, "customer@test.com", "John", "Doe");
        when(userServiceClient.getUserById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.existsByTypeAndReferenceIdAndRecipientEmail(
                any(NotificationType.class), anyString(), anyString())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handlePaymentSuccess(event);

        verify(channel).send(any(Notification.class));
    }
}
