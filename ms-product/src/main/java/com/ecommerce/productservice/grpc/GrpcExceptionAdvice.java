package com.ecommerce.productservice.grpc;

import com.ecommerce.productservice.exception.DuplicateResourceException;
import com.ecommerce.productservice.exception.ImageUploadException;
import com.ecommerce.productservice.exception.InvalidRequestException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@GrpcAdvice
@Slf4j
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler(ResourceNotFoundException.class)
    public StatusRuntimeException handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.info("gRPC NOT_FOUND: {}", ex.getMessage());
        return Status.NOT_FOUND
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asRuntimeException();
    }

    @GrpcExceptionHandler(DuplicateResourceException.class)
    public StatusRuntimeException handleDuplicateResourceException(DuplicateResourceException ex) {
        log.info("gRPC ALREADY_EXISTS: {}", ex.getMessage());
        return Status.ALREADY_EXISTS
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asRuntimeException();
    }

    @GrpcExceptionHandler(InvalidRequestException.class)
    public StatusRuntimeException handleInvalidRequestException(InvalidRequestException ex) {
        log.info("gRPC INVALID_ARGUMENT: {}", ex.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asRuntimeException();
    }

    @GrpcExceptionHandler(ImageUploadException.class)
    public StatusRuntimeException handleImageUploadException(ImageUploadException ex) {
        log.error("gRPC INTERNAL (Image Upload): {}", ex.getMessage());
        return Status.INTERNAL
                .withDescription("Image upload failed: " + ex.getMessage())
                .withCause(ex)
                .asRuntimeException();
    }

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleIllegalArgumentException(IllegalArgumentException ex) {
        log.info("gRPC INVALID_ARGUMENT: {}", ex.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asRuntimeException();
    }

    @GrpcExceptionHandler(SecurityException.class)
    public StatusRuntimeException handleSecurityException(SecurityException ex) {
        log.warn("gRPC PERMISSION_DENIED: {}", ex.getMessage());
        return Status.PERMISSION_DENIED
                .withDescription("Access denied: " + ex.getMessage())
                .withCause(ex)
                .asRuntimeException();
    }

    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleGenericException(Exception ex) {
        log.error("gRPC INTERNAL: Unexpected error", ex);
        return Status.INTERNAL
                .withDescription("An unexpected error occurred")
                .withCause(ex)
                .asRuntimeException();
    }

}
