package com.ecommerce.productservice.repositories;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByIdAndIsActiveTrue(Long id);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    List<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    Page<Product> findByCategoryIdInAndIsActiveTrue(List<Long> categoryIds, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIsActiveTrue(String slug);

    boolean existsBySkuAndIsActiveTrue(String sku);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Long countByCategoryId(Long categoryId);

    Page<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String keyword, Pageable pageable);

}
