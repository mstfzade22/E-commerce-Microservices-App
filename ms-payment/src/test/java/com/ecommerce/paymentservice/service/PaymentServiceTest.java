package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.client.KapitalBankClient;
import com.ecommerce.paymentservice.client.OrderServiceClient;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankOrderResponse;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankRefundResponse;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankStatusResponse;
import com.ecommerce.paymentservice.dto.request.InitiatePaymentRequest;
import com.ecommerce.paymentservice.dto.response.PaymentInitiatedResponse;
import com.ecommerce.paymentservice.dto.response.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.exception.InvalidPaymentStatusException;
import com.ecommerce.paymentservice.exception.KapitalBankException;
import com.ecommerce.paymentservice.exception.OrderNotConfirmedException;
import com.ecommerce.paymentservice.exception.PaymentAlreadyExistsException;
import com.ecommerce.paymentservice.kafka.PaymentEventProducer;
import com.ecommerce.paymentservice.mapper.PaymentMapper;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.repository.PaymentStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentStatusHistoryRepository statusHistoryRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private KapitalBankClient kapitalBankClient;
    @Mock
    private PaymentEventProducer paymentEventProducer;
    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private PaymentService paymentService;

    private final UUID userId = UUID.randomUUID();
    private final String role = "ROLE_CUSTOMER";

    private void setUpCallbackUrl() {
        ReflectionTestUtils.setField(paymentService, "callbackBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(paymentService, "testMode", false);
    }

    @Test
    void initiatePayment_happyPath() {
        setUpCallbackUrl();
        InitiatePaymentRequest request = new InitiatePaymentRequest("ORD-001");
        OrderServiceClient.OrderDetails orderDetails = new OrderServiceClient.OrderDetails("CONFIRMED", BigDecimal.valueOf(100));

        when(orderServiceClient.getOrderDetails("ORD-001", userId, role)).thenReturn(orderDetails);
        when(paymentRepository.existsByOrderNumberAndStatusIn(eq("ORD-001"), anyList())).thenReturn(false);
        when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.empty());

        KapitalBankOrderResponse.OrderData orderData = new KapitalBankOrderResponse.OrderData(
                123L, "http://hpp.bank.az", "secret123", "Preparing", null, null);
        KapitalBankOrderResponse bankResponse = new KapitalBankOrderResponse(orderData);

        when(kapitalBankClient.createOrder(any(BigDecimal.class), anyString(), anyString())).thenReturn(bankResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentInitiatedResponse expected = mock(PaymentInitiatedResponse.class);
        when(paymentMapper.toPaymentInitiatedResponse(any(Payment.class))).thenReturn(expected);

        PaymentInitiatedResponse result = paymentService.initiatePayment(userId, request, role);

        assertThat(result).isEqualTo(expected);
        verify(paymentEventProducer).sendPaymentInitiatedEvent(any());
    }

    @Test
    void initiatePayment_orderNotConfirmed_throwsOrderNotConfirmedException() {
        setUpCallbackUrl();
        InitiatePaymentRequest request = new InitiatePaymentRequest("ORD-001");
        OrderServiceClient.OrderDetails orderDetails = new OrderServiceClient.OrderDetails("PENDING", BigDecimal.valueOf(100));

        when(orderServiceClient.getOrderDetails("ORD-001", userId, role)).thenReturn(orderDetails);

        assertThatThrownBy(() -> paymentService.initiatePayment(userId, request, role))
                .isInstanceOf(OrderNotConfirmedException.class);
    }

    @Test
    void initiatePayment_paymentAlreadyExists_throwsPaymentAlreadyExistsException() {
        setUpCallbackUrl();
        InitiatePaymentRequest request = new InitiatePaymentRequest("ORD-001");
        OrderServiceClient.OrderDetails orderDetails = new OrderServiceClient.OrderDetails("CONFIRMED", BigDecimal.valueOf(100));

        when(orderServiceClient.getOrderDetails("ORD-001", userId, role)).thenReturn(orderDetails);
        when(paymentRepository.existsByOrderNumberAndStatusIn(eq("ORD-001"), anyList())).thenReturn(true);

        assertThatThrownBy(() -> paymentService.initiatePayment(userId, request, role))
                .isInstanceOf(PaymentAlreadyExistsException.class);
    }

    @Test
    void initiatePayment_bankApiFails_throwsKapitalBankException() {
        setUpCallbackUrl();
        InitiatePaymentRequest request = new InitiatePaymentRequest("ORD-001");
        OrderServiceClient.OrderDetails orderDetails = new OrderServiceClient.OrderDetails("CONFIRMED", BigDecimal.valueOf(100));

        when(orderServiceClient.getOrderDetails("ORD-001", userId, role)).thenReturn(orderDetails);
        when(paymentRepository.existsByOrderNumberAndStatusIn(eq("ORD-001"), anyList())).thenReturn(false);
        when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.empty());
        when(kapitalBankClient.createOrder(any(BigDecimal.class), anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> paymentService.initiatePayment(userId, request, role))
                .isInstanceOf(KapitalBankException.class);
    }

    @Test
    void handleCallback_fullyPaid_approvesPayment() {
        ReflectionTestUtils.setField(paymentService, "testMode", true);
        Payment payment = Payment.builder().id(1L).orderNumber("ORD-001").userId(userId)
                .amount(BigDecimal.valueOf(100)).status(PaymentStatus.INITIATED)
                .kapitalOrderId("123").statusHistory(new LinkedHashSet<>()).build();

        when(paymentRepository.findByKapitalOrderId("123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        String orderNumber = paymentService.handleCallback("123", "FullyPaid");

        assertThat(orderNumber).isEqualTo("ORD-001");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(paymentEventProducer).sendPaymentSuccessEvent(any());
    }

    @Test
    void handleCallback_cancelled_cancelsPayment() {
        Payment payment = Payment.builder().id(1L).orderNumber("ORD-001").userId(userId)
                .amount(BigDecimal.valueOf(100)).status(PaymentStatus.INITIATED)
                .kapitalOrderId("123").statusHistory(new LinkedHashSet<>()).build();

        when(paymentRepository.findByKapitalOrderId("123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        String orderNumber = paymentService.handleCallback("123", "Cancelled");

        assertThat(orderNumber).isEqualTo("ORD-001");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentEventProducer).sendPaymentFailedEvent(any());
    }

    @Test
    void handleCallback_declined_declinesPayment() {
        ReflectionTestUtils.setField(paymentService, "testMode", true);
        Payment payment = Payment.builder().id(1L).orderNumber("ORD-001").userId(userId)
                .amount(BigDecimal.valueOf(100)).status(PaymentStatus.INITIATED)
                .kapitalOrderId("123").statusHistory(new LinkedHashSet<>()).build();

        when(paymentRepository.findByKapitalOrderId("123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        String orderNumber = paymentService.handleCallback("123", "Declined");

        assertThat(orderNumber).isEqualTo("ORD-001");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        verify(paymentEventProducer).sendPaymentFailedEvent(any());
    }

    @Test
    void refundPayment_onlyApprovedCanBeRefunded() {
        Payment payment = Payment.builder().id(1L).orderNumber("ORD-001").userId(userId)
                .amount(BigDecimal.valueOf(100)).status(PaymentStatus.INITIATED)
                .kapitalOrderId("123").statusHistory(new LinkedHashSet<>()).build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(1L))
                .isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    void refundPayment_approvedPayment_refunds() {
        Payment payment = Payment.builder().id(1L).orderNumber("ORD-001").userId(userId)
                .amount(BigDecimal.valueOf(100)).status(PaymentStatus.APPROVED)
                .kapitalOrderId("123").statusHistory(new LinkedHashSet<>()).build();

        KapitalBankRefundResponse refundResponse = mock(KapitalBankRefundResponse.class);
        when(refundResponse.isSuccessful()).thenReturn(true);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(kapitalBankClient.refundOrder("123", BigDecimal.valueOf(100))).thenReturn(refundResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse expected = mock(PaymentResponse.class);
        when(paymentMapper.toPaymentResponse(payment)).thenReturn(expected);

        PaymentResponse result = paymentService.refundPayment(1L);

        assertThat(result).isEqualTo(expected);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentEventProducer).sendPaymentRefundedEvent(any());
    }

    @Test
    void handleOrderCancelled_approvedPayment_autoRefunds() {
        Payment payment = Payment.builder().id(1L).orderNumber("ORD-001").userId(userId)
                .amount(BigDecimal.valueOf(100)).status(PaymentStatus.APPROVED)
                .kapitalOrderId("123").statusHistory(new LinkedHashSet<>()).build();

        KapitalBankRefundResponse refundResponse = mock(KapitalBankRefundResponse.class);
        when(refundResponse.isSuccessful()).thenReturn(true);

        when(paymentRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(payment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(kapitalBankClient.refundOrder("123", BigDecimal.valueOf(100))).thenReturn(refundResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        paymentService.handleOrderCancelled("ORD-001", "Customer requested");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
