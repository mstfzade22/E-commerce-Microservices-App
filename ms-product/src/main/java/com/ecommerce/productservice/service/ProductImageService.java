package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.request.ProductImageRequest;
import com.ecommerce.productservice.dto.response.ProductImageResponse;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductImages;
import com.ecommerce.productservice.exception.InvalidRequestException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.mapper.ProductImagesMapper;
import com.ecommerce.productservice.repositories.ProductImagesRepository;
import com.ecommerce.productservice.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImagesRepository productImagesRepository;
    private final ProductRepository productRepository;
    private final ProductImagesMapper productImagesMapper;
    private final MinioService minioService;

    @Transactional
    public ProductImageResponse addImage(Long productId, MultipartFile file, ProductImageRequest request) {
        log.info("Adding image to product ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        String imageUrl = minioService.uploadImage(file);

        ProductImages productImage = ProductImages.builder()
                .product(product)
                .imageUrl(imageUrl)
                .altText(request.altText())
                .isPrimary(request.isPrimary())
                .displayOrder(request.displayOrder())
                .build();

        if (request.isPrimary()) {
            unsetPrimaryImage(productId);
        }

        if (productImagesRepository.countByProductId(productId) == 0) {
            productImage.setPrimary(true);
        }

        ProductImages savedImage = productImagesRepository.save(productImage);
        log.info("Image added successfully with ID: {}", savedImage.getId());

        return productImagesMapper.toResponse(savedImage);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        log.info("Deleting image with ID: {}", imageId);

        ProductImages image = productImagesRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));

        Long productId = image.getProduct().getId();
        boolean wasPrimary = image.isPrimary();

        minioService.deleteImage(image.getImageUrl());

        productImagesRepository.delete(image);

        if (wasPrimary) {
            setFirstImageAsPrimary(productId);
        }

        log.info("Image deleted successfully with ID: {}", imageId);
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getProductImages(Long productId) {
        log.info("Fetching images for product ID: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }

        List<ProductImages> images = productImagesRepository.findByProductIdOrderByDisplayOrderAsc(productId);

        return productImagesMapper.toResponseList(images);
    }

    @Transactional
    public ProductImageResponse setPrimaryImage(Long productId, Long imageId) {
        log.info("Setting image {} as primary for product {}", imageId, productId);

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }

        ProductImages image = productImagesRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new InvalidRequestException("Image does not belong to the specified product");
        }

        unsetPrimaryImage(productId);

        image.setPrimary(true);
        ProductImages updatedImage = productImagesRepository.save(image);

        log.info("Image {} set as primary for product {}", imageId, productId);

        return productImagesMapper.toResponse(updatedImage);
    }

    @Transactional
    public List<ProductImageResponse> reorderImages(Long productId, List<Long> imageIds) {
        log.info("Reordering images for product ID: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }

        List<ProductImages> images = productImagesRepository.findByProductId(productId);

        if (images.size() != imageIds.size()) {
            throw new InvalidRequestException("Image count mismatch. Provide all image IDs for this product.");
        }

        for (int i = 0; i < imageIds.size(); i++) {
            Long imageId = imageIds.get(i);
            int newOrder = i;

            ProductImages image = images.stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException("Image ID " + imageId + " does not belong to product " + productId));

            image.setDisplayOrder(newOrder);
        }

        List<ProductImages> savedImages = productImagesRepository.saveAll(images);

        log.info("Images reordered successfully for product ID: {}", productId);

        return productImagesMapper.toResponseList(
                savedImages.stream()
                        .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                        .toList()
        );
    }

    @Transactional
    public void deleteAllProductImages(Long productId) {
        log.info("Deleting all images for product ID: {}", productId);

        List<ProductImages> images = productImagesRepository.findByProductId(productId);

        for (ProductImages image : images) {
            try {
                minioService.deleteImage(image.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete image from MinIO: {}", image.getImageUrl(), e);
            }
        }

        productImagesRepository.deleteByProductId(productId);

        log.info("All images deleted for product ID: {}", productId);
    }

    private void unsetPrimaryImage(Long productId) {
        productImagesRepository.findByProductIdAndIsPrimaryTrue(productId)
                .ifPresent(primaryImage -> {
                    primaryImage.setPrimary(false);
                    productImagesRepository.save(primaryImage);
                });
    }

    private void setFirstImageAsPrimary(Long productId) {
        List<ProductImages> images = productImagesRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        if (!images.isEmpty()) {
            ProductImages firstImage = images.get(0);
            firstImage.setPrimary(true);
            productImagesRepository.save(firstImage);
        }
    }

}
