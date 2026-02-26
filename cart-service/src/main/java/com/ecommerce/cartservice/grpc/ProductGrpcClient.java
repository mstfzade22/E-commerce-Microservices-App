package com.ecommerce.cartservice.grpc;

import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.productservice.grpc.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProductGrpcClient {

    @GrpcClient("product-service")
    private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productStub;

    /**
     * Fetches a single product by ID.
     */
    public ProductResponse getProduct(Long productId) {
        log.debug("gRPC call: GetProduct({})", productId);
        try {
            return productStub.getProduct(
                    GetProductRequest.newBuilder().setProductId(productId).build()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting product {}: {}", productId, e.getStatus());
            throw new ProductNotAvailableException("Product not available or error occurred: " + productId);
        }
    }

    /**
     * Bulk fetches products by a list of IDs.
     */
    public List<ProductResponse> getProductsByIds(List<Long> productIds) {
        log.debug("gRPC call: GetProductsByIds({})", productIds);
        try {
            ProductListResponse response = productStub.getProductsByIds(
                    GetProductsByIdsRequest.newBuilder().addAllProductIds(productIds).build()
            );
            return response.getProductsList();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting bulk products: {}", e.getStatus());
            throw new ProductNotAvailableException("Failed to fetch products for cart validation");
        }
    }

    /**
     * Quickly checks if a product exists.
     */
    public boolean checkProductExists(Long productId) {
        log.debug("gRPC call: CheckProductExists({})", productId);
        try {
            CheckProductExistsResponse response = productStub.checkProductExists(
                    CheckProductExistsRequest.newBuilder().setProductId(productId).build()
            );
            return response.getExists();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking product existence {}: {}", productId, e.getStatus());
            return false;
        }
    }
}