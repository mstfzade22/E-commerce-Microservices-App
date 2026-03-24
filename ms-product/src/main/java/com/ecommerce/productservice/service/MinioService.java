package com.ecommerce.productservice.service;

import com.ecommerce.productservice.exception.ImageUploadException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:product-images}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;


    public String uploadImage(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String objectName = generateObjectName(extension);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            String imageUrl = buildImageUrl(objectName);
            log.info("Image uploaded successfully: {}", imageUrl);

            return imageUrl;

        } catch (Exception e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            throw new ImageUploadException("Failed to upload image: " + e.getMessage());
        }
    }

    public void deleteImage(String imageUrl) {
        String objectName = extractObjectName(imageUrl);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            log.info("Image deleted successfully: {}", objectName);

        } catch (Exception e) {
            log.error("Failed to delete image: {}", e.getMessage(), e);
            throw new ImageUploadException("Failed to delete image: " + e.getMessage());
        }
    }

    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );

            log.debug("Generated presigned URL for: {}", objectName);
            return presignedUrl;

        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage(), e);
            throw new ImageUploadException("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("File is empty or null");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ImageUploadException("Only image files are allowed");
        }

        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new ImageUploadException("File size exceeds maximum limit of 5MB");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String generateObjectName(String extension) {
        return "products/" + UUID.randomUUID() + "." + extension;
    }

    private String buildImageUrl(String objectName) {
        return endpoint + "/" + bucketName + "/" + objectName;
    }

    private String extractObjectName(String imageUrl) {
        String prefix = endpoint + "/" + bucketName + "/";
        if (imageUrl.startsWith(prefix)) {
            return imageUrl.substring(prefix.length());
        }
        throw new ImageUploadException("Invalid image URL format: " + imageUrl);
    }

}
