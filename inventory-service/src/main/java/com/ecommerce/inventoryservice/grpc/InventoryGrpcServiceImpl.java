package com.ecommerce.inventoryservice.grpc;

import com.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.inventoryservice.service.StockReservationService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcServiceImpl extends InventoryGrpcServiceGrpc.InventoryGrpcServiceImplBase {

    private final InventoryService inventoryService;
    private final StockReservationService stockReservationService;

    @Override
    public void checkStock(CheckStockRequest request, StreamObserver<CheckStockResponse> responseObserver) {
        log.info("gRPC checkStock request for productId: {}, requested quantity: {}", request.getProductId(), request.getQuantity());

        InventoryResponse inventory = inventoryService.getStockInfo(request.getProductId());
        int availableQuantity = inventory.quantity() - inventory.reservedQuantity();
        boolean isAvailable = availableQuantity >= request.getQuantity();

        CheckStockResponse response = CheckStockResponse.newBuilder()
                .setIsAvailable(isAvailable)
                .setAvailableQuantity(availableQuantity)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void reserveStock(ReserveStockRequest request, StreamObserver<ReserveStockResponse> responseObserver) {
        log.info("gRPC reserveStock request for productId: {}, orderId: {}", request.getProductId(), request.getOrderId());

        stockReservationService.reserveStock(request.getOrderId(), request.getProductId(), request.getQuantity());

        ReserveStockResponse response = ReserveStockResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Stock reserved successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void releaseStock(ReleaseStockRequest request, StreamObserver<ReleaseStockResponse> responseObserver) {
        log.info("gRPC releaseStock request for productId: {}, orderId: {}", request.getProductId(), request.getOrderId());

        stockReservationService.releaseStock(request.getOrderId(), request.getProductId());

        ReleaseStockResponse response = ReleaseStockResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Stock released successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void confirmStock(ConfirmStockRequest request, StreamObserver<ConfirmStockResponse> responseObserver) {
        log.info("gRPC confirmStock request for productId: {}, orderId: {}", request.getProductId(), request.getOrderId());

        stockReservationService.confirmStock(request.getOrderId(), request.getProductId());

        ConfirmStockResponse response = ConfirmStockResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Stock confirmed successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getStockInfo(GetStockInfoRequest request, StreamObserver<StockInfoResponse> responseObserver) {
        log.info("gRPC getStockInfo request for productId: {}", request.getProductId());

        InventoryResponse inventory = inventoryService.getStockInfo(request.getProductId());

        StockInfoResponse response = StockInfoResponse.newBuilder()
                .setProductId(inventory.productId())
                .setQuantity(inventory.quantity())
                .setReservedQuantity(inventory.reservedQuantity())
                .setStockStatus(inventory.stockStatus().name())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}