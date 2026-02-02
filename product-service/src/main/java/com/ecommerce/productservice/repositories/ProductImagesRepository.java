package com.ecommerce.productservice.repositories;

import com.ecommerce.productservice.entity.ProductImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImagesRepository extends JpaRepository<ProductImages, Long> {

    List<ProductImages> findByProductId(Long productId);

    List<ProductImages> findByProductIdOrderByDisplayOrderAsc(Long productId);

    Optional<ProductImages> findByProductIdAndIsPrimaryTrue(Long productId);

    Long countByProductId(Long productId);

    void deleteByProductId(Long productId);

}
