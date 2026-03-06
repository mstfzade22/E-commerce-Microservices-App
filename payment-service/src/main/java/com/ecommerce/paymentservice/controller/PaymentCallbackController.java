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

    @GetMapping("/result")
    @Operation(summary = "Payment result callback", description = "Called by Kapital Bank when payment flow completes")
    public ResponseEntity<Void> resultCallback(
            @RequestParam("ID") String orderId,
            @RequestParam("STATUS") String status) {
        log.info("Received callback: ID={}, STATUS={}", orderId, status);
        String orderNumber = paymentService.handleCallback(orderId, status);

        String redirectUrl = switch (status) {
            case "FullyPaid" -> frontendSuccessUrl + "?order=" + orderNumber;
            case "Cancelled", "Canceled" -> frontendCancelUrl + "?order=" + orderNumber;
            default -> frontendDeclineUrl + "?order=" + orderNumber;
        };

        return redirect(redirectUrl);
    }

    private ResponseEntity<Void> redirect(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
