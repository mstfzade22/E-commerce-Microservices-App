package com.ecommerce.cartservice.grpc;

import com.ecommerce.inventoryservice.grpc.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InventoryGrpcClient {

    @GrpcClient("inventory-service")
    private InventoryGrpcServiceGrpc.InventoryGrpcServiceBlockingStub inventoryStub;

    public CheckStockResponse checkStock(Long productId, int quantity) {
        log.debug("gRPC call: CheckStock(productId={}, quantity={})", productId, quantity);
        try {
            return inventoryStub.checkStock(
                    CheckStockRequest.newBuilder()
                            .setProductId(productId)
                            .setQuantity(quantity)
                            .build()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking stock for product {}: {}", productId, e.getStatus());
            return CheckStockResponse.newBuilder()
                    .setIsAvailable(false)
                    .setAvailableQuantity(0)
                    .build();
        }
    }

    public StockInfoResponse getStockInfo(Long productId) {
        log.debug("gRPC call: GetStockInfo({})", productId);
        try {
            return inventoryStub.getStockInfo(
                    GetStockInfoRequest.newBuilder().setProductId(productId).build()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting stock info for product {}: {}", productId, e.getStatus());
            throw new RuntimeException("Inventory service unavailable for product: " + productId);
        }
    }
}