package com.ecommerce.orderservice.grpc;

import com.ecommerce.productservice.grpc.GetProductRequest;
import com.ecommerce.productservice.grpc.GetProductsByIdsRequest;
import com.ecommerce.productservice.grpc.ProductGrpcServiceGrpc;
import com.ecommerce.productservice.grpc.ProductListResponse;
import com.ecommerce.productservice.grpc.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProductGrpcClient {

    private static final long TIMEOUT_SECONDS = 5;

    @GrpcClient("product-service")
    private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productStub;

    public ProductResponse getProduct(Long productId) {
        log.debug("Calling Product Service gRPC GetProduct for productId: {}", productId);
        GetProductRequest request = GetProductRequest.newBuilder()
                .setProductId(productId)
                .build();
        return productStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).getProduct(request);
    }

    public ProductListResponse getProductsByIds(List<Long> productIds) {
        log.debug("Calling Product Service gRPC GetProductsByIds for productIds: {}", productIds);
        GetProductsByIdsRequest request = GetProductsByIdsRequest.newBuilder()
                .addAllProductIds(productIds)
                .build();
        return productStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).getProductsByIds(request);
    }
}
