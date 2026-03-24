package com.ecommerce.inventoryservice.grpc;

import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@Slf4j
@GrpcAdvice
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler(InventoryNotFoundException.class)
    public StatusRuntimeException handleResourceNotFoundException(InventoryNotFoundException e) {
        log.error("gRPC Not Found Error: {}", e.getMessage());
        return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    }

    @GrpcExceptionHandler(InsufficientStockException.class)
    public StatusRuntimeException handleInsufficientStockException(InsufficientStockException e) {
        log.error("gRPC Failed Precondition Error: {}", e.getMessage());
        return Status.FAILED_PRECONDITION.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    }

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("gRPC Invalid Argument Error: {}", e.getMessage());
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    }

    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleGenericException(Exception e) {
        log.error("gRPC Internal Error: {}", e.getMessage(), e);
        return Status.INTERNAL.withDescription("An unexpected error occurred").withCause(e).asRuntimeException();
    }
}