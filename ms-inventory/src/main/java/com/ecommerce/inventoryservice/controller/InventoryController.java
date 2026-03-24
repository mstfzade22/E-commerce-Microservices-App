package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.request.StockReservationRequest;
import com.ecommerce.inventoryservice.dto.request.StockUpdateRequest;
import com.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.ecommerce.inventoryservice.dto.response.StockStatusResponse;
import com.ecommerce.inventoryservice.entity.StockStatus;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.inventoryservice.service.StockReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "APIs for managing product stock levels and reservations")
public class InventoryController {

    private final InventoryService inventoryService;
    private final StockReservationService stockReservationService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get stock info", description = "Returns full stock details for a product")
    public ResponseEntity<InventoryResponse> getStockInfo(@PathVariable Long productId) {
        log.info("REST request to get stock info for product: {}", productId);
        return ResponseEntity.ok(inventoryService.getStockInfo(productId));
    }

    @GetMapping("/{productId}/status")
    @Operation(summary = "Get stock status", description = "Returns simplified availability status")
    public ResponseEntity<StockStatusResponse> getStockStatus(@PathVariable Long productId) {
        log.info("REST request to get stock status for product: {}", productId);
        InventoryResponse info = inventoryService.getStockInfo(productId);
        return ResponseEntity.ok(new StockStatusResponse(
                info.productId(),
                info.stockStatus(),
                info.quantity() - info.reservedQuantity()
        ));
    }

    @PutMapping("/{productId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update stock levels", description = "Manually set stock quantity or low stock threshold (Admin only)")
    public ResponseEntity<InventoryResponse> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockUpdateRequest request) {
        log.info("REST request to update stock for product: {}", productId);
        return ResponseEntity.ok(inventoryService.updateStock(productId, request));
    }

    @PostMapping("/{productId}/reserve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE')")
    @Operation(summary = "Reserve stock", description = "Creates a stock reservation for an order (for testing, normally called via gRPC)")
    public ResponseEntity<Void> reserveStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockReservationRequest request) {
        log.info("REST request to reserve {} items of product {} for order {}",
                request.quantity(), productId, request.orderId());
        stockReservationService.reserveStock(request.orderId(), productId, request.quantity());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{productId}/release")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Release reservation", description = "Releases a stock reservation (Admin only)")
    public ResponseEntity<Void> releaseStock(
            @PathVariable Long productId,
            @RequestParam String orderId) {
        log.info("REST request to release stock for product: {}, order: {}", productId, orderId);
        stockReservationService.releaseStock(orderId, productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get low stock products", description = "Returns all products with LOW_STOCK or OUT_OF_STOCK status (Admin only)")
    public ResponseEntity<List<InventoryResponse>> getLowStockProducts() {
        log.info("REST request to get low stock products");
        return ResponseEntity.ok(inventoryService.getLowStockProducts());
    }
}
