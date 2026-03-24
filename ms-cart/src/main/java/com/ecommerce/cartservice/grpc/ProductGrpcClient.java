package com.ecommerce.cartservice.grpc;

import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.productservice.grpc.*;
import io.grpc.StatusRuntimeException;
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
        log.debug("gRPC call: GetProduct({})", productId);
        try {
            return productStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).getProduct(
                    GetProductRequest.newBuilder().setProductId(productId).build()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting product {}: {}", productId, e.getStatus());
            throw new ProductNotAvailableException("Product not available or error occurred: " + productId);
        }
    }

    public List<ProductResponse> getProductsByIds(List<Long> productIds) {
        log.debug("gRPC call: GetProductsByIds({})", productIds);
        try {
            ProductListResponse response = productStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).getProductsByIds(
                    GetProductsByIdsRequest.newBuilder().addAllProductIds(productIds).build()
            );
            return response.getProductsList();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting bulk products: {}", e.getStatus());
            throw new ProductNotAvailableException("Failed to fetch products for cart validation");
        }
    }

    public boolean checkProductExists(Long productId) {
        log.debug("gRPC call: CheckProductExists({})", productId);
        try {
            CheckProductExistsResponse response = productStub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).checkProductExists(
                    CheckProductExistsRequest.newBuilder().setProductId(productId).build()
            );
            return response.getExists();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking product existence {}: {}", productId, e.getStatus());
            return false;
        }
    }
}
