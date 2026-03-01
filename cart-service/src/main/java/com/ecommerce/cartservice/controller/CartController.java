package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.request.AddToCartRequest;
import com.ecommerce.cartservice.dto.request.UpdateCartItemRequest;
import com.ecommerce.cartservice.dto.response.CartResponse;
import com.ecommerce.cartservice.dto.response.CartSummaryResponse;
import com.ecommerce.cartservice.dto.response.CartValidationResponse;
import com.ecommerce.cartservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Cart", description = "Shopping cart management endpoints")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get cart", description = "Returns the current user's active cart with all items")
    public ResponseEntity<CartResponse> getCart() {
        UUID userId = extractUserId();
        log.debug("GET /cart for user {}", userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the cart. If the product already exists, increases the quantity")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        UUID userId = extractUserId();
        log.debug("POST /cart/items for user {} - productId={}, quantity={}", userId, request.productId(), request.quantity());
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update item quantity", description = "Updates the quantity of a specific product in the cart")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        UUID userId = extractUserId();
        log.debug("PUT /cart/items/{} for user {} - quantity={}", productId, userId, request.quantity());
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, request));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Removes a specific product from the cart")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long productId) {
        UUID userId = extractUserId();
        log.debug("DELETE /cart/items/{} for user {}", productId, userId);
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Removes all items from the cart")
    public ResponseEntity<Void> clearCart() {
        UUID userId = extractUserId();
        log.debug("DELETE /cart for user {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    @Operation(summary = "Get cart summary", description = "Returns a lightweight summary with total items and total price")
    public ResponseEntity<CartSummaryResponse> getSummary() {
        UUID userId = extractUserId();
        log.debug("GET /cart/summary for user {}", userId);
        return ResponseEntity.ok(cartService.getSummary(userId));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate cart", description = "Checks all cart items for availability, stock, and price changes before checkout")
    public ResponseEntity<CartValidationResponse> validateCart() {
        UUID userId = extractUserId();
        log.debug("POST /cart/validate for user {}", userId);
        return ResponseEntity.ok(cartService.validateCart(userId));
    }

    private UUID extractUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
