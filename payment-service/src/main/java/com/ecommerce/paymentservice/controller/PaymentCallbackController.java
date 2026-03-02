package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/payments/callback")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Callbacks", description = "Kapital Bank callback endpoints (public)")
public class PaymentCallbackController {

    private final PaymentService paymentService;

    @Value("${frontend.success-url}")
    private String frontendSuccessUrl;

    @Value("${frontend.cancel-url}")
    private String frontendCancelUrl;

    @Value("${frontend.decline-url}")
    private String frontendDeclineUrl;

    @GetMapping("/approve")
    @Operation(summary = "Approve callback", description = "Called by Kapital Bank when payment is approved")
    public ResponseEntity<Void> approveCallback(
            @RequestParam("ORDERID") String orderId,
            @RequestParam("SESSIONID") String sessionId) {
        log.info("Received approve callback: ORDERID={}, SESSIONID={}", orderId, sessionId);
        String orderNumber = paymentService.handleApproveCallback(orderId, sessionId);
        return redirect(frontendSuccessUrl + "?order=" + orderNumber);
    }

    @GetMapping("/cancel")
    @Operation(summary = "Cancel callback", description = "Called by Kapital Bank when customer cancels payment")
    public ResponseEntity<Void> cancelCallback(
            @RequestParam("ORDERID") String orderId,
            @RequestParam("SESSIONID") String sessionId) {
        log.info("Received cancel callback: ORDERID={}, SESSIONID={}", orderId, sessionId);
        String orderNumber = paymentService.handleCancelCallback(orderId, sessionId);
        return redirect(frontendCancelUrl + "?order=" + orderNumber);
    }

    @GetMapping("/decline")
    @Operation(summary = "Decline callback", description = "Called by Kapital Bank when payment is declined")
    public ResponseEntity<Void> declineCallback(
            @RequestParam("ORDERID") String orderId,
            @RequestParam("SESSIONID") String sessionId) {
        log.info("Received decline callback: ORDERID={}, SESSIONID={}", orderId, sessionId);
        String orderNumber = paymentService.handleDeclineCallback(orderId, sessionId);
        return redirect(frontendDeclineUrl + "?order=" + orderNumber);
    }

    private ResponseEntity<Void> redirect(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
