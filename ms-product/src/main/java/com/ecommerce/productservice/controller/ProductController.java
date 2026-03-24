package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.request.CreateProductRequest;
import com.ecommerce.productservice.dto.request.ProductFilterRequest;
import com.ecommerce.productservice.dto.request.UpdateProductRequest;
import com.ecommerce.productservice.dto.response.PagedResponse;
import com.ecommerce.productservice.dto.response.ProductCreateResponse;
import com.ecommerce.productservice.dto.response.ProductDetailResponse;
import com.ecommerce.productservice.dto.response.ProductSummaryResponse;
import com.ecommerce.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "APIs for managing products in the e-commerce platform")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Filter products", description = "Retrieves a paginated list of products with dynamic filtering, sorting, and search")
    public ResponseEntity<PagedResponse<ProductSummaryResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) Map<String, String> attributes,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Map<String, String> filteredAttributes = null;
        if (attributes != null) {
            filteredAttributes = attributes.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("attr."))
                    .collect(java.util.stream.Collectors.toMap(
                            e -> e.getKey().substring(5),
                            Map.Entry::getValue));
            if (filteredAttributes.isEmpty()) filteredAttributes = null;
        }

        ProductFilterRequest filter = new ProductFilterRequest(
                keyword, categoryId, featured, minPrice, maxPrice,
                stockStatus, filteredAttributes, sortBy, sortDir);

        return ResponseEntity.ok(productService.filterProducts(filter, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a product by its ID with detailed information")
    public ResponseEntity<ProductDetailResponse> getProductById(@PathVariable Long id) {
        log.info("Request to get a product with ID: {}", id);

        ProductDetailResponse product = productService.getProductById(id);

        return ResponseEntity.ok(product);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug", description = "Retrieves a product by its slug with detailed information")
    public ResponseEntity<ProductDetailResponse> getProductBySlug(@PathVariable String slug) {
        log.info("Request to get a product with Slug: {}", slug);

        ProductDetailResponse product = productService.getProductBySlug(slug);

        return ResponseEntity.ok(product);
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products", description = "Retrieves featured products with summary information")
    public ResponseEntity<List<ProductSummaryResponse>> getFeaturedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Request to get featured Products. Limit: {}", limit);

        List<ProductSummaryResponse> products = productService.getFeaturedProducts(limit);

        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by keyword with pagination")
    public ResponseEntity<PagedResponse<ProductSummaryResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to search products with keyword: '{}', Page: {}, Size: {}", keyword, page, size);

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<ProductSummaryResponse> products = productService.searchProducts(keyword, pageable);

        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category", description = "Retrieves paginated products by category ID")
    public ResponseEntity<PagedResponse<ProductSummaryResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to get products for category ID: {}, Page: {}, Size: {}", categoryId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<ProductSummaryResponse> products = productService.getProductsByCategory(categoryId, pageable);

        return ResponseEntity.ok(products);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STORE')")
    @Operation(summary = "Create product", description = "Creates a new product (Admin/Store only)")
    public ResponseEntity<ProductCreateResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("Request to create product with slug: {}", request.slug());

        ProductCreateResponse response = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STORE')")
    @Operation(summary = "Update product", description = "Updates an existing product (Admin/Store only)")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("Request to update product with ID: {}", id);

        ProductDetailResponse response = productService.updateProduct(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STORE')")
    @Operation(summary = "Delete product", description = "Soft deletes a product (Admin/Store only)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Request to delete product with ID: {}", id);

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }

}
