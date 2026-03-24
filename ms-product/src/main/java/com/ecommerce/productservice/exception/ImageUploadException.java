package com.ecommerce.productservice.exception;

public class ImageUploadException extends RuntimeException {

    public ImageUploadException(String message) {
        super(message);
    }

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageUploadException(String fileName, String reason) {
        super(String.format("Failed to upload image '%s': %s", fileName, reason));
    }

}
