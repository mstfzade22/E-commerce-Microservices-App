package com.ecommerce.orderservice.service;

import com.ecommerce.inventoryservice.grpc.ConfirmStockResponse;
import com.ecommerce.inventoryservice.grpc.ReleaseStockResponse;
import com.ecommerce.inventoryservice.grpc.ReserveStockResponse;
import com.ecommerce.orderservice.client.CartServiceClient;
import com.ecommerce.orderservice.config.RedisConfig;
import com.ecommerce.orderservice.dto.event.*;
import com.ecommerce.orderservice.dto.request.CancelOrderRequest;
import com.ecommerce.orderservice.dto.request.CreateOrderRequest;
import com.ecommerce.orderservice.dto.response.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.entity.OrderStatusHistory;
import com.ecommerce.orderservice.exception.*;
import com.ecommerce.orderservice.grpc.InventoryGrpcClient;
import com.ecommerce.orderservice.grpc.ProductGrpcClient;
import com.ecommerce.orderservice.kafka.OrderEventProducer;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.productservice.grpc.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartServiceClient cartServiceClient;
    private final ProductGrpcClient productGrpcClient;
    private final InventoryGrpcClient inventoryGrpcClient;
    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderRepository orderRepository,
                        OrderMapper orderMapper,
                        CartServiceClient cartServiceClient,
                        ProductGrpcClient productGrpcClient,
                        InventoryGrpcClient inventoryGrpcClient,
                        OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.cartServiceClient = cartServiceClient;
        this.productGrpcClient = productGrpcClient;
        this.inventoryGrpcClient = inventoryGrpcClient;
        this.orderEventProducer = orderEventProducer;
    }

    @Transactional
    public OrderCreateResponse createOrder(UUID userId, CreateOrderRequest request, String accessToken) {
        log.info("Creating order for user {}", userId);

        CartResponse cart = cartServiceClient.getCart(accessToken);
        if (cart.items() == null || cart.items().isEmpty()) {
            throw new CartEmptyException("Cannot create order: cart is empty");
        }

        CartValidationResponse validation = cartServiceClient.validateCart(accessToken);
        if (!validation.valid()) {
            throw new CartValidationException("Cart validation failed", validation.errors());
        }

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .shippingAddressLine1(request.shippingAddressLine1())
                .shippingAddressLine2(request.shippingAddressLine2())
                .shippingCity(request.shippingCity())
                .shippingState(request.shippingState())
                .shippingPostalCode(request.shippingPostalCode())
                .shippingCountry(request.shippingCountry())
                .notes(request.notes())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Long> reservedProductIds = new ArrayList<>();

        for (CartItemResponse cartItem : cart.items()) {
            ProductResponse product;
            try {
                product = productGrpcClient.getProduct(cartItem.productId());
            } catch (Exception e) {
                releaseReservedStock(orderNumber, reservedProductIds);
                throw new StockReservationException("Failed to fetch product details for productId: " + cartItem.productId());
            }

            ReserveStockResponse reserveResponse;
            try {
                reserveResponse = inventoryGrpcClient.reserveStock(orderNumber, cartItem.productId(), cartItem.quantity());
            } catch (Exception e) {
                releaseReservedStock(orderNumber, reservedProductIds);
                throw new StockReservationException("Failed to reserve stock for productId: " + cartItem.productId());
            }

            if (!reserveResponse.getSuccess()) {
                releaseReservedStock(orderNumber, reservedProductIds);
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }

            reservedProductIds.add(cartItem.productId());

            BigDecimal price = new BigDecimal(product.getPrice());
            BigDecimal discountPrice = product.getDiscountPrice().isEmpty()
                    ? BigDecimal.ZERO : new BigDecimal(product.getDiscountPrice());
            BigDecimal unitPrice = discountPrice.compareTo(BigDecimal.ZERO) > 0 ? discountPrice : price;
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.quantity()));

            String imageUrl = product.getImagesList().stream()
                    .filter(img -> img.getIsPrimary())
                    .map(img -> img.getImageUrl())
                    .findFirst()
                    .orElse(product.getImagesList().isEmpty() ? null : product.getImages(0).getImageUrl());

            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.productId())
                    .productName(product.getName())
                    .productImageUrl(imageUrl)
                    .sku(product.getSku())
                    .unitPrice(unitPrice)
                    .quantity(cartItem.quantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount.subtract(order.getDiscountAmount()));

        OrderStatusHistory initialHistory = OrderStatusHistory.builder()
                .previousStatus(null)
                .newStatus(OrderStatus.PENDING)
                .changedBy(userId)
                .reason("Order created")
                .build();
        order.addStatusHistory(initialHistory);

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} created successfully with {} items", orderNumber, savedOrder.getItems().size());

        try {
            cartServiceClient.clearCart(accessToken);
        } catch (Exception e) {
            log.warn("Failed to clear cart for user {} after order creation: {}", userId, e.getMessage());
        }

        List<OrderCreatedEvent.OrderEventItem> eventItems = savedOrder.getItems().stream()
                .map(item -> new OrderCreatedEvent.OrderEventItem(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        orderEventProducer.sendOrderCreatedEvent(new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "ORDER_CREATED",
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                userId,
                eventItems,
                savedOrder.getTotalAmount(),
                savedOrder.getFinalAmount(),
                savedOrder.getStatus().name(),
                Instant.now()
        ));

        return new OrderCreateResponse(
                savedOrder.getOrderNumber(),
                savedOrder.getStatus(),
                savedOrder.getFinalAmount(),
                savedOrder.getItems().size(),
                "Order created successfully",
                savedOrder.getCreatedAt()
        );
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.ORDER_BY_ID, key = "#orderNumber")
    public OrderResponse confirmOrder(String orderNumber, UUID changedBy) {
        log.info("Confirming order {}", orderNumber);

        Order order = findOrderByNumber(orderNumber);
        validateStatusTransition(order, OrderStatus.CONFIRMED);

        for (OrderItem item : order.getItems()) {
            ConfirmStockResponse response = inventoryGrpcClient.confirmStock(orderNumber, item.getProductId());
            if (!response.getSuccess()) {
                throw new StockReservationException("Failed to confirm stock for productId: " + item.getProductId());
            }
        }

        updateOrderStatus(order, OrderStatus.CONFIRMED, changedBy, "Payment confirmed");

        orderEventProducer.sendOrderConfirmedEvent(new OrderConfirmedEvent(
                UUID.randomUUID().toString(),
                "ORDER_CONFIRMED",
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                Instant.now(),
                Instant.now()
        ));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.ORDER_BY_ID, key = "#orderNumber")
    public OrderResponse cancelOrder(String orderNumber, UUID userId, CancelOrderRequest request, String role) {
        log.info("Cancelling order {}", orderNumber);

        Order order = findOrderByNumber(orderNumber);

        Set<OrderStatus> allowedStatuses;
        if ("ROLE_CUSTOMER".equals(role)) {
            allowedStatuses = Set.of(OrderStatus.PENDING);
        } else {
            allowedStatuses = Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);
        }

        if (!allowedStatuses.contains(order.getStatus())) {
            throw new InvalidOrderStatusException(
                    "Cannot cancel order in status " + order.getStatus() + ". Allowed: " + allowedStatuses);
        }

        for (OrderItem item : order.getItems()) {
            try {
                ReleaseStockResponse response = inventoryGrpcClient.releaseStock(orderNumber, item.getProductId());
                if (!response.getSuccess()) {
                    log.warn("Failed to release stock for productId {} on order {}: {}",
                            item.getProductId(), orderNumber, response.getMessage());
                }
            } catch (Exception e) {
                log.warn("Error releasing stock for productId {} on order {}: {}",
                        item.getProductId(), orderNumber, e.getMessage());
            }
        }

        order.setCancelledReason(request.reason());
        updateOrderStatus(order, OrderStatus.CANCELLED, userId, request.reason());

        orderEventProducer.sendOrderCancelledEvent(new OrderCancelledEvent(
                UUID.randomUUID().toString(),
                "ORDER_CANCELLED",
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                request.reason(),
                Instant.now(),
                Instant.now()
        ));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.ORDER_BY_ID, key = "#orderNumber")
    public OrderResponse processOrder(String orderNumber, UUID changedBy) {
        log.info("Processing order {}", orderNumber);

        Order order = findOrderByNumber(orderNumber);
        validateStatusTransition(order, OrderStatus.PROCESSING);
        updateOrderStatus(order, OrderStatus.PROCESSING, changedBy, "Order is being prepared");

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.ORDER_BY_ID, key = "#orderNumber")
    public OrderResponse shipOrder(String orderNumber, UUID changedBy) {
        log.info("Shipping order {}", orderNumber);

        Order order = findOrderByNumber(orderNumber);
        validateStatusTransition(order, OrderStatus.SHIPPED);
        updateOrderStatus(order, OrderStatus.SHIPPED, changedBy, "Order shipped");

        orderEventProducer.sendOrderShippedEvent(new OrderShippedEvent(
                UUID.randomUUID().toString(),
                "ORDER_SHIPPED",
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                Instant.now(),
                Instant.now()
        ));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CacheNames.ORDER_BY_ID, key = "#orderNumber")
    public OrderResponse deliverOrder(String orderNumber, UUID changedBy) {
        log.info("Delivering order {}", orderNumber);

        Order order = findOrderByNumber(orderNumber);
        validateStatusTransition(order, OrderStatus.DELIVERED);
        updateOrderStatus(order, OrderStatus.DELIVERED, changedBy, "Order delivered");

        orderEventProducer.sendOrderDeliveredEvent(new OrderDeliveredEvent(
                UUID.randomUUID().toString(),
                "ORDER_DELIVERED",
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                Instant.now(),
                Instant.now()
        ));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderNumber, UUID userId, String role) {
        log.debug("Getting order {} for user {}", orderNumber, userId);

        Order order;
        if ("ROLE_ADMIN".equals(role)) {
            order = findOrderByNumber(orderNumber);
        } else {
            order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        }

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> getOrdersByUser(UUID userId, int page, int size) {
        log.debug("Getting orders for user {}, page={}, size={}", userId, page, size);

        Page<Order> orders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return PagedResponse.from(orders, orderMapper::toOrderSummaryResponse);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> getOrdersByStatus(OrderStatus status, int page, int size) {
        log.debug("Getting orders by status {}, page={}, size={}", status, page, size);

        Page<Order> orders = orderRepository.findAllByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
        return PagedResponse.from(orders, orderMapper::toOrderSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(String orderNumber, UUID userId, String role) {
        log.debug("Getting order history for {}", orderNumber);

        Order order;
        if ("ROLE_ADMIN".equals(role)) {
            order = findOrderByNumber(orderNumber);
        } else {
            order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        }

        return order.getStatusHistory().stream()
                .map(orderMapper::toOrderStatusHistoryResponse)
                .toList();
    }

    private Order findOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
    }

    private void validateStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();
        Set<OrderStatus> validTransitions = switch (currentStatus) {
            case PENDING -> Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
            case CONFIRMED -> Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED);
            case PROCESSING -> Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED);
            case SHIPPED -> Set.of(OrderStatus.DELIVERED);
            case CANCELLED -> Set.of(OrderStatus.REFUNDED);
            case DELIVERED, REFUNDED -> Set.of();
        };

        if (!validTransitions.contains(newStatus)) {
            throw new InvalidOrderStatusException(
                    "Cannot transition from " + currentStatus + " to " + newStatus);
        }
    }

    private void updateOrderStatus(Order order, OrderStatus newStatus, UUID changedBy, String reason) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .previousStatus(order.getStatus())
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .build();
        order.addStatusHistory(history);
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long todayCount = orderRepository.countByCreatedAtToday() + 1;
        return String.format("ORD-%s-%03d", datePart, todayCount);
    }

    private void releaseReservedStock(String orderNumber, List<Long> reservedProductIds) {
        for (Long productId : reservedProductIds) {
            try {
                inventoryGrpcClient.releaseStock(orderNumber, productId);
            } catch (Exception e) {
                log.error("Failed to release stock for productId {} on order {}: {}",
                        productId, orderNumber, e.getMessage());
            }
        }
    }
}
