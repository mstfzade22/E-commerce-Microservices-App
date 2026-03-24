package com.ecommerce.productservice.grpc;

import com.ecommerce.productservice.dto.response.PagedResponse;
import com.ecommerce.productservice.dto.response.ProductDetailResponse;
import com.ecommerce.productservice.dto.response.ProductSummaryResponse;
import com.ecommerce.productservice.service.ProductService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class ProductGrpcServiceImpl extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase {

    private final ProductService productService;

    @Override
    public void getProduct(GetProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        log.info("gRPC: GetProduct called for productId: {}", request.getProductId());

        ProductDetailResponse product = productService.getProductById(request.getProductId());

        ProductResponse response = mapToProductResponse(product);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getProductsByIds(GetProductsByIdsRequest request, StreamObserver<ProductListResponse> responseObserver) {
        log.info("gRPC: GetProductsByIds called for {} products", request.getProductIdsList().size());

        List<ProductDetailResponse> products = productService.getProductsByIds(request.getProductIdsList());

        ProductListResponse response = ProductListResponse.newBuilder()
                .addAllProducts(products.stream().map(this::mapToProductResponse).toList())
                .setTotalCount(products.size())
                .setPage(0)
                .setSize(products.size())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void checkProductExists(CheckProductExistsRequest request, StreamObserver<CheckProductExistsResponse> responseObserver) {
        log.info("gRPC: CheckProductExists called for productId: {}", request.getProductId());

        boolean exists = productService.checkProductExists(request.getProductId());

        CheckProductExistsResponse response = CheckProductExistsResponse.newBuilder()
                .setExists(exists)
                .setProductId(request.getProductId())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getProductStockStatus(GetProductStockStatusRequest request, StreamObserver<ProductStockStatusResponse> responseObserver) {
        log.info("gRPC: GetProductStockStatus called for productId: {}", request.getProductId());

        ProductDetailResponse product = productService.getProductById(request.getProductId());

        ProductStockStatusResponse response = ProductStockStatusResponse.newBuilder()
                .setProductId(product.id())
                .setStockStatus(mapToGrpcStockStatus(product.stockStatus()))
                .setIsActive(product.isActive() != null && product.isActive())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getProductsByCategory(GetProductsByCategoryRequest request, StreamObserver<ProductListResponse> responseObserver) {
        log.info("gRPC: GetProductsByCategory called for categoryId: {}, page: {}, size: {}",
                request.getCategoryId(), request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        PagedResponse<ProductSummaryResponse> pagedProducts = productService.getProductsByCategory(
                request.getCategoryId(), pageable);

        ProductListResponse response = ProductListResponse.newBuilder()
                .addAllProducts(pagedProducts.content().stream()
                        .map(this::mapSummaryToProductResponse)
                        .toList())
                .setTotalCount((int) pagedProducts.totalElements())
                .setPage(pagedProducts.page())
                .setSize(pagedProducts.size())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    private ProductResponse mapToProductResponse(ProductDetailResponse product) {
        ProductResponse.Builder builder = ProductResponse.newBuilder()
                .setId(product.id())
                .setName(product.name())
                .setSlug(product.slug())
                .setStockStatus(mapToGrpcStockStatus(product.stockStatus()))
                .setIsActive(product.isActive() != null && product.isActive())
                .setIsFeatured(product.isFeatured() != null && product.isFeatured());

        if (product.description() != null) {
            builder.setDescription(product.description());
        }
        if (product.shortDescription() != null) {
            builder.setShortDescription(product.shortDescription());
        }
        if (product.price() != null) {
            builder.setPrice(product.price().toString());
        }
        if (product.discountPrice() != null) {
            builder.setDiscountPrice(product.discountPrice().toString());
        }
        if (product.sku() != null) {
            builder.setSku(product.sku());
        }
        if (product.createdAt() != null) {
            builder.setCreatedAt(product.createdAt().toString());
        }
        if (product.updatedAt() != null) {
            builder.setUpdatedAt(product.updatedAt().toString());
        }

        if (product.category() != null) {
            builder.setCategory(CategoryInfo.newBuilder()
                    .setId(product.category().id())
                    .setName(product.category().name())
                    .setSlug(product.category().slug())
                    .build());
        }

        if (product.images() != null) {
            product.images().forEach(img -> builder.addImages(
                    ProductImageInfo.newBuilder()
                            .setId(img.id())
                            .setImageUrl(img.imageUrl() != null ? img.imageUrl() : "")
                            .setAltText(img.altText() != null ? img.altText() : "")
                            .setIsPrimary(img.isPrimary())
                            .setDisplayOrder(img.displayOrder())
                            .build()
            ));
        }

        return builder.build();
    }

    private ProductResponse mapSummaryToProductResponse(ProductSummaryResponse product) {
        ProductResponse.Builder builder = ProductResponse.newBuilder()
                .setId(product.id())
                .setName(product.name())
                .setSlug(product.slug())
                .setStockStatus(mapToGrpcStockStatus(product.stockStatus()))
                .setIsActive(product.isActive() != null && product.isActive())
                .setIsFeatured(product.isFeatured() != null && product.isFeatured());

        if (product.shortDescription() != null) {
            builder.setShortDescription(product.shortDescription());
        }
        if (product.price() != null) {
            builder.setPrice(product.price().toString());
        }
        if (product.discountPrice() != null) {
            builder.setDiscountPrice(product.discountPrice().toString());
        }
        if (product.primaryImageUrl() != null) {
            builder.addImages(ProductImageInfo.newBuilder()
                    .setImageUrl(product.primaryImageUrl())
                    .setIsPrimary(true)
                    .build());
        }

        return builder.build();
    }

    private StockStatus mapToGrpcStockStatus(com.ecommerce.productservice.entity.StockStatus stockStatus) {
        if (stockStatus == null) {
            return StockStatus.STOCK_STATUS_UNSPECIFIED;
        }
        return switch (stockStatus) {
            case AVAILABLE -> StockStatus.AVAILABLE;
            case LOW_STOCK -> StockStatus.LOW_STOCK;
            case OUT_OF_STOCK -> StockStatus.OUT_OF_STOCK;
        };
    }

}
