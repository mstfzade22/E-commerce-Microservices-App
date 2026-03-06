package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.client.KapitalBankClient;
import com.ecommerce.paymentservice.client.OrderServiceClient;
import com.ecommerce.paymentservice.dto.event.PaymentFailedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentInitiatedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentRefundedEvent;
import com.ecommerce.paymentservice.dto.event.PaymentSuccessEvent;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankOrderResponse;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankRefundResponse;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankStatusResponse;
import com.ecommerce.paymentservice.dto.request.InitiatePaymentRequest;
import com.ecommerce.paymentservice.dto.response.PagedResponse;
import com.ecommerce.paymentservice.dto.response.PaymentInitiatedResponse;
import com.ecommerce.paymentservice.dto.response.PaymentResponse;
import com.ecommerce.paymentservice.dto.response.PaymentStatusHistoryResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.entity.PaymentStatusHistory;
import com.ecommerce.paymentservice.exception.InvalidPaymentStatusException;
import com.ecommerce.paymentservice.exception.KapitalBankException;
import com.ecommerce.paymentservice.exception.OrderNotConfirmedException;
import com.ecommerce.paymentservice.exception.PaymentAlreadyExistsException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.kafka.PaymentEventProducer;
import com.ecommerce.paymentservice.mapper.PaymentMapper;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.repository.PaymentStatusHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository statusHistoryRepository;
    private final PaymentMapper paymentMapper;
    private final KapitalBankClient kapitalBankClient;
    private final PaymentEventProducer paymentEventProducer;
    private final OrderServiceClient orderServiceClient;

    @Value("${kapitalbank.callback-base-url}")
    private String callbackBaseUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentStatusHistoryRepository statusHistoryRepository,
                          PaymentMapper paymentMapper,
                          KapitalBankClient kapitalBankClient,
                          PaymentEventProducer paymentEventProducer,
                          OrderServiceClient orderServiceClient) {
        this.paymentRepository = paymentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.paymentMapper = paymentMapper;
        this.kapitalBankClient = kapitalBankClient;
        this.paymentEventProducer = paymentEventProducer;
        this.orderServiceClient = orderServiceClient;
    }

    @Transactional
    public PaymentInitiatedResponse initiatePayment(UUID userId, InitiatePaymentRequest request, BigDecimal amount, String accessToken) {
        log.info("Initiating payment for order {} by user {}", request.orderNumber(), userId);

        String orderStatus = orderServiceClient.getOrderStatus(request.orderNumber(), accessToken);
        if (!"CONFIRMED".equals(orderStatus)) {
            throw new OrderNotConfirmedException(
                    "Order " + request.orderNumber() + " is in status " + orderStatus + ". Only CONFIRMED orders can be paid.");
        }

        if (paymentRepository.existsByOrderNumberAndStatusNot(request.orderNumber(), PaymentStatus.ERROR)) {
            throw new PaymentAlreadyExistsException("Payment already exists for order: " + request.orderNumber());
        }

        String description = "Order " + request.orderNumber();
        String redirectUrl = callbackBaseUrl + "/payments/callback/result";

        KapitalBankOrderResponse bankResponse;
        try {
            bankResponse = kapitalBankClient.createOrder(amount, description, redirectUrl);
        } catch (Exception e) {
            log.error("Failed to create order at Kapital Bank for order {}: {}", request.orderNumber(), e.getMessage());
            throw new KapitalBankException("Failed to initiate payment with bank: " + e.getMessage(), e);
        }

        var orderData = bankResponse.order();
        if (!"Preparing".equals(orderData.status())) {
            throw new KapitalBankException("Kapital Bank returned unexpected status: " + orderData.status());
        }

        String fullPaymentUrl = orderData.hppUrl() + "?id=" + orderData.id() + "&password=" + orderData.password();

        Payment payment = Payment.builder()
                .orderNumber(request.orderNumber())
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.INITIATED)
                .kapitalOrderId(String.valueOf(orderData.id()))
                .kapitalPassword(orderData.password())
                .paymentUrl(fullPaymentUrl)
                .description(description)
                .build();

        addStatusHistory(payment, null, PaymentStatus.INITIATED, "Payment initiated");
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment {} created for order {}", savedPayment.getId(), request.orderNumber());

        paymentEventProducer.sendPaymentInitiatedEvent(new PaymentInitiatedEvent(
                UUID.randomUUID().toString(),
                "PAYMENT_INITIATED",
                savedPayment.getId(),
                savedPayment.getOrderNumber(),
                userId,
                amount,
                savedPayment.getCurrency(),
                Instant.now()
        ));

        return paymentMapper.toPaymentInitiatedResponse(savedPayment);
    }

    @Transactional
    public String handleCallback(String orderId, String status) {
        log.info("Processing callback for Kapital order {}, status={}", orderId, status);

        Payment payment = findByKapitalOrderId(orderId);

        if ("FullyPaid".equals(status)) {
            return handleApproveCallback(payment, orderId);
        } else if ("Cancelled".equals(status) || "Canceled".equals(status)) {
            return handleCancelCallback(payment);
        } else if ("Declined".equals(status)) {
            return handleDeclineCallback(payment, orderId);
        } else {
            return handleDeclineCallback(payment, orderId);
        }
    }

    private String handleApproveCallback(Payment payment, String orderId) {
        KapitalBankStatusResponse statusResponse;
        try {
            statusResponse = kapitalBankClient.getOrderStatus(orderId);
        } catch (Exception e) {
            log.error("Failed to verify payment status for Kapital order {}: {}", orderId, e.getMessage());
            updatePaymentStatus(payment, PaymentStatus.ERROR, "Failed to verify payment: " + e.getMessage());
            return payment.getOrderNumber();
        }

        String bankStatus = statusResponse.order().status();
        if ("FullyPaid".equals(bankStatus)) {
            updatePaymentStatus(payment, PaymentStatus.APPROVED, "Payment approved by bank");

            paymentEventProducer.sendPaymentSuccessEvent(new PaymentSuccessEvent(
                    UUID.randomUUID().toString(),
                    "PAYMENT_SUCCESS",
                    payment.getId(),
                    payment.getOrderNumber(),
                    payment.getUserId(),
                    payment.getAmount(),
                    payment.getKapitalOrderId(),
                    Instant.now(),
                    Instant.now()
            ));
        } else {
            updatePaymentStatus(payment, PaymentStatus.ERROR, "Unexpected status from bank: " + bankStatus);
        }

        return payment.getOrderNumber();
    }

    private String handleCancelCallback(Payment payment) {
        updatePaymentStatus(payment, PaymentStatus.CANCELLED, "Customer cancelled on bank page");

        paymentEventProducer.sendPaymentFailedEvent(new PaymentFailedEvent(
                UUID.randomUUID().toString(),
                "PAYMENT_FAILED",
                payment.getId(),
                payment.getOrderNumber(),
                payment.getUserId(),
                "Customer cancelled",
                Instant.now(),
                Instant.now()
        ));

        return payment.getOrderNumber();
    }

    private String handleDeclineCallback(Payment payment, String orderId) {
        String reason = "Payment declined";
        try {
            KapitalBankStatusResponse statusResponse = kapitalBankClient.getOrderStatus(orderId);
            reason = "Payment declined: " + statusResponse.order().status();
        } catch (Exception e) {
            log.warn("Failed to get decline details for Kapital order {}: {}", orderId, e.getMessage());
        }

        updatePaymentStatus(payment, PaymentStatus.DECLINED, reason);

        paymentEventProducer.sendPaymentFailedEvent(new PaymentFailedEvent(
                UUID.randomUUID().toString(),
                "PAYMENT_FAILED",
                payment.getId(),
                payment.getOrderNumber(),
                payment.getUserId(),
                reason,
                Instant.now(),
                Instant.now()
        ));

        return payment.getOrderNumber();
    }

    @Transactional
    public PaymentResponse refundPayment(Long paymentId) {
        log.info("Initiating refund for payment {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new InvalidPaymentStatusException(
                    "Cannot refund payment in status " + payment.getStatus() + ". Only APPROVED payments can be refunded.");
        }

        KapitalBankRefundResponse refundResponse;
        try {
            refundResponse = kapitalBankClient.refundOrder(
                    payment.getKapitalOrderId(),
                    payment.getAmount()
            );
        } catch (Exception e) {
            log.error("Failed to refund payment {} at Kapital Bank: {}", paymentId, e.getMessage());
            throw new KapitalBankException("Failed to process refund with bank: " + e.getMessage(), e);
        }

        if (!refundResponse.isSuccessful()) {
            String errorDesc = refundResponse.errorDescription() != null
                    ? refundResponse.errorDescription()
                    : "Unknown refund error";
            throw new KapitalBankException("Kapital Bank refund failed: " + errorDesc);
        }

        updatePaymentStatus(payment, PaymentStatus.REFUNDED, "Payment refunded");

        paymentEventProducer.sendPaymentRefundedEvent(new PaymentRefundedEvent(
                UUID.randomUUID().toString(),
                "PAYMENT_REFUNDED",
                payment.getId(),
                payment.getOrderNumber(),
                payment.getUserId(),
                payment.getAmount(),
                Instant.now(),
                Instant.now()
        ));

        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional
    public void handleOrderCancelled(String orderNumber, String reason) {
        log.info("Handling order cancellation for order {}", orderNumber);

        paymentRepository.findByOrderNumber(orderNumber).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.APPROVED) {
                log.info("Auto-initiating refund for approved payment on cancelled order {}", orderNumber);
                try {
                    refundPayment(payment.getId());
                } catch (Exception e) {
                    log.error("Failed to auto-refund payment {} for cancelled order {}: {}",
                            payment.getId(), orderNumber, e.getMessage());
                    updatePaymentStatus(payment, PaymentStatus.ERROR, "Auto-refund failed: " + e.getMessage());
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId, UUID userId, String role) {
        Payment payment;
        if ("ROLE_ADMIN".equals(role)) {
            payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        } else {
            payment = paymentRepository.findById(paymentId)
                    .filter(p -> p.getUserId().equals(userId))
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        }
        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderNumber(String orderNumber, UUID userId, String role) {
        Payment payment;
        if ("ROLE_ADMIN".equals(role)) {
            payment = paymentRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderNumber));
        } else {
            payment = paymentRepository.findByOrderNumber(orderNumber)
                    .filter(p -> p.getUserId().equals(userId))
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderNumber));
        }
        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getMyPayments(UUID userId, int page, int size) {
        Page<Payment> payments = paymentRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return PagedResponse.from(payments, paymentMapper::toPaymentResponse);
    }

    @Transactional(readOnly = true)
    public List<PaymentStatusHistoryResponse> getPaymentHistory(Long paymentId) {
        return statusHistoryRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).stream()
                .map(paymentMapper::toPaymentStatusHistoryResponse)
                .toList();
    }

    private Payment findByKapitalOrderId(String kapitalOrderId) {
        return paymentRepository.findByKapitalOrderId(kapitalOrderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for Kapital order: " + kapitalOrderId));
    }

    private void updatePaymentStatus(Payment payment, PaymentStatus newStatus, String reason) {
        addStatusHistory(payment, payment.getStatus(), newStatus, reason);
        payment.setStatus(newStatus);
        if (newStatus == PaymentStatus.ERROR || newStatus == PaymentStatus.DECLINED) {
            payment.setErrorMessage(reason);
        }
        paymentRepository.save(payment);
    }

    private void addStatusHistory(Payment payment, PaymentStatus previousStatus, PaymentStatus newStatus, String reason) {
        PaymentStatusHistory history = PaymentStatusHistory.builder()
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();
        payment.addStatusHistory(history);
    }
}
