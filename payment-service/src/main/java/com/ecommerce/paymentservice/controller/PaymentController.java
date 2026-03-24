package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.request.InitiatePaymentRequest;
import com.ecommerce.paymentservice.dto.response.PagedResponse;
import com.ecommerce.paymentservice.dto.response.PaymentInitiatedResponse;
import com.ecommerce.paymentservice.dto.response.PaymentResponse;
import com.ecommerce.paymentservice.dto.response.PaymentStatusHistoryResponse;
import com.ecommerce.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Initiate payment", description = "Initiates a payment for an order (order must be CONFIRMED)")
    public ResponseEntity<PaymentInitiatedResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        UUID userId = extractUserId();
        String role = extractRole();
        log.debug("POST /payments/initiate for order {} by user {}", request.orderNumber(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(userId, request, role));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Get payment details", description = "Returns payment details by payment ID")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        UUID userId = extractUserId();
        String role = extractRole();
        log.debug("GET /payments/{} by user {}", paymentId, userId);
        return ResponseEntity.ok(paymentService.getPayment(paymentId, userId, role));
    }

    @GetMapping("/order/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Get payment by order", description = "Returns payment details by order number")
    public ResponseEntity<PaymentResponse> getPaymentByOrderNumber(@PathVariable String orderNumber) {
        UUID userId = extractUserId();
        String role = extractRole();
        log.debug("GET /payments/order/{} by user {}", orderNumber, userId);
        return ResponseEntity.ok(paymentService.getPaymentByOrderNumber(orderNumber, userId, role));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Get my payments", description = "Returns the current user's payments (paginated)")
    public ResponseEntity<PagedResponse<PaymentResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = extractUserId();
        log.debug("GET /payments/my for user {}, page={}, size={}", userId, page, size);
        return ResponseEntity.ok(paymentService.getMyPayments(userId, page, size));
    }

    @GetMapping("/{paymentId}/history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Get payment history", description = "Returns the status change history for a payment")
    public ResponseEntity<List<PaymentStatusHistoryResponse>> getPaymentHistory(@PathVariable Long paymentId) {
        log.debug("GET /payments/{}/history", paymentId);
        return ResponseEntity.ok(paymentService.getPaymentHistory(paymentId));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund payment", description = "Initiates a refund for an approved payment (admin only)")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long paymentId) {
        log.debug("POST /payments/{}/refund", paymentId);
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }

    private UUID extractUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String extractRole() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority();
    }

}
