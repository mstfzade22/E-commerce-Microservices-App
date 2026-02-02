package com.ecommerce.productservice.dto.response;

public record ImageUploadResponse(
        Long id,
        String imageUrl,
        String fileName,
        Long size,
        String contentType,
        String message
) {
    public static ImageUploadResponse of(Long id, String imageUrl, String fileName, Long size, String contentType) {
        return new ImageUploadResponse(
                id,
                imageUrl,
                fileName,
                size,
                contentType,
                "Image uploaded successfully"
        );
    }
}
