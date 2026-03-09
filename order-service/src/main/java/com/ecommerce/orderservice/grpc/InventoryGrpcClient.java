package com.ecommerce.orderservice.grpc;

import com.ecommerce.inventoryservice.grpc.CheckStockRequest;
import com.ecommerce.inventoryservice.grpc.CheckStockResponse;
import com.ecommerce.inventoryservice.grpc.ConfirmStockRequest;
import com.ecommerce.inventoryservice.grpc.ConfirmStockResponse;
import com.ecommerce.inventoryservice.grpc.InventoryGrpcServiceGrpc;
import com.ecommerce.inventoryservice.grpc.ReleaseStockRequest;
import com.ecommerce.inventoryservice.grpc.ReleaseStockResponse;
import com.ecommerce.inventoryservice.grpc.ReserveStockRequest;
import com.ecommerce.inventoryservice.grpc.ReserveStockResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class InventoryGrpcClient {

    private static final long TIMEOUT_SECONDS = 5;

    @GrpcClient("inventory-service")
    private InventoryGrpcServiceGrpc.InventoryGrpcServiceBlockingStub inventoryStub;

    public CheckStockResponse checkStock(Long productId, Integer quantity) {
        log.debug("Calling Inventory Service gRPC CheckStock for productId: {}, quantity: {}", productId, quantity);
        CheckStockRequest request = CheckStockRequest.newBuilder()
                .setProductId(productId)
                .setQuantity(quantity)
                .build();
        return inventoryStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).checkStock(request);
    }

    public ReserveStockResponse reserveStock(String orderId, Long productId, Integer quantity) {
        log.debug("Calling Inventory Service gRPC ReserveStock for orderId: {}, productId: {}, quantity: {}", orderId, productId, quantity);
        ReserveStockRequest request = ReserveStockRequest.newBuilder()
                .setOrderId(orderId)
                .setProductId(productId)
                .setQuantity(quantity)
                .build();
        return inventoryStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).reserveStock(request);
    }

    public ConfirmStockResponse confirmStock(String orderId, Long productId) {
        log.debug("Calling Inventory Service gRPC ConfirmStock for orderId: {}, productId: {}", orderId, productId);
        ConfirmStockRequest request = ConfirmStockRequest.newBuilder()
                .setOrderId(orderId)
                .setProductId(productId)
                .build();
        return inventoryStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).confirmStock(request);
    }

    public ReleaseStockResponse releaseStock(String orderId, Long productId) {
        log.debug("Calling Inventory Service gRPC ReleaseStock for orderId: {}, productId: {}", orderId, productId);
        ReleaseStockRequest request = ReleaseStockRequest.newBuilder()
                .setOrderId(orderId)
                .setProductId(productId)
                .build();
        return inventoryStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).releaseStock(request);
    }
}
