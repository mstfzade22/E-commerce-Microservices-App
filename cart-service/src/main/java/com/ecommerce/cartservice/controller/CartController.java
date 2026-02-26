package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.request.AddToCartRequest;
import com.ecommerce.cartservice.dto.request.UpdateCartItemRequest;
import com.ecommerce.cartservice.dto.response.CartResponse;
import com.ecommerce.cartservice.dto.response.CartSummaryResponse;
import com.ecommerce.cartservice.dto.response.CartValidationResponse;
import com.ecommerce.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        UUID userId = extractUserId();
        log.debug("GET /cart for user {}", userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        UUID userId = extractUserId();
        log.debug("POST /cart/items for user {} - productId={}, quantity={}", userId, request.productId(), request.quantity());
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        UUID userId = extractUserId();
        log.debug("PUT /cart/items/{} for user {} - quantity={}", productId, userId, request.quantity());
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, request));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long productId) {
        UUID userId = extractUserId();
        log.debug("DELETE /cart/items/{} for user {}", productId, userId);
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        UUID userId = extractUserId();
        log.debug("DELETE /cart for user {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<CartSummaryResponse> getSummary() {
        UUID userId = extractUserId();
        log.debug("GET /cart/summary for user {}", userId);
        return ResponseEntity.ok(cartService.getSummary(userId));
    }

    @PostMapping("/validate")
    public ResponseEntity<CartValidationResponse> validateCart() {
        UUID userId = extractUserId();
        log.debug("POST /cart/validate for user {}", userId);
        return ResponseEntity.ok(cartService.validateCart(userId));
    }

    private UUID extractUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
