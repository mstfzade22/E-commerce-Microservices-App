package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.request.CreateProductRequest;
import com.ecommerce.productservice.dto.request.ProductFilterRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_happyPath() {
        CreateProductRequest request = mock(CreateProductRequest.class);
        when(request.slug()).thenReturn("test-product");
        when(request.sku()).thenReturn("SKU-001");
        when(request.initialStock()).thenReturn(50);
        when(request.categoryId()).thenReturn(1L);

        when(productRepository.existsBySlugAndIsActiveTrue("test-product")).thenReturn(false);
        when(productRepository.existsBySkuAndIsActiveTrue("SKU-001")).thenReturn(false);

        Product product = Product.builder().id(1L).slug("test-product").sku("SKU-001")
                .price(BigDecimal.TEN).isActive(true).build();
        when(productMapper.toEntity(request)).thenReturn(product);

        Category category = new Category();
        category.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductCreateResponse expected = mock(ProductCreateResponse.class);
        when(productMapper.toCreateResponse(product)).thenReturn(expected);

        ProductCreateResponse result = productService.createProduct(request);

        assertThat(result).isEqualTo(expected);
        verify(kafkaProducerService).publishProductCreatedEvent(product, 50);
    }

    @Test
    void createProduct_duplicateSlug_throwsDuplicateResourceException() {
        CreateProductRequest request = mock(CreateProductRequest.class);
        when(request.slug()).thenReturn("existing-slug");

        when(productRepository.existsBySlugAndIsActiveTrue("existing-slug")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateProduct_priceChange_publishesPriceChangedEvent() {
        Product product = Product.builder().id(1L).slug("test").sku("SKU-001")
                .price(BigDecimal.TEN).discountPrice(null).isActive(true).build();

        UpdateProductRequest request = mock(UpdateProductRequest.class);
        when(request.getSlug()).thenReturn(null);
        when(request.getSku()).thenReturn(null);
        when(request.getCategoryId()).thenReturn(null);
        when(request.getStock()).thenReturn(null);
        when(request.getPrice()).thenReturn(BigDecimal.valueOf(20));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDetailResponse expected = mock(ProductDetailResponse.class);
        when(productMapper.toDetailResponse(product)).thenReturn(expected);

        ProductDetailResponse result = productService.updateProduct(1L, request);

        assertThat(result).isEqualTo(expected);
        verify(kafkaProducerService).publishPriceChangedEvent(eq(product), eq(BigDecimal.TEN), isNull());
    }

    @Test
    void updateProduct_noPriceChange_noPriceEvent() {
        Product product = Product.builder().id(1L).slug("test").sku("SKU-001")
                .price(BigDecimal.TEN).discountPrice(null).isActive(true).build();

        UpdateProductRequest request = mock(UpdateProductRequest.class);
        when(request.getSlug()).thenReturn(null);
        when(request.getSku()).thenReturn(null);
        when(request.getCategoryId()).thenReturn(null);
        when(request.getStock()).thenReturn(null);
        when(request.getPrice()).thenReturn(null);
        when(request.getDiscountPrice()).thenReturn(null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDetailResponse expected = mock(ProductDetailResponse.class);
        when(productMapper.toDetailResponse(product)).thenReturn(expected);

        productService.updateProduct(1L, request);

        verify(kafkaProducerService, never()).publishPriceChangedEvent(any(), any(), any());
    }

    @Test
    void deleteProduct_softDelete() {
        Product product = Product.builder().id(1L).slug("test").sku("SKU-001")
                .price(BigDecimal.TEN).isActive(true).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(1L);

        assertThat(product.getIsActive()).isFalse();
        verify(kafkaProducerService).publishProductDeletedEvent(1L, "test", "SKU-001");
    }

    @Test
    @SuppressWarnings("unchecked")
    void filterProducts_withFilters() {
        ProductFilterRequest filter = new ProductFilterRequest(
                "phone", null, null, null, null, null, null, "price", "asc");

        Product product = Product.builder().id(1L).name("Phone").build();
        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        ProductSummaryResponse summary = mock(ProductSummaryResponse.class);
        when(productMapper.toSummaryResponse(product)).thenReturn(summary);

        PagedResponse<ProductSummaryResponse> result = productService.filterProducts(filter, 0, 10);

        assertThat(result.content()).hasSize(1);
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void deleteProduct_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
