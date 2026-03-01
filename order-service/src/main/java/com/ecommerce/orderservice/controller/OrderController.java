package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.request.CancelOrderRequest;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.response.*;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Create order", description = "Creates an order from the current user's cart")
    public ResponseEntity<OrderCreateResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = extractUserId();
        String accessToken = extractToken(httpRequest);
        log.debug("POST /orders for user {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(userId, request, accessToken));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Get my orders", description = "Returns the current user's orders (paginated)")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = extractUserId();
        log.debug("GET /orders for user {}, page={}, size={}", userId, page, size);
        return ResponseEntity.ok(orderService.getOrdersByUser(userId, page, size));
    }

    @GetMapping("/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Get order details", description = "Returns full order details by order number")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {
        UUID userId = extractUserId();
        String role = extractRole();
        log.debug("GET /orders/{} for user {}", orderNumber, userId);
        return ResponseEntity.ok(orderService.getOrder(orderNumber, userId, role));
    }

    @PostMapping("/{orderNumber}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE')")
    @Operation(summary = "Confirm order", description = "Confirms an order (simulates payment success)")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable String orderNumber) {
        UUID userId = extractUserId();
        log.debug("POST /orders/{}/confirm by user {}", orderNumber, userId);
        return ResponseEntity.ok(orderService.confirmOrder(orderNumber, userId));
    }

    @PostMapping("/{orderNumber}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Cancel order", description = "Cancels an order with a reason")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody CancelOrderRequest request) {
        UUID userId = extractUserId();
        String role = extractRole();
        log.debug("POST /orders/{}/cancel by user {}", orderNumber, userId);
        return ResponseEntity.ok(orderService.cancelOrder(orderNumber, userId, request, role));
    }

    @PostMapping("/{orderNumber}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE')")
    @Operation(summary = "Process order", description = "Marks an order as being prepared/packed")
    public ResponseEntity<OrderResponse> processOrder(@PathVariable String orderNumber) {
        UUID userId = extractUserId();
        log.debug("POST /orders/{}/process by user {}", orderNumber, userId);
        return ResponseEntity.ok(orderService.processOrder(orderNumber, userId));
    }

    @PostMapping("/{orderNumber}/ship")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE')")
    @Operation(summary = "Ship order", description = "Marks an order as shipped")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable String orderNumber) {
        UUID userId = extractUserId();
        log.debug("POST /orders/{}/ship by user {}", orderNumber, userId);
        return ResponseEntity.ok(orderService.shipOrder(orderNumber, userId));
    }

    @PostMapping("/{orderNumber}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE')")
    @Operation(summary = "Deliver order", description = "Marks an order as delivered")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable String orderNumber) {
        UUID userId = extractUserId();
        log.debug("POST /orders/{}/deliver by user {}", orderNumber, userId);
        return ResponseEntity.ok(orderService.deliverOrder(orderNumber, userId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by status", description = "Returns all orders by status (admin only, paginated)")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("GET /orders/status/{}, page={}, size={}", status, page, size);
        return ResponseEntity.ok(orderService.getOrdersByStatus(status, page, size));
    }

    @GetMapping("/{orderNumber}/history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE', 'ADMIN')")
    @Operation(summary = "Get order history", description = "Returns the status change history for an order")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getOrderHistory(@PathVariable String orderNumber) {
        UUID userId = extractUserId();
        String role = extractRole();
        log.debug("GET /orders/{}/history for user {}", orderNumber, userId);
        return ResponseEntity.ok(orderService.getOrderHistory(orderNumber, userId, role));
    }

    private UUID extractUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String extractRole() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority();
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "access_token".equals(cookie.getName()))
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
}
