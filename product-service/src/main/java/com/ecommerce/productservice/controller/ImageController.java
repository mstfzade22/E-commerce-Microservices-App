package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.request.ProductImageRequest;
import com.ecommerce.productservice.dto.response.ProductImageResponse;
import com.ecommerce.productservice.service.ProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products/{productId}/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Image Management", description = "APIs for managing product images")
public class ImageController {

    private final ProductImageService productImageService;

    @GetMapping
    @Operation(summary = "Get product images", description = "Retrieves all images for a product")
    public ResponseEntity<List<ProductImageResponse>> getProductImages(@PathVariable Long productId) {
        log.info("Request to get images for product ID: {}", productId);

        List<ProductImageResponse> images = productImageService.getProductImages(productId);

        return ResponseEntity.ok(images);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('STORE')")
    @Operation(summary = "Upload product image", description = "Uploads a new image for a product (Admin/Store only)")
    public ResponseEntity<ProductImageResponse> uploadImage(
            @PathVariable Long productId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "altText", required = false) String altText,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
            @RequestParam(value = "displayOrder", defaultValue = "0") int displayOrder) {
        log.info("Request to upload image for product ID: {}", productId);

        ProductImageRequest request = new ProductImageRequest(
                null,
                altText,
                isPrimary,
                displayOrder
        );

        ProductImageResponse response = productImageService.addImage(productId, file, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STORE')")
    @Operation(summary = "Delete product image", description = "Deletes an image from a product (Admin/Store only)")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        log.info("Request to delete image ID: {} for product ID: {}", imageId, productId);

        productImageService.deleteImage(imageId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STORE')")
    @Operation(summary = "Set primary image", description = "Sets an image as the primary image for a product (Admin/Store only)")
    public ResponseEntity<ProductImageResponse> setPrimaryImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        log.info("Request to set image ID: {} as primary for product ID: {}", imageId, productId);

        ProductImageResponse response = productImageService.setPrimaryImage(productId, imageId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STORE')")
    @Operation(summary = "Reorder images", description = "Reorders images for a product (Admin/Store only)")
    public ResponseEntity<List<ProductImageResponse>> reorderImages(
            @PathVariable Long productId,
            @RequestBody List<Long> imageIds) {
        log.info("Request to reorder images for product ID: {}", productId);

        List<ProductImageResponse> response = productImageService.reorderImages(productId, imageIds);

        return ResponseEntity.ok(response);
    }

}
