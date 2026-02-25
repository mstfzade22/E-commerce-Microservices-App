package com.ecommerce.productservice.service;

import com.ecommerce.productservice.config.RedisConfig;
import com.ecommerce.productservice.dto.request.CreateProductRequest;
import com.ecommerce.productservice.dto.request.UpdateProductRequest;
import com.ecommerce.productservice.dto.response.PagedResponse;
import com.ecommerce.productservice.dto.response.ProductCreateResponse;
import com.ecommerce.productservice.dto.response.ProductDetailResponse;
import com.ecommerce.productservice.dto.response.ProductSummaryResponse;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.StockStatus;
import com.ecommerce.productservice.exception.DuplicateResourceException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.repositories.CategoryRepository;
import com.ecommerce.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public ProductCreateResponse createProduct(CreateProductRequest request) {
        log.info("Creating product with slug: {}", request.slug());

        if (productRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Product", "slug", request.slug());
        }

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product", "sku", request.sku());
        }

        Product product = productMapper.toEntity(request);

        if (request.initialStock() != null && request.initialStock() > 0) {
            product.setStockStatus(StockStatus.fromQuantity(request.initialStock()));
        } else {
            product.setStockStatus(StockStatus.OUT_OF_STOCK);
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            product.setCategory(category);
        }

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());

        kafkaProducerService.publishProductCreatedEvent(savedProduct, request.initialStock());

        return productMapper.toCreateResponse(savedProduct);
    }

    @Transactional
    @CachePut(value = RedisConfig.CacheNames.PRODUCT_BY_ID, key = "#id")
    @CacheEvict(value = RedisConfig.CacheNames.PRODUCT_BY_SLUG, allEntries = true)
    public ProductDetailResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        BigDecimal oldPrice = product.getPrice();
        BigDecimal oldDiscountPrice = product.getDiscountPrice();

        if (request.getSlug() != null && !request.getSlug().equals(product.getSlug())) {
            if (productRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
                log.warn("Update failed - Slug already exists: {}", request.getSlug());
                throw new DuplicateResourceException("Product", "slug", request.getSlug());
            }
        }

        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
                log.warn("Update failed - SKU already exists: {}", request.getSku());
                throw new DuplicateResourceException("Product", "sku", request.getSku());
            }
        }

        if (request.getCategoryId() != null &&
                (product.getCategory() == null || !request.getCategoryId().equals(product.getCategory().getId()))) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        }

        productMapper.updateEntityFromRequest(request, product);

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", updatedProduct.getId());

        kafkaProducerService.publishProductUpdatedEvent(updatedProduct);

        boolean priceChanged = (request.getPrice() != null && !request.getPrice().equals(oldPrice)) ||
                (request.getDiscountPrice() != null && !request.getDiscountPrice().equals(oldDiscountPrice));

        if (priceChanged) {
            kafkaProducerService.publishPriceChangedEvent(updatedProduct, oldPrice, oldDiscountPrice);
        }

        return productMapper.toDetailResponse(updatedProduct);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.PRODUCT_BY_ID, key = "#id")
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        String slug = product.getSlug();
        String sku = product.getSku();

        product.setIsActive(false);
        productRepository.save(product);

        log.info("Product soft-deleted successfully with ID: {}", id);

        kafkaProducerService.publishProductDeletedEvent(id, slug, sku);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CacheNames.PRODUCT_BY_ID, key = "#id")
    public ProductDetailResponse getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        return productMapper.toDetailResponse(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CacheNames.PRODUCT_BY_SLUG, key = "#slug")
    public ProductDetailResponse getProductBySlug(String slug) {
        log.info("Fetching product with slug: {}", slug);

        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));

        return productMapper.toDetailResponse(product);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductSummaryResponse> getAllProducts(Pageable pageable) {
        log.info("Fetching all active products, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> productPage = productRepository.findByIsActiveTrue(pageable);

        return PagedResponse.from(productPage, productMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductSummaryResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.info("Fetching products for category ID: {}, page: {}, size: {}", categoryId, pageable.getPageNumber(), pageable.getPageSize());

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }

        Page<Product> productPage = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);

        return PagedResponse.from(productPage, productMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getFeaturedProducts(int limit) {
        log.info("Fetching featured products, limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Product> featuredProducts = productRepository.findByIsFeaturedTrueAndIsActiveTrue(pageable);

        return productMapper.toSummaryResponseList(featuredProducts);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductSummaryResponse> searchProducts(String keyword, Pageable pageable) {
        log.info("Searching products with keyword: '{}', page: {}, size: {}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCase(keyword, pageable);

        return PagedResponse.from(productPage, productMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductDetailResponse> getProductsByIds(List<Long> ids) {
        log.info("Fetching products by IDs: {}", ids);

        List<Product> products = productRepository.findAllById(ids);

        return products.stream()
                .map(productMapper::toDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean checkProductExists(Long id) {
        log.info("Checking if product exists with ID: {}", id);
        return productRepository.existsById(id);
    }

    @Transactional
    @CachePut(value = RedisConfig.CacheNames.PRODUCT_BY_ID, key = "#productId")
    public ProductDetailResponse updateStockStatus(Long productId, StockStatus stockStatus) {
        log.info("Updating stock status for product ID: {} to {}", productId, stockStatus);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        product.setStockStatus(stockStatus);
        Product updatedProduct = productRepository.save(product);

        log.info("Stock status updated successfully for product ID: {}", productId);

        return productMapper.toDetailResponse(updatedProduct);
    }

}




